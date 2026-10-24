package com.unciv.ui.battlescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.troop.Formation
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.map.TileGroupMap
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.tilegroups.TileSetStrings
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.unciv.utils.concurrency.Dispatcher
import kotlinx.coroutines.*

/** Renders session values and submits offered commands; never executes combat rules. */
class BattleSessionScreen(
    client: BattleClient,
    private val side: BattleSide,
    initial: BattleSessionSnapshot,
    ruleset: Ruleset
) : BaseScreen() {
    private val cursors by lazy { BattleCursorSet() }
    private val presentationRenderer by lazy { BattlePresentationRenderer(tileGroups) }
    private val controller = BattleClientController(client, side)
    private var displayed = initial
    private val description = requireNotNull(initial.state.field)
    private val map = BattleFieldViewMapFactory.create(description, ruleset)
    private val tileGroups = map.values.map { TileGroup(it, TileSetStrings()) }
    private val byPosition = tileGroups.associateBy {
        Point(it.tileInfo.position.x.toInt(), it.tileInfo.position.y.toInt())
    }
    private val mapView = TileGroupMap(tileGroups)
    private val naturalWidth = mapView.width
    private val naturalHeight = mapView.height
    private val troops = mutableMapOf<Int, Troop>()
    private val views = mutableMapOf<Int, TroopBattleView>()
    private var hovered: Int? = null
    private var busy = false
    private var scope: CoroutineScope? = null
    private var polling: Job? = null
    private val status = "Connecting…".toLabel().apply { setWrap(true) }
    private val retryButton = "Retry pending action".toTextButton()
    private val header = Table()
    private val host = object : BattleTroopViewHost {
        override val stage: Stage get() = this@BattleSessionScreen.stage
        override fun setHoveredTroop(troop: Troop?) {
            hovered?.let { views[it]?.setHoveredEnemy(false) }
            hovered = troop?.id
            hovered?.let { views[it]?.setHoveredEnemy(true) }
        }
        override fun isCurrentTroop(troop: Troop) = displayed.state.currentTroopId == troop.id
        override fun hasFormationRecoveryChance(troop: Troop) =
            displayed.state.troops.firstOrNull { it.id == troop.id }?.hasFormationRecoveryChance == true
        override fun getNormalRetaliationBudget(troop: Troop) =
            displayed.state.troops.firstOrNull { it.id == troop.id }?.normalRetaliationBudget ?: 0
    }
    private val bottomBar = BattleTurnBar(
        queueProvider = { displayed.state.turnQueue.mapNotNull(troops::get) },
        isAttacker = { troop -> displayed.state.troops.any { it.id == troop.id && it.side == BattleSide.ATTACKER } },
        onSkip = { skip() }, onExit = { leave() },
        onHover = { host.setHoveredTroop(it) },
        remainingInRoundProvider = { displayed.state.remainingTurnsInRound }
    )

    init {
        tileGroups.forEach { group ->
            group.showEntireMap = true
            group.update()
            group.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    chooseCommand(group, x, y)?.let(::submit)
                }
            })
        }
        stage.addActor(mapView)
        stage.addActor(bottomBar)
        retryButton.onClick { operate { controller.retry() } }
        header.add(status).growX().pad(8f)
        header.add(retryButton).width(210f).height(44f).pad(8f)
        stage.addActor(header)
        globalShortcuts.add(KeyCharAndCode.BACK) { leave() }
        globalShortcuts.add(KeyCharAndCode.SPACE) { skip() }
        applySnapshot(initial)
        layout()
    }

    private fun canAct(): Boolean = !busy && controller.snapshot != null &&
        controller.pendingRequest == null && !displayed.state.finished && displayed.state.currentSide == side

    private fun skip() {
        displayed.state.availableCommands.filterIsInstance<BattleCommand.Skip>().firstOrNull()?.let(::submit)
    }

    private fun chooseCommand(group: TileGroup, x: Float, y: Float): BattleCommand? {
        if (!canAct()) return null
        val target = Point(group.tileInfo.position.x.toInt(), group.tileInfo.position.y.toInt())
        val commands = displayed.state.availableCommands
        commands.filterIsInstance<BattleCommand.Shoot>().firstOrNull { it.target == target }
            ?.let { return it }
        commands.filterIsInstance<BattleCommand.Move>().firstOrNull { it.target == target }
            ?.let { return it }
        val direction = BattlePointerGeometry.attackDirection(x, y, group.baseLayerGroup.width)
        if (direction == com.unciv.logic.Direction.DirError) return null
        val neighbor = map.getNeighborTile(group.tileInfo, direction) ?: return null
        val approach = Point(neighbor.position.x.toInt(), neighbor.position.y.toInt())
        return commands.filterIsInstance<BattleCommand.Attack>().firstOrNull {
            it.target == target && it.attackFrom == approach
        }
    }

    private fun submit(command: BattleCommand) {
        if (!canAct() || command !in displayed.state.availableCommands) return
        operate { controller.submit(command) }
    }

    private fun applySnapshot(snapshot: BattleSessionSnapshot) {
        require(snapshot.state.field == description) { "Battlefield changed during the session" }
        displayed = snapshot
        val alive = snapshot.state.troops.map { it.id }.toSet()
        views.keys.filter { it !in alive }.forEach { id ->
            views.remove(id)?.perish()
            troops.remove(id)
            if (hovered == id) hovered = null
        }
        snapshot.state.troops.forEach { value ->
            val troop = troops.getOrPut(value.id) {
                Troop(unitName = value.unitName, amount = value.initialAmount, speed = value.speed,
                    damage = value.damage, maxHealth = value.maxHealth, rangedStrength = value.rangedStrength,
                    currentAmount = value.currentAmount, currentHealth = value.currentHealth, id = value.id,
                    formationHealthPercent = value.formationHealthPercent,
                    formationDamageReductionPercent = value.formationDamageReductionPercent)
            }
            troop.currentAmount = value.currentAmount
            troop.currentHealth = value.currentHealth
            troop.formation = Formation(value.formation, value.maximumFormation)
            val tile = value.position?.let(byPosition::get)
            if (tile != null) {
                val existing = views[value.id]
                val view = existing ?: TroopBattleView(troop, host).also {
                    views[value.id] = it
                    it.draw(tile, value.side == BattleSide.ATTACKER)
                }
                if (existing != null && view.getCurrentGroup().parent !== tile) view.updatePosition(tile)
                view.setActive(snapshot.state.currentTroopId == value.id)
                view.updateStats()
            }
        }
        bottomBar.refresh()
        updateStatus()
    }

    private fun updateStatus() {
        val pending = controller.pendingRequest != null
        retryButton.isDisabled = busy || !pending
        status.setText(
            when {
                busy -> "Waiting for server…"
                pending -> "Action outcome unknown. Refreshing state; you can retry the same action."
                displayed.state.finished -> displayed.state.winner?.let { "Battle finished. Winner: $it" }
                    ?: "Battle finished: draw."

                displayed.state.currentSide == side -> "Your turn — click a target or press Space to skip."
                else -> "Opponent's turn."
            })
        presentationRenderer.update(displayed.state)
    }

    private fun operate(action: suspend () -> BattleSessionSnapshot) {
        val owner = scope ?: return
        if (busy) return
        polling?.cancel()
        polling = null
        busy = true
        updateStatus()
        owner.launch {
            var failed = false
            try {
                applySnapshot(action())
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                failed = true
            } finally {
                if (scope === owner && isActive) {
                    busy = false
                    updateStatus()
                    if (failed) status.setText("Connection or state update failed. Input is paused until the next successful refresh.")
                    if (failed) busy = true
                    startPolling()
                }
            }
        }
    }

    private fun startPolling() {
        val owner = scope ?: return
        if (polling?.isActive == true) return
        polling = owner.launch {
            while (isActive) {
                delay(1200)
                try {
                    val snapshot = controller.refresh()
                    busy = false
                    applySnapshot(snapshot)
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    busy = true
                    updateStatus()
                    status.setText("Connection interrupted. Reconnecting…")
                    delay(2500)
                }
            }
        }
    }

    override fun show() {
        super.show()
        if (scope == null) scope = CoroutineScope(SupervisorJob() + Dispatcher.GL)
        busy = false
        operate { controller.refresh() }
    }

    override fun hide() {
        stopRequests()
        Gdx.graphics.setSystemCursor(com.badlogic.gdx.graphics.Cursor.SystemCursor.Arrow)
        super.hide()
    }

    override fun dispose() {
        stopRequests()
        views.values.forEach { it.hideInfo() }
        cursors.dispose()
        super.dispose()
    }

    private fun stopRequests() {
        scope?.cancel()
        scope = null
        polling = null
    }

    private fun leave() {
        stopRequests()
        game.popScreen()
    }

    override fun render(delta: Float) {
        super.render(delta)
        val pointer = stage.screenToStageCoordinates(
            Vector2(
                Gdx.input.x.toFloat(),
                Gdx.input.y.toFloat()
            )
        )
        var actor = stage.hit(pointer.x, pointer.y, true)
        var tile: TileGroup? = null
        var overQueue = false
        while (actor != null) {
            if (actor === bottomBar) overQueue = true
            if (actor is TileGroup) {
                tile = actor; break
            }
            actor = actor.parent
        }
        val position = tile?.tileInfo?.position?.let { Point(it.x.toInt(), it.y.toInt()) }
        val occupant =
                displayed.state.troops.firstOrNull { position != null && it.position == position }
        if (!overQueue && hovered != occupant?.id) host.setHoveredTroop(occupant?.id?.let(troops::get))
        val local = tile?.stageToLocalCoordinates(pointer.cpy())
        val command =
                if (tile != null && local != null) chooseCommand(tile, local.x, local.y) else null
        val preview =
                displayed.state.presentation.attacks.firstOrNull { it.command == command }?.preview
        if (tile != null && local != null && canAct()) {
            cursors.show(
                command, BattlePointerGeometry.attackDirection(
                    local.x, local.y,
                    tile.baseLayerGroup.width
                ), preview
            )
        } else Gdx.graphics.setSystemCursor(com.badlogic.gdx.graphics.Cursor.SystemCursor.Arrow)
        val shift =
                Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
        presentationRenderer.hover(command, if (shift) occupant?.id else null)
        val inspected = occupant?.takeIf { Gdx.input.isButtonPressed(Input.Buttons.RIGHT) }
        views.forEach { (id, view) -> if (id != inspected?.id) view.hideInfo() }
        if (inspected != null && tile != null) {
            val enemyTarget = canAct() && inspected.side != displayed.state.currentSide
            views[inspected.id]?.showInfo(
                tile, preview, inspected.retaliationBudget,
                attackUnavailable = enemyTarget && preview == null
            )
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        layout()
    }

    private fun layout() {
        val available = (stage.height - BattleTurnBar.HEIGHT - 90f).coerceAtLeast(1f)
        val scale = minOf(stage.width / naturalWidth, available / naturalHeight)
        mapView.setScale(scale)
        mapView.setPosition((stage.width - naturalWidth * scale) / 2f,
            BattleTurnBar.HEIGHT + (available - naturalHeight * scale) / 2f)
        bottomBar.setSize(stage.width, BattleTurnBar.HEIGHT)
        header.setBounds(0f, stage.height - 80f, stage.width, 80f)
    }
}
