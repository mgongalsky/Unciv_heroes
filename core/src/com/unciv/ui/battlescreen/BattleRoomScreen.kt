package com.unciv.ui.battlescreen

import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.infrastructure.battle.network.RemoteBattleClient
import com.unciv.infrastructure.battle.network.RemoteBattleLobby
import com.unciv.infrastructure.battle.network.RemoteRoomPhase
import com.unciv.infrastructure.battle.network.RemoteRoomState
import com.unciv.infrastructure.battle.network.RemoteScenario
import com.unciv.infrastructure.battle.network.UrlBattleHttpTransport
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.unciv.utils.concurrency.Dispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/** Room operations run through the remote API; all widget updates run on Dispatcher.GL. */
class BattleRoomScreen(
    private val onBattleReady: (RemoteBattleClient, RemoteRoomState) -> Unit,
    initialAddress: String = "http://127.0.0.1:8081"
) : BaseScreen() {
    private val pasteCodeButton = "Paste".toTextButton().apply {
        onClick { roomCode.pasteCode() }
    }
    private val copyCodeButton = "Copy code".toTextButton().apply {
        onClick {
            val code = client?.roomCode ?: roomCode.text.trim()
            if (code.isNotEmpty()) {
                com.badlogic.gdx.Gdx.app.clipboard.contents = code
                setText("Copied!")
                clearActions()
                addAction(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                        com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(2f),
                        com.badlogic.gdx.scenes.scene2d.actions.Actions.run { setText("Copy code") }
                    ))
            }
        }
    }
    private val address = TextField(initialAddress, skin)
    private val roomCode = BattleRoomCodeField(skin) { message -> status.setText(message) }
    private val scenarios = SelectBox<String>(skin)
    private val status = "Enter the server address and load scenarios, or join by room code.".toLabel().apply {
        setWrap(true)
    }
    private val roomStatus = "".toLabel().apply { setWrap(true) }
    private val loadButton = "Load scenarios".toTextButton()
    private val createButton = "Create room".toTextButton()
    private val joinButton = "Join room".toTextButton()
    private val readyButton = "Ready".toTextButton()
    private val refreshButton = "Refresh".toTextButton()
    private var scope: CoroutineScope? = null
    private var polling: Job? = null
    private var busy = false
    private var handedOff = false
    private var client: RemoteBattleClient? = null
    private var catalog = emptyList<RemoteScenario>()
    private var catalogAddress: String? = null
    private var joinAttempt: Pair<String, String>? = null
    private var joinNonce: String? = null
    private var lastRoom: RemoteRoomState? = null

    init {
        val content = Table().apply {
            defaults().pad(8f)
            add("Network battle".toLabel()).colspan(2).padBottom(16f).row()
            add("Server address".toLabel()).left()
            add(address).width(480f).height(44f).row()
            add(loadButton).colspan(2).height(44f).row()
            add("Scenario".toLabel()).left()
            add(scenarios).width(480f).height(44f).row()
            add(createButton).colspan(2).height(44f).row()
            add("Room code".toLabel()).left()
            add(Table().apply {
                add(roomCode).growX().height(44f)
                add(pasteCodeButton).padLeft(8f).width(90f).height(44f)
                add(copyCodeButton).padLeft(8f).width(130f).height(44f)
            }).width(480f).row()
            add(joinButton).colspan(2).height(44f).row()
            add(roomStatus).colspan(2).width(660f).row()
            add(Table().apply {
                defaults().pad(6f).height(44f)
                add(readyButton).width(180f)
                add(refreshButton).width(180f)
            }).colspan(2).row()
            add(status).colspan(2).width(660f).minHeight(60f).row()
            add("Back".toTextButton().onClick { leave() }).colspan(2).height(44f)
        }
        stage.addActor(Table().apply {
            setFillParent(true)
            add(com.unciv.ui.utils.AutoScrollPane(content)).grow()
        })
        loadButton.onClick {
            val server = address.text.trim()
            perform {
                val loaded = RemoteBattleLobby(UrlBattleHttpTransport(server)).scenarios()
                catalog = loaded
                catalogAddress = server
                scenarios.setItems(*loaded.map { it.title }.toTypedArray())
                status.setText(if (loaded.isEmpty()) "No scenarios available." else "Choose a scenario or join a room.")
            }
        }
        createButton.onClick {
            val server = address.text.trim()
            val selected = catalog.getOrNull(scenarios.selectedIndex)
            if (server != catalogAddress || selected == null) {
                status.setText("Load scenarios for this server first.")
            } else perform {
                client = RemoteBattleLobby(UrlBattleHttpTransport(server)).create(selected.id)
                roomCode.text = requireNotNull(client).roomCode
                accept(requireNotNull(client).room())
            }
        }
        joinButton.onClick {
            val server = address.text.trim()
            val code = roomCode.text.trim()
            val attempt = server to code
            if (joinAttempt != attempt) {
                joinAttempt = attempt
                joinNonce = UUID.randomUUID().toString().replace("-", "")
            }
            val nonce = requireNotNull(joinNonce)
            perform {
                client = RemoteBattleLobby(UrlBattleHttpTransport(server)).join(code, nonce)
                accept(requireNotNull(client).room())
            }
        }
        readyButton.onClick {
            val connected = client
            if (connected != null) perform { accept(connected.ready()) }
        }
        refreshButton.onClick {
            val connected = client
            if (connected != null) perform { accept(connected.room()) }
        }
        globalShortcuts.add(KeyCharAndCode.BACK) { leave() }
        updateControls()
    }

    override fun show() {
        super.show()
        if (scope == null) scope = CoroutineScope(SupervisorJob() + Dispatcher.GL)
        busy = false
        updateControls()
        startPolling()
    }

    override fun hide() {
        stopRequests()
        super.hide()
    }

    override fun dispose() {
        stopRequests()
        super.dispose()
    }

    private fun stopRequests() {
        polling?.cancel()
        polling = null
        scope?.cancel()
        scope = null
    }

    private fun leave() {
        stopRequests()
        game.popScreen()
    }

    private fun perform(action: suspend () -> Unit) {
        val owner = scope ?: return
        if (busy || handedOff) return
        polling?.cancel()
        polling = null
        busy = true
        updateControls()
        status.setText("Connecting…")
        owner.launch {
            try {
                action()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                status.setText("Request failed. Check the address and connection, then retry. If a room was created but its code was not received, it will expire automatically.")
            } finally {
                if (scope === owner && isActive) {
                    busy = false
                    updateControls()
                    startPolling()
                }
            }
        }
    }

    private fun startPolling() {
        val owner = scope ?: return
        if (client == null || busy || handedOff || polling?.isActive == true) return
        polling = owner.launch {
            while (isActive && !handedOff) {
                delay(1500)
                try {
                    accept(requireNotNull(client).room())
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    status.setText("Connection interrupted. Retrying; your room credentials are retained.")
                    delay(3500)
                }
            }
        }
    }

    private fun accept(room: RemoteRoomState) {
        lastRoom = room
        val connected = requireNotNull(client)
        if (!handedOff && room.recruitment != null && room.phase == RemoteRoomPhase.WAITING) {
            handedOff = true
            updateControls()
            game.replaceCurrentScreen(NetworkArmySetupScreen(connected, room, onBattleReady))
            return
        }
        roomStatus.setText(
            "Room: ${connected.roomCode}   Your side: ${connected.side}\n" +
                    "Attacker: ${if (room.attackerReady) "ready" else "waiting"}   " +
                    "Defender: ${if (!room.opponentJoined) "not joined" else if (room.defenderReady) "ready" else "waiting"}"
        )
        status.setText(
            when (room.phase) {
                RemoteRoomPhase.WAITING -> "Share the room code with your opponent. Both players must press Ready."
                RemoteRoomPhase.ACTIVE -> "Battle ready."
                RemoteRoomPhase.FINISHED -> "Battle finished."
                RemoteRoomPhase.CLOSED -> "Room closed."
            }
        )
        updateControls()
        if (!handedOff && (room.phase == RemoteRoomPhase.ACTIVE || room.phase == RemoteRoomPhase.FINISHED)) {
            requireNotNull(room.battle.state.field) { "Server did not provide battlefield terrain" }
            handedOff = true
            updateControls()
            onBattleReady(connected, room)
        }
    }

    private fun updateControls() {
        val connected = client != null
        address.isDisabled = busy || connected || handedOff
        roomCode.isDisabled = busy || connected || handedOff
        pasteCodeButton.isDisabled = roomCode.isDisabled
        scenarios.isDisabled = busy || connected || handedOff || catalog.isEmpty()
        loadButton.isDisabled = busy || connected || handedOff
        createButton.isDisabled = busy || connected || handedOff || catalog.isEmpty()
        joinButton.isDisabled = busy || connected || handedOff
        readyButton.isDisabled =
                busy || !connected || handedOff || lastRoom?.phase == RemoteRoomPhase.CLOSED
        refreshButton.isDisabled = busy || !connected || handedOff
    }
}
