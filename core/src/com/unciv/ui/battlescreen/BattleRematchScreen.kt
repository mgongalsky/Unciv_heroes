package com.unciv.ui.battlescreen

import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.infrastructure.battle.network.RemoteBattleClient
import com.unciv.infrastructure.battle.network.RemoteRoomState
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.unciv.utils.concurrency.Dispatcher
import kotlinx.coroutines.*

/** Keeps the authenticated room while both players review results and agree to another battle. */
class BattleRematchScreen(
    private val client: RemoteBattleClient,
    initial: RemoteRoomState,
    private val ruleset: Ruleset
) : BaseScreen() {
    private var scope: CoroutineScope? = null
    private var polling: Job? = null
    private var voteRequested = false
    private var changingScreen = false
    private var opened = false
    private val message = "Play again when you are ready.".toLabel().apply { setWrap(true) }
    private val replay = "Play again".toTextButton()
    private val popup = BattleResultPopup(this, requireNotNull(initial.report)) { leave() }.apply {
        children.filterIsInstance<TextButton>().forEach { it.setText("Leave room") }
        row()
        add(message).colspan(2).width(650f).minHeight(55f).padTop(12f).row()
        add(replay).colspan(2).width(240f).height(55f)
    }

    init {
        require(initial.battle.state.finished && initial.battleNumber == client.battleNumber)
        replay.onClick {
            voteRequested = true
            replay.isDisabled = true
            message.setText("Requesting another battle…")
            startPolling(immediate = true)
        }
        globalShortcuts.add(KeyCharAndCode.BACK) { leave() }
        showWaiting(initial)
    }

    override fun show() {
        super.show()
        if (scope == null) scope = CoroutineScope(SupervisorJob() + Dispatcher.GL)
        if (!opened) {
            opened = true
            popup.open(force = true)
        }
        startPolling(immediate = true)
    }

    private fun showWaiting(room: RemoteRoomState) {
        val ownVote = if (client.side == BattleSide.ATTACKER) room.attackerRematch else room.defenderRematch
        val otherVote = if (client.side == BattleSide.ATTACKER) room.defenderRematch else room.attackerRematch
        if (ownVote) voteRequested = true
        replay.isDisabled = voteRequested
        message.setText(when {
            ownVote -> "You are ready. Waiting for your opponent to press Play again."
            voteRequested -> "Sending your request for another battle…"
            otherVote -> "Your opponent wants to play again. Press Play again to start."
            else -> "Play again in this room, or leave when you are done."
        })
    }

    private fun accept(room: RemoteRoomState) {
        if (changingScreen) return
        if (room.battleNumber == client.battleNumber) {
            showWaiting(room)
            return
        }
        require(room.battleNumber > client.battleNumber) { "Battle number moved backwards" }
        val nextClient = client.forBattle(room.battleNumber)
        val next: BaseScreen = if (room.recruitment != null &&
                room.phase == com.unciv.infrastructure.battle.network.RemoteRoomPhase.WAITING
        ) {
            NetworkArmySetupScreen(nextClient, room) { connected, readyRoom ->
                game.replaceCurrentScreen(
                    BattleSessionScreen(
                        connected,
                        connected.side,
                        readyRoom.battle,
                        ruleset
                    )
                )
            }
        } else BattleSessionScreen(nextClient, client.side, room.battle, ruleset)
        changingScreen = true
        game.replaceCurrentScreen(next)
    }

    private fun startPolling(immediate: Boolean) {
        val owner = scope ?: return
        polling?.cancel()
        polling = owner.launch {
            if (!immediate) delay(1200)
            while (isActive && !changingScreen) {
                try {
                    var room = client.room()
                    if (room.battleNumber == client.battleNumber && voteRequested) {
                        val accepted = if (client.side == BattleSide.ATTACKER) room.attackerRematch else room.defenderRematch
                        if (!accepted) room = client.rematch()
                    }
                    accept(room)
                } catch (ex: CancellationException) {
                    throw ex
                } catch (_: Exception) {
                    message.setText("Connection interrupted. Retrying; your results and room are retained.")
                }
                delay(1200)
            }
        }
    }

    private fun leave() {
        stopRequests()
        game.popScreen()
    }
    private fun stopRequests() {
        scope?.cancel()
        scope = null
        polling = null
    }
    override fun hide() { stopRequests(); super.hide() }
    override fun dispose() { stopRequests(); super.dispose() }
}
