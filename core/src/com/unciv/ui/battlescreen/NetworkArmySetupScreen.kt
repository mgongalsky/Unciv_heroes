package com.unciv.ui.battlescreen

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.infrastructure.battle.network.RemoteBattleClient
import com.unciv.infrastructure.battle.network.RemoteRoomPhase
import com.unciv.infrastructure.battle.network.RemoteRoomState
import com.unciv.pure.application.battle.recruitment.BattleArmyStack
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.unciv.utils.concurrency.Dispatcher
import kotlinx.coroutines.*

/** Retains one immutable Ready payload until the server confirms it. */
class NetworkArmySetupScreen(
    private val client: RemoteBattleClient,
    initial: RemoteRoomState,
    private val onBattleReady: (RemoteBattleClient, RemoteRoomState) -> Unit
) : BaseScreen() {
    private val selection = requireNotNull(initial.recruitment)
    private val ready = "Ready".toTextButton()
    private val status = "Choose your army.".toLabel().apply { setWrap(true) }
    private var valid = false
    private var confirmed = false
    private var pending: List<BattleArmyStack>? = null
    private var changingScreen = false
    private var scope: CoroutineScope? = null
    private var polling: Job? = null
    private val picker = BattleArmyPicker(selection) {
        valid = it
        ready.isDisabled = !valid || confirmed || pending != null
    }

    init {
        require(initial.battleNumber == client.battleNumber)
        picker.setArmy(initial.ownArmy)
        val content = Table().apply {
            defaults().pad(8f)
            add("Room: ${client.roomCode} — ${client.side}".toLabel()).row()
            add("Copy room code".toTextButton().apply {
                onClick { com.badlogic.gdx.Gdx.app.clipboard.contents = client.roomCode }
            }).height(44f).row()
            add(picker).row()
            add(ready).height(48f).row()
            add(status).width(460f).minHeight(60f).row()
            add("Leave room".toTextButton().apply { onClick { leave() } }).height(44f)
        }
        stage.addActor(Table().apply { setFillParent(true); add(AutoScrollPane(content)).grow() })
        ready.onClick {
            if (!ready.isDisabled && pending == null && !confirmed && valid) {
                val chosen = picker.trySelectedArmy()
                if (chosen != null) {
                    pending = chosen
                    picker.setLocked(true)
                    ready.isDisabled = true
                    status.setText("Confirming your army…")
                    startPolling()
                }
            }
        }
        globalShortcuts.add(KeyCharAndCode.BACK) { leave() }
        accept(initial, allowTransition = false)
    }

    private fun accept(room: RemoteRoomState, allowTransition: Boolean = true) {
        require(room.battleNumber == client.battleNumber) { "Battle number changed during army selection" }
        val catalog = requireNotNull(room.recruitment)
        require(catalog.budget == selection.budget && catalog.maxStacks == selection.maxStacks && catalog.units == selection.units) {
            "Recruitment catalog changed during army selection"
        }
        confirmed = if (client.side == BattleSide.ATTACKER) room.attackerReady else room.defenderReady
        if (confirmed) {
            pending?.let { require(it == room.ownArmy) { "Server confirmed a different army" } }
            pending = null
            picker.setArmy(room.ownArmy)
        }
        picker.setLocked(confirmed || pending != null)
        ready.isDisabled = !valid || confirmed || pending != null
        status.setText(when {
            room.phase == RemoteRoomPhase.ACTIVE -> "Both armies are ready. Starting battle…"
            pending != null -> "Confirming your army…"
            confirmed && !room.opponentJoined -> "Army confirmed. Share the room code with your opponent."
            confirmed -> "Army confirmed. Waiting for your opponent."
            else -> "Choose up to ${selection.maxStacks} stacks, then press Ready."
        })
        if (allowTransition && !changingScreen && room.phase == RemoteRoomPhase.ACTIVE) {
            requireNotNull(room.battle.state.field)
            changingScreen = true
            onBattleReady(client, room)
        }
    }

    private fun startPolling() {
        val owner = scope ?: return
        if (polling?.isActive == true) return
        polling = owner.launch {
            while (isActive && !changingScreen) {
                try {
                    val room = client.room()
                    accept(room)
                    val request = pending
                    if (!changingScreen && !confirmed && request != null) accept(client.ready(request))
                } catch (ex: CancellationException) {
                    throw ex
                } catch (_: Exception) {
                    status.setText("Connection or confirmation failed. Retrying the same army; your room is retained.")
                }
                delay(1200)
            }
        }
    }

    override fun show() {
        super.show()
        if (scope == null) scope = CoroutineScope(SupervisorJob() + Dispatcher.GL)
        startPolling()
    }
    private fun leave() { stopRequests(); game.popScreen() }
    private fun stopRequests() { scope?.cancel(); scope = null; polling = null }
    override fun hide() { stopRequests(); super.hide() }
    override fun dispose() { stopRequests(); super.dispose() }
}
