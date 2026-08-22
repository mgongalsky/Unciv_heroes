package com.unciv.ui.battlescreen

import BattleActionResult
import ErrorId
import com.unciv.logic.battle.BattleManager

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Cursor.SystemCursor
import com.badlogic.gdx.graphics.Texture
import com.unciv.logic.Direction
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.unciv.ai.AIBattle
import com.unciv.logic.HexMath
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.battle.CityCombatant
import com.unciv.logic.battle.ICombatant
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.map.ClimateParameters
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.mapgenerator.MapGenerator
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.pathfinding.TroopMovementContext
import com.unciv.pure.domain.battle.BattleEvent
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.map.TileGroupMap
import com.unciv.ui.overviewscreen.EmpireOverviewTab
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.tilegroups.TileSetStrings
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RecreateOnResize
import com.unciv.ui.utils.TabbedPager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.resume
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.application.battle.BattleScreenCommandMapper

// Now it's just copied from HeroOverviewScreen
// All coordinates are hex, not offset

/**
 * Screen for a battle.
 * This class handles the visual representation of the battle and user interactions.
 * The battle logic itself is managed by the [BattleManager] class.
 * Should not be used for AI duels as it includes player interaction logic.
 */
class BattleScreen private constructor(
    private val attackerArmy: ArmyInfo,
    private val defenderArmy: ArmyInfo,
    private val attackerIsPlayer: Boolean,
    private val defenderIsPlayer: Boolean,
    private val attackerClimate: ClimateParameters,
    private val defenderClimate: ClimateParameters,
    // Null в sandbox — world-map cleanup не нужен
    private val attacker: ICombatant? = null,
    private val defender: ICombatant? = null,
) : BaseScreen(), RecreateOnResize, KoinComponent {

    private val ruleset: Ruleset by inject()

    companion object {
        fun fromCombatants(attacker: ICombatant, defender: ICombatant): BattleScreen {
            val attackerArmy = when (attacker) {
                is MapUnitCombatant -> attacker.unit.army
                is CityCombatant   -> attacker.city.garrisonInfo
                else -> throw IllegalArgumentException("Unsupported attacker type")
            }
            val defenderArmy = when (defender) {
                is MapUnitCombatant -> defender.unit.army
                is CityCombatant   -> defender.city.garrisonInfo
                else -> throw IllegalArgumentException("Unsupported defender type")
            }
            val attackerCiv = when (attacker) {
                is MapUnitCombatant -> attacker.unit.civInfo
                is CityCombatant   -> attacker.city.civInfo
                else -> throw IllegalArgumentException()
            }
            val defenderCiv = when (defender) {
                is MapUnitCombatant -> defender.unit.civInfo
                is CityCombatant   -> defender.city.civInfo
                else -> throw IllegalArgumentException()
            }
            return BattleScreen(
                attackerArmy = attackerArmy,
                defenderArmy = defenderArmy,
                attackerIsPlayer = attackerCiv.isPlayerCivilization(),
                defenderIsPlayer = defenderCiv.isPlayerCivilization(),
                attackerClimate = attacker.getTile().getClimateParameters()
                    ?: ClimateParameters(0.3, 0.5, 0.6),
                defenderClimate = defender.getTile().getClimateParameters()
                    ?: ClimateParameters(0.3, 0.5, 0.6),
                attacker = attacker,
                defender = defender,
            )
        }

        fun forTesting(
            attackerArmy: ArmyInfo,
            defenderArmy: ArmyInfo,
            climate: ClimateParameters = ClimateParameters(0.3, 0.5, 0.6)
        ): BattleScreen = BattleScreen(
            attackerArmy = attackerArmy,
            defenderArmy = defenderArmy,
            attackerIsPlayer = true,
            defenderIsPlayer = false,
            attackerClimate = climate,
            defenderClimate = climate,
        )
    }

    /*
    private var defenderCiv = when (val d = defender) {
        is MapUnitCombatant -> d.unit.civInfo // Если защитник - MapUnitCombatant, берем армию юнита
        is CityCombatant -> d.city.civInfo // Если защитник - CityCombatant, берем информацию о гарнизоне
        else -> throw IllegalArgumentException("Unsupported defender type")
    }

     */


    // Arrays to store visual representations of troops for attackers and defenders
    private val attackerTroopViewsArray: Array<TroopBattleView?> =
            Array(attackerArmy.getAllTroops().size ?: 0) { null }
    private val defenderTroopViewsArray: Array<TroopBattleView?> =
            Array(defenderArmy.getAllTroops().size ?: 0) { null }

    private val verboseTurn = true // Toggle for detailed logging of turns

    // Constants for the center area and battlefield dimensions
    internal val centerAreaHeight = stage.height - 82f
    internal val BFwidth: Int = 14
    internal val BFheight: Int = 8

    /*
    private val defenderTile = when (val d = defender) {
        is MapUnitCombatant -> d.unit.currentTile // Если защитник - MapUnitCombatant, берем армию юнита
        is CityCombatant -> d.city.getCenterTile() // Если защитник - CityCombatant, берем информацию о гарнизоне
        else -> throw IllegalArgumentException("Unsupported defender type")
    }

     */

    // TileMap represents the battlefield layout
    //private val battleField: TileMap = TileMap(
     //   BFwidth, BFheight,
     //   game.gameInfo!!.ruleSet, defenderTile.baseTerrain
    //)
    // Предположим, что BFwidth, BFheight, attackerTile и defenderTile уже определены
    val mapGenerator = MapGenerator(ruleset)
    val battleField: TileMap = mapGenerator.generateBattlefield(BFwidth, BFheight, attackerClimate, defenderClimate)

    /*private val battleField: TileMap = TileMap(
        BFwidth, BFheight,
        game.gameInfo!!.ruleSet, defenderTile.baseTerrain
    )

     */

    private var manager = BattleManager(attackerArmy, defenderArmy, battleField)


    // Holds the mapping of tiles to their groups for visual representation
    val tileGroups = HashMap<TileInfo, List<TileGroup>>()

    private lateinit var tileGroupMap: TileGroupMap<TileGroup>
    private val allTileGroups = ArrayList<TileGroup>()

    /** Position of a pointer to the currently active troop */
    lateinit var pointerPosition: Vector2

    // List of images for the pointer representation
    var pointerImages: ArrayList<Image>
    var daTileGroups: List<TileGroup>

    // Different cursors for various actions
    val cursorMove: Cursor
    val cursorCancel: Cursor
    val cursorShoot: Cursor
    var cursorAttack: ArrayList<Cursor> = ArrayList()

    // External images for visual effects (e.g., morale and luck indicators)
    val luckRainbowImage = ImageGetter.getExternalImage("LuckRainbow.png")
    val moraleImage = ImageGetter.getExternalImage("MoraleBird.png")

    /** Container for the battlefield UI elements. Could be reworked in the future */
    private val tabbedPager: TabbedPager

    override fun dispose() {
        battleScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        tabbedPager.selectPage(-1)
        super.dispose()
    }

    init {
        manager.initializeBattle()
        attackerArmy.getAllTroops()?.forEachIndexed { index, troop ->
            if (troop != null) attackerTroopViewsArray[index] = TroopBattleView(troop, this)
        }
        defenderArmy.getAllTroops()?.forEachIndexed { index, troop ->
            if (troop != null) defenderTroopViewsArray[index] = TroopBattleView(troop, this)
        }

        cursorMove = loadCursor("BattleMoveCursor128.png", 32, 64)
        cursorShoot = loadCursor("BattleArrowCursor128.png", 64, 64)
        cursorCancel = loadCursor("BattleCancelCursor64.png", 32, 32)
        cursorAttack.add(loadCursor("BattleAttackCursor0.png", 64, 64))
        cursorAttack.add(loadCursor("BattleAttackCursor1.png", 64, 16))
        cursorAttack.add(loadCursor("BattleAttackCursor2.png", 64, 64))
        cursorAttack.add(loadCursor("BattleAttackCursor3.png", 64, 64))
        cursorAttack.add(loadCursor("BattleAttackCursor4.png", 64, 16))
        cursorAttack.add(loadCursor("BattleAttackCursor5.png", 64, 64))

        globalShortcuts.add(KeyCharAndCode.BACK) { shutdownScreen() }
        globalShortcuts.add(KeyCharAndCode.SPACE) { sendSkipTurnRequest() }
        val tileSetStrings = TileSetStrings()
        daTileGroups = battleField.values.map { TileGroup(it, tileSetStrings) }
        pointerImages = ImageGetter.getLayeredImageColored(
            "TileSets/FantasyHex/Highlight", Color.valueOf("#00AAFF77")
        )
        tabbedPager = TabbedPager(
            stage.width, stage.width, centerAreaHeight, centerAreaHeight,
            separatorColor = Color.WHITE
        )
        tabbedPager.addClosePage { shutdownScreen() }
        addTiles()
        stage.addActor(tabbedPager)
        val index = tabbedPager.addPage(caption = "Battle", content = tileGroupMap)
        tabbedPager.selectPage(index)
        tabbedPager.setFillParent(true)
        updateTilesShadowing()

        manager.onEvent = { event ->
            when (event) {
                is BattleEvent.TroopMoved -> Gdx.app.postRunnable {
                    val troop = manager.getTroopById(event.troopId) ?: return@postRunnable
                    val troopView = getTroopViewFor(troop) ?: return@postRunnable
                    val newTile = daTileGroups.firstOrNull {
                        it.tileInfo.position.x.toInt() == event.to.x &&
                                it.tileInfo.position.y.toInt() == event.to.y
                    }
                    troopView.updatePosition(newTile)
                    refreshTroopViews()
                    if (event.isMorale && manager.isBattleOn()) showMoraleBird(troopView)
                }

                is BattleEvent.TroopAttacked -> Gdx.app.postRunnable {
                    val attacker = manager.getTroopById(event.attackerId) ?: return@postRunnable
                    val attackerView = getTroopViewFor(attacker) ?: return@postRunnable
                    if (event.isLuck) showLuckRainbow(attackerView)
                    attackerView.updatePosition(daTileGroups.firstOrNull {
                        it.tileInfo == manager.getTroopTile(attacker)
                    })
                    refreshTroopViews()
                    if (event.isMorale && manager.isBattleOn()) showMoraleBird(attackerView)
                }

                is BattleEvent.TroopShot -> Gdx.app.postRunnable {
                    val attacker = manager.getTroopById(event.attackerId) ?: return@postRunnable
                    val attackerView = getTroopViewFor(attacker) ?: return@postRunnable
                    if (event.isLuck) showLuckRainbow(attackerView)
                    refreshTroopViews()
                    if (event.isMorale && manager.isBattleOn()) showMoraleBird(attackerView)
                }

                is BattleEvent.TurnAdvanced ->
                    println("[EVENT] TurnAdvanced: nextTroop=${event.nextTroopId}")

                is BattleEvent.BattleEnded -> Gdx.app.postRunnable {
                    shutdownScreen()
                    manager.finishBattle()
                    val battleResult = manager.getBattleResult()
                    if (battleResult == null) {
                        println("Bug with battle result.")
                    } else {
                        if (verboseTurn)
                            println("Army of ${battleResult.winningArmy.civInfo.nation.name} won.")
                        com.unciv.logic.battle.BattleWorldOutcomeHandler(attacker, defender)
                            .apply(attackerArmy == battleResult.winningArmy)
                    }
                }

                is BattleEvent.TurnSkipped -> println("[EVENT] TurnSkipped")
            }
        }
        battleScope.launch { runBattleLoop() }
    }

    private fun sendSkipTurnRequest() {
        val currentTroop = manager.getCurrentTroop() ?: return
        val currentTile = manager.getTroopTile(currentTroop) ?: return
        val targetTileGroup = daTileGroups.firstOrNull { it.tileInfo == currentTile }
        if (targetTileGroup == null) {
            println("Error: No tile group found for current troop's position.")
            return
        }
        onPlayerActionReceived?.invoke(
            Pair(BattleCommand.Skip(currentTroop.id), targetTileGroup)
        )
    }

    /**
     * Loads a custom cursor from a file.
     *
     * @param filename Name of the cursor image file.
     * @param xHotspot X-coordinate of the cursor's hotspot.
     * @param yHotspot Y-coordinate of the cursor's hotspot.
     * @return A [Cursor] object for the specified image.
     */
    fun loadCursor(filename: String, xHotspot: Int, yHotspot: Int) : Cursor{

        val texture = Texture("ExtraImages/" + filename)
        texture.textureData.prepare()
        val pixmap = texture.textureData.consumePixmap()
        return Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot)

    }


    private fun isTroopPlayerControlled(troop: Troop): Boolean {
        return if (manager.getAttackerArmy().contains(troop)) attackerIsPlayer
        else defenderIsPlayer
    }

    suspend fun runBattleLoop() = coroutineScope {
        manager.onEvent = null
        while (manager.isBattleOn()) {
            val currentTroop = manager.getCurrentTroop()
            if (currentTroop == null) {
                Gdx.app.postRunnable { shutdownScreen() }
                return@coroutineScope
            }
            if (verboseTurn) println(
                "Current troop: ${currentTroop.unitName} at position ${
                    manager.getTroopTile(
                        currentTroop
                    )?.position
                }"
            )

            if (isTroopPlayerControlled(currentTroop)) {
                var success = false
                while (!success) {
                    if (verboseTurn) println("Waiting for player action...")
                    val (command, _) = waitForPlayerAction()
                    if (verboseTurn) println("Received command: $command")
                    val result = manager.execute(command, ::handleApplicationEvent)
                    if (result.success) {
                        success = true
                    } else {
                        if (verboseTurn) println("Command $command failed with rejection: ${result.rejection}")
                        handleActionError(result.rejection)
                    }
                }
            } else {
                if (verboseTurn) println("AI is performing action for troop: ${currentTroop.unitName}")
                AIBattle(manager, ::handleApplicationEvent).performTurn(currentTroop)
            }

            val currentTroopAfter = manager.getCurrentTroop()
            if (currentTroopAfter != null && !manager.getTurnQueue().isEmpty()) {
                manager.advanceTurn()
                if (verboseTurn) println("Turn advanced to next troop")
            }

            movePointerToNextTroop()
            updateTilesShadowing()
        }
        manager.finishBattle()
        println("Battle has ended!")
    }

    /**
     * Refreshes the arrays of troop views for both attackers and defenders
     * based on the current state of the armies in the manager.
     */
    fun refreshTroopViews() {
        Gdx.app.postRunnable {
            // Убедитесь, что операции обновления происходят в графическом потоке
            for (i in attackerTroopViewsArray.indices) {
                val troop = manager.getAttackerArmy().getTroopAt(i)
                if (troop == null) {
                    attackerTroopViewsArray[i]?.perish()
                    attackerTroopViewsArray[i] = null
                } else {
                    attackerTroopViewsArray[i]?.updateStats()
                }
            }

            for (i in defenderTroopViewsArray.indices) {
                if (i < defenderArmy.maxSlots) {
                    val troop = manager.getDefenderArmy().getTroopAt(i)
                    if (troop == null) {
                        defenderTroopViewsArray[i]?.perish()
                        defenderTroopViewsArray[i] = null
                    } else {
                        defenderTroopViewsArray[i]?.updateStats()
                    }
                }
            }
        }

    }

    /**
     * Finds the TroopBattleView corresponding to the given TroopInfo.
     *
     * @param troop The TroopInfo for which the view is needed.
     * @return The corresponding TroopBattleView, or null if not found.
     */
    fun getTroopViewFor(troop: Troop): TroopBattleView? {
        // Search in the attacker's troop views
        attackerTroopViewsArray.forEach { troopView ->
            if (troopView?.getTroopInfo() == troop) {
                return troopView
            }
        }

        // Search in the defender's troop views
        defenderTroopViewsArray.forEach { troopView ->
            if (troopView?.getTroopInfo() == troop) {
                return troopView
            }
        }

        return null // Not found
    }

    suspend fun waitForPlayerAction(): Pair<BattleCommand, TileGroup> {
        return suspendCancellableCoroutine { continuation ->
            onPlayerActionReceived = { commandAndTileGroup ->
                continuation.resume(commandAndTileGroup)
            }

            continuation.invokeOnCancellation {
                onPlayerActionReceived = null
            }
        }
    }

    private var onPlayerActionReceived: ((Pair<BattleCommand, TileGroup>) -> Unit)? = null

    private fun handleActionError(rejection: BattleRejection?) {
        when (rejection) {
            BattleRejection.TOO_FAR -> showError("Target is too far away!")
            BattleRejection.OCCUPIED_BY_ALLY -> showError("Target tile is occupied by an ally!")
            BattleRejection.NOT_IMPLEMENTED -> showError("This action is not implemented yet!")
            BattleRejection.INVALID_TARGET -> showError("Invalid target!")
            else -> showError("An unknown error occurred!")
        }
    }

    /**
     * Displays an error message to the user.
     *
     * @param message The error message to display.
     */
    private fun showError(message: String) {
        // Простое сообщение в консоль для отладки
        println("Error: $message")

        // Not tested below:
        /*
        // Отобразим сообщение игроку, например, через всплывающее окно
        val errorLabel = Label(message, BaseScreen.skin).apply {
            color = Color.RED
            setFontScale(1.2f)
            setPosition(stage.width / 2 - width / 2, stage.height - 100f)
        }

        stage.addActor(errorLabel)

        // Удалим сообщение через 3 секунды
        errorLabel.addAction(
            Actions.sequence(
            Actions.delay(3f),
            Actions.fadeOut(0.5f),
            Actions.removeActor()
        ))

         */
    }



    /**
     * Draws a pointer to the currently active troop.
     */
    fun draw_pointer() {
        // Find a tileGroup with specified pointer position
        // TODO: switch pointerPosition to TileGroup
        val pointerTile = daTileGroups.firstOrNull { it.tileInfo.position == pointerPosition }
        if (pointerTile == null) {
            println("Error: No tile found at position $pointerPosition during drawing a pointer")
            return // Или другая логика для обработки отсутствия тайла
        }
        for (pointerImage in pointerImages) {
            // Note that here fixed sizes are used. Must be improved.
            pointerImage.setScale(pointerTile.width / 256f, pointerTile.width / 256f * 0.5f)
            pointerImage.setPosition(0f, pointerTile.height * 0.15f)

            pointerImage.setOrigin(pointerTile.originX, pointerTile.originY)
            pointerImage.touchable = Touchable.disabled
            pointerImage.name = "pointer"

            // Set the pointer image color to white (but it is black in fact)
            pointerImage.color = Color.WHITE


            // Here we find an actor devoted to a troop and put a pointer underneath
            //val act = pointerTile.findActor("troopGroup")
            //if(pointerTile.findActor("troopGroup") == null)
            val troopGroupActor = pointerTile.findActor<Group>("troopGroup")
            if (troopGroupActor != null) {
                pointerTile.addActorBefore(troopGroupActor, pointerImage)
            } else {
                println("Warning: troopGroup not found in pointerTile. Adding pointerImage directly.")
                pointerTile.addActor(pointerImage)
            }

            //pointerTile.addActorBefore(pointerTile.findActor("troopGroup"), pointerImage)
        }

        // Now we highlight achievable hexes by transparency. First of all we make all hexes non-transparent.
        //for (tileGroup in daTileGroups)
        //    tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 1f)
        // TODO: Principally it works, but we need to fix coordinates conversions and distances. UPD maybe fixed
        // var achievableHexes = daTileGroups.filter { manager.isHexAchievable(it.tileInfo.position) }
        // for (achievableHex in achievableHexes)
        //       achievableHex.baseLayerGroup.color = Color(1f,1f,1f,0.7f)
        //  }
    }

    /**
     * Adds tiles to create a rectangular array of battlefield tiles.
     */
    fun addTiles(){

        tileGroupMap = TileGroupMap(daTileGroups)

        // Draw attacking troops
        attackerTroopViewsArray.forEach { troopView ->
            if (troopView != null) {
                val troopTile = manager.getTroopTile(troopView.getTroopInfo())
                val troopTileGroup = daTileGroups.firstOrNull {
                    it.tileInfo == troopTile
                }
                if (troopTileGroup != null)
                    troopView.draw(troopTileGroup, attacker = true)
                else
                    println("Warning: no tile group found for troop ${troopView.getTroopInfo().unitName}")
            }
        }

// Draw defending troops
        defenderTroopViewsArray.forEach { troopView ->
            if (troopView != null) {
                val troopTile = manager.getTroopTile(troopView.getTroopInfo())
                val troopTileGroup = daTileGroups.firstOrNull { it.tileInfo == troopTile }
                if (troopTileGroup != null)
                    troopView.draw(troopTileGroup, attacker = false)
                else
                    println("Warning: no tile group found for troop ${troopView.getTroopInfo().unitName}")
            }
        }



        // Set pointer to first troop
        val currentTroop = manager.getCurrentTroop()
        if (currentTroop == null) {
            println("No troops in both armies at the beggining of the battle")
            shutdownScreen()
            return
        }
        val currentTile = manager.getTroopTile(currentTroop)
            ?: throw IllegalStateException("Current troop has no tile: $currentTroop")

        // TODO: switch to tile
        pointerPosition = currentTile.position
        draw_pointer()

        // Add various mouse listeners to each tile
        for (tileGroup in daTileGroups)
        {
            // Right mouse click listener
            tileGroup.addListener(object : ClickListener() {
                override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                    if(manager.isBattleOn()) {
                        // TODO: it is better to use width directly from Hexagon actor rather than baseLayerGroup actors
                        chooseCrosshair(tileGroup, x, y, tileGroup.baseLayerGroup.width)
                    }
                        return super.mouseMoved(event, x, y)
                }

                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if(manager.isBattleOn()) {
                        // Выводим координаты тайла в консоль
                        println("Tile clicked: position=${tileGroup.tileInfo.position}")

                        // Или через логгирование GDX
                        Gdx.app.log(
                            "TileClick",
                            "Tile clicked at position=${tileGroup.tileInfo.position}"
                        )

                        handleTileClick(tileGroup, x, y) // Обрабатываем клик на тайл
                    }
                }

                override fun enter(
                    event: InputEvent?,
                    x: Float,
                    y: Float,
                    pointer: Int,
                    fromActor: Actor?
                ) {
                    if(manager.isBattleOn()) {
                        // Highlight a tile as currently targeted by mouse pointer
                        tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 0.5f)

                        // Choose apropriate crosshair
                        if (fromActor != null) {
                            val width = fromActor.width
                            chooseCrosshair(tileGroup, x, y, width)
                        }

                        super.enter(event, x, y, pointer, fromActor)
                    }
                }

                // Restore the tile after mouse pointer exited it
                override fun exit(
                    event: InputEvent?,
                    x: Float,
                    y: Float,
                    pointer: Int,
                    toActor: Actor?
                ) {
                    // TODO: This must be rewritten to avoid code doubling
                    if(manager.isBattleOn()) {
                        val currentTroopNew = manager.getCurrentTroop()

                        val currentTroopOnTile = getCurrentTroopView() ?: return
                        if (currentTroopNew != null && manager.getReachableTiles(currentTroopNew).contains(tileGroup.tileInfo))                        /*
                        if (manager.isHexAchievable(
                                    currentTroopOnTile.getTroopInfo(),
                                    tileGroup.tileInfo.position
                                )
                        )

                         */
                            tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 0.7f)
                        else
                            tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 1f)

                        super.exit(event, x, y, pointer, toActor)
                    }


                }
            })

            allTileGroups.add(tileGroup)

            tileGroups[tileGroup.tileInfo] = listOf(tileGroup)
        }

        for (tileGroup in allTileGroups) {

            tileGroup.showEntireMap = true
            tileGroup.update()
        }

        tileGroupMap.setSize(stage.width, stage.height)
        stage.addActor(tileGroupMap)

    }

    private fun handleTileClick(tileGroup: TileGroup, x: Float, y: Float) {
        if (onPlayerActionReceived == null) {
            println("Player action is not expected at the moment.")
            return
        }

        val currentTroopView = getCurrentTroopView()
        if (currentTroopView == null) {
            println("Error: No current troop selected for player action.")
            return
        }

        val currentTroop = currentTroopView.getTroopInfo()
        val targetTile = tileGroup.tileInfo
        val canShoot = manager.canShoot(currentTroop)
        val targetIsEnemy = manager.isTileOccupiedByEnemy(currentTroop, targetTile)
        val attackFrom = if (targetIsEnemy && !canShoot) {
            val direction = pixelToDirection(x, y, tileGroup.baseLayerGroup.width)
            battleField.getNeighborTile(targetTile, direction)?.toPoint()
        } else null
        val targetIsReachable = if (targetIsEnemy) {
            false
        } else {
            manager.isTileAchievable(currentTroop, targetTile)
        }

        val command = BattleScreenCommandMapper.map(
            BattleScreenCommandMapper.Input(
                troopId = currentTroop.id,
                target = targetTile.toPoint(),
                canShoot = canShoot,
                targetIsEnemy = targetIsEnemy,
                targetIsReachable = targetIsReachable,
                attackFrom = attackFrom
            )
        ) ?: return

        onPlayerActionReceived?.invoke(Pair(command, tileGroup))
    }

    /**
     * Returns the current troop view based on the troop in the manager's queue.
     *
     * @return The view for the current troop, or null if not found.
     */
    fun getCurrentTroopView(): TroopBattleView? {
        val currentTroop = manager.getCurrentTroop()
        return attackerTroopViewsArray.find { it?.getTroopInfo() == currentTroop }
            ?: defenderTroopViewsArray.find { it?.getTroopInfo() == currentTroop }
    }

    /**
     * Updates tile shadowing based on the achievable positions for the current troop.
     */
    private fun updateTilesShadowing(){
        //val currentTroop = getCurrentTroopView() ?: return
        val currentTroop = manager.getCurrentTroop()


        // TODO: Principally it works, but we need to fix coordinates conversions and distances. UPD maybe fixed
        daTileGroups.forEach {
            if (currentTroop != null && manager.getReachableTiles(currentTroop).contains(it.tileInfo))

            //if (manager.isHexAchievable(currentTroop.getTroopInfo(), it.tileInfo.position))
                it.baseLayerGroup.color = Color(1f,1f,1f,0.7f)
            else
                it.baseLayerGroup.color = Color(1f,1f,1f,1f)


        }
    }

    /**
     * Moves the pointer to the next troop in the turn queue.
     */
    fun movePointerToNextTroop() {
        val currentTroop = manager.getCurrentTroop()
        if (currentTroop != null){
            val currentTile = manager.getTroopTile(currentTroop)
                ?: throw IllegalStateException("Current troop has no tile: $currentTroop")
            pointerPosition = currentTile.position
            draw_pointer()
        } else
            println("Queue is empty, nowhere to put pointer")
    }

    /**
     * Displays a morale bird animation above the specified troop view.
     *
     * @param troopView The view of the troop to display the morale bird for.
     */
    fun showMoraleBird(troopView: TroopBattleView) {
        val troopGroup = troopView.getCurrentGroup()

        // Ищем потомка с именем "troopImage"
        val troopImageActor = troopGroup.findActor<Image>("troopImage")
        if (troopImageActor == null) {
            println("Error: No actor named 'troopImage' found in troop group.")
            return
        }

        // Учитываем размеры и масштаб "troopImage"
        val troopWidth = troopImageActor.width * troopImageActor.scaleX
        val troopHeight = troopImageActor.height * troopImageActor.scaleY

        // Создаем изображение птицы морали
        moraleImage.apply {
            setScale(0.075f) // Настраиваем масштаб
            // TODO: those numbers are magical, we need to apply normal object hierarchy

            setPosition(
                troopWidth * -0.035f,  // Сдвиг влево
                troopHeight * 1.10f    // Сдвиг вверх
            )
            touchable = Touchable.disabled // Птица не кликабельна
            color = Color.WHITE.cpy().apply { a = 0f } // Начальная прозрачность
            name = "moraleBirdImage" // Уникальное имя для отладки
        }

        // Анимация: появление -> задержка -> исчезновение
        val fadeIn = Actions.alpha(1f, 0.5f)  // Плавное появление (0.5 секунды)
        val delay = Actions.delay(0.3f)       // Задержка (0.3 секунды)
        val fadeOut = Actions.alpha(0f, 0.5f) // Плавное исчезновение (0.5 секунды)
        val removeActor = Actions.run {
            moraleImage.remove() // Удаляем изображение после завершения анимации
        }

        // Привязываем последовательность действий к изображению
        moraleImage.addAction(Actions.sequence(fadeIn, delay, fadeOut, removeActor))

        // Добавляем изображение в группу отряда
        troopGroup.addActor(moraleImage)
    }

    /**
     * Displays a luck rainbow animation above the specified troop view.
     *
     * @param troopView The view of the troop to display the luck rainbow for.
     */
    fun showLuckRainbow(troopView: TroopBattleView) {
        val troopGroup = troopView.getCurrentGroup()

        // Ищем потомка с именем "troopImage"
        val troopImageActor = troopGroup.findActor<Image>("troopImage")
        if (troopImageActor == null) {
            println("Error: No actor named 'troopImage' found in troop group.")
            return
        }

        // Учитываем размеры и масштаб "troopImage"
        val troopWidth = troopImageActor.width * troopImageActor.scaleX
        val troopHeight = troopImageActor.height * troopImageActor.scaleY

        // Создаем изображение радуги удачи
        luckRainbowImage.apply {
            val scaleRainbowImage = 0.130f
            setScale(scaleRainbowImage) // Настраиваем масштаб
            //setOrigin(Align.center)
            align = Align.topRight
            // TODO: those numbers are magical, we need to apply normal object hierarchy
            setPosition(-width*scaleRainbowImage*0.2f,height*scaleRainbowImage*0.4f)
            //setPosition(
            //    troopWidth * -0.25f,  // Сдвиг влево
            //    troopHeight * 0.30f    // Сдвиг вверх
            //)

            //setPosition(0f, 0f, Align.center)
            //setPosition(
             //   troopWidth * 0f - width/2/0.13f,  // Сдвиг влево
            //    troopHeight * 0.55f  // Сдвиг вверх
            //)
            touchable = Touchable.disabled // Радуга не кликабельна
            color = Color.WHITE.cpy().apply { a = 0f } // Начальная прозрачность
            name = "luckRainbowImage" // Уникальное имя для отладки
        }

        // Анимация: появление -> задержка -> исчезновение
        val fadeIn = Actions.alpha(1f, 0.5f)  // Плавное появление (0.5 секунды)
        val delay = Actions.delay(0.3f)       // Задержка (0.3 секунды)
        val fadeOut = Actions.alpha(0f, 0.5f) // Плавное исчезновение (0.5 секунды)
        val removeActor = Actions.run {
            luckRainbowImage.remove() // Удаляем изображение после завершения анимации
        }

        // Привязываем последовательность действий к изображению
        luckRainbowImage.addAction(Actions.sequence(fadeIn, delay, fadeOut, removeActor))

        // Добавляем изображение в группу отряда
        troopGroup.addActor(luckRainbowImage)
    }

    fun refreshTargetTroop(targetHex: Vector2) {

    }

    /**
     * Selects the appropriate crosshair cursor based on the tile state.
     *
     * @param tileGroup The tile group the pointer is hovering over.
     * @param x The x-coordinate of the pointer relative to the tile.
     * @param y The y-coordinate of the pointer relative to the tile.
     * @param width The width of the hexagon tile.
     */
    fun chooseCrosshair(tileGroup: TileGroup, x: Float, y: Float, width: Float) {
        val targetTile = tileGroup.tileInfo
        val currentTroop = getCurrentTroopView() ?: return

        if (manager.canShoot(currentTroop.getTroopInfo()) &&
                manager.isTileOccupiedByEnemy(currentTroop.getTroopInfo(), targetTile)) {
            Gdx.graphics.setCursor(cursorShoot)
            return
        }

        if (!manager.getReachableTiles(currentTroop.getTroopInfo()).contains(tileGroup.tileInfo)
                && manager.isTileFree(targetTile)) {
            Gdx.graphics.setCursor(cursorCancel)
        } else {
            if (manager.isTileOccupiedByAlly(currentTroop.getTroopInfo(), tileGroup.tileInfo)) {
                Gdx.graphics.setCursor(cursorCancel)
                return
            }

            if (manager.isTileOccupiedByEnemy(currentTroop.getTroopInfo(), targetTile)) {
                val direction = pixelToDirection(x, y, width)
                val tileToMove = battleField.getNeighborTile(targetTile, direction)

                if (manager.getReachableTiles(currentTroop.getTroopInfo()).contains(tileToMove)
                        && (tileToMove != null && manager.isTileFree(tileToMove)
                                || tileToMove == manager.getTroopTile(currentTroop.getTroopInfo())))
                    Gdx.graphics.setCursor(cursorAttack[direction.num])
                else
                    Gdx.graphics.setCursor(cursorCancel)
                return
            }

            Gdx.graphics.setCursor(cursorMove)
        }
    }    /**
     * Determines the direction of the attack based on the mouse pointer position.
     *
     * @param x The x-coordinate of the pointer relative to the tile.
     * @param y The y-coordinate of the pointer relative to the tile.
     * @param width The width of the hexagon tile.
     * @return The direction of the attack.
     */
    fun pixelToDirection(x: Float, y: Float, width: Float): Direction{
        // width of the hex

        val x0 = width/2
        val height = width * 1.1547f
        val y0 = x0 * 0.577f // tangents of 30 degrees

        // Here we divide the hex with defender into 6 triangles in order to show from which adjacent hex attack will be mad
        // We have three diagonal lines intersecting at the center of the hex:

        when{
            y - (height - y0) + x * (height - y0) / (3f * x0) >= 0 &&
                    x <= x0
            -> return Direction.TopLeft

            y - (height - y0) + x * (height - y0) / (3f * x0) < 0 &&
                    y - y0 - x * y0 / x0 >= 0
            -> return Direction.CenterLeft

            y - y0 - x * y0 / x0 < 0 &&
                    x <= x0
            -> return Direction.BottomLeft

            y - (height - y0) + x * (height - y0) / (3f * x0) < 0 &&
                    x > x0
            -> return Direction.BottomRight

            y - (height - y0) + x * (height - y0) / (3f * x0) >= 0 &&
                    y - y0 - x * y0 / x0 < 0
            -> return Direction.CenterRight

            y - y0 - x * y0 / x0 >= 0 &&
                    x > x0
            -> return Direction.TopRight
        }
        return Direction.DirError

    }

     /**
     * Resumes the screen. Used to replace the current screen after recreation.
     */
    override fun resume() {
        game.replaceCurrentScreen(recreate())
    }

    /**
     * Recreates the screen instance.
     *
     * @return A new instance of [BattleScreen].
     */
    override fun recreate(): BaseScreen {
        val a = attacker ?: return this
        val d = defender ?: return this
        return fromCombatants(a, d)
    }

    override fun resize(width: Int, height: Int) {
        // BattleScreen не пересоздаётся при ресайзе — это вызывает бесконечную рекурсию
        stage.viewport.update(width, height, true)
    }

    fun resizePage(tab: EmpireOverviewTab) {
    }

    /**
     * Shuts down the battle screen and performs cleanup.
     * Ensures the default system cursor is restored and the game screen stack is updated.
     */
    private fun shutdownScreen()
    {
        // Change cursor to arrow, default for map view.
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow)
        game.popScreen()

    }

    private val battleScope: kotlinx.coroutines.CoroutineScope
        get() = BattleScreenScopeRegistry.scopeFor(this)
    private fun handleApplicationEvent(event: com.unciv.pure.application.battle.BattleEvent) {
        when (event) {
            is com.unciv.pure.application.battle.BattleEvent.TroopMoved -> Gdx.app.postRunnable {
                val troop = manager.getTroopById(event.troopId) ?: return@postRunnable
                val troopView = getTroopViewFor(troop) ?: return@postRunnable
                val newTile = daTileGroups.firstOrNull {
                    it.tileInfo.position.x.toInt() == event.to.x &&
                            it.tileInfo.position.y.toInt() == event.to.y
                }
                troopView.updatePosition(newTile)
                refreshTroopViews()
                if (event.isMorale && manager.isBattleOn()) showMoraleBird(troopView)
            }

            is com.unciv.pure.application.battle.BattleEvent.TroopAttacked -> Gdx.app.postRunnable {
                val attacker = manager.getTroopById(event.attackerId) ?: return@postRunnable
                val attackerView = getTroopViewFor(attacker) ?: return@postRunnable
                if (event.isLuck) showLuckRainbow(attackerView)
                attackerView.updatePosition(daTileGroups.firstOrNull {
                    it.tileInfo == manager.getTroopTile(attacker)
                })
                refreshTroopViews()
                if (event.isMorale && manager.isBattleOn()) showMoraleBird(attackerView)
            }

            is com.unciv.pure.application.battle.BattleEvent.TroopShot -> Gdx.app.postRunnable {
                val attacker = manager.getTroopById(event.attackerId) ?: return@postRunnable
                val attackerView = getTroopViewFor(attacker) ?: return@postRunnable
                if (event.isLuck) showLuckRainbow(attackerView)
                refreshTroopViews()
                if (event.isMorale && manager.isBattleOn()) showMoraleBird(attackerView)
            }

            is com.unciv.pure.application.battle.BattleEvent.TurnAdvanced ->
                println("[EVENT] TurnAdvanced: nextTroop=${event.nextTroopId}")

            is com.unciv.pure.application.battle.BattleEvent.BattleEnded -> Gdx.app.postRunnable {
                shutdownScreen()
                manager.finishBattle()
                val battleResult = manager.getBattleResult()
                if (battleResult == null) {
                    println("Bug with battle result.")
                } else {
                    if (verboseTurn)
                        println("Army of ${battleResult.winningArmy.civInfo.nation.name} won.")
                    com.unciv.logic.battle.BattleWorldOutcomeHandler(attacker, defender)
                        .apply(attackerArmy == battleResult.winningArmy)
                }
            }

            com.unciv.pure.application.battle.BattleEvent.TurnSkipped ->
                println("[EVENT] TurnSkipped")
        }
    }
}

