package com.unciv.ui.playground

import com.unciv.battle.protocol.BattleProtocol
import com.unciv.infrastructure.battle.network.BattleHttpTransport
import com.unciv.infrastructure.battle.network.RemoteBattleLobby
import com.unciv.infrastructure.battle.network.RemoteRoomPhase
import com.unciv.infrastructure.battle.network.RemoteRoomState
import com.unciv.logic.arena.ArenaBattleSetup
import com.unciv.logic.battle.session.BattleRecruitmentRules
import com.unciv.pure.application.battle.session.BattleSessionSnapshot
import com.unciv.pure.application.battle.session.BattleStateSnapshot
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.ui.battlescreen.NetworkArmySetupScreen
import com.unciv.ui.utils.BaseScreen
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking

/** Production recruitment screen with deterministic, offline room data. */
fun createNetworkArmySetupPreview(): BaseScreen {
    val transport = object : BattleHttpTransport {
        override suspend fun request(method: String, path: String, body: String?, token: String?): String {
            if (method == "POST" && path == "/v1/rooms") {
                return """{"protocolVersion":${BattleProtocol.VERSION},"roomCode":"TESTROOM","playerToken":"${"t".repeat(43)}","side":"ATTACKER"}"""
            }
            // Disposal cancels polling; no network request or mutable room state is needed.
            if (method == "GET") awaitCancellation()
            error("The layout preview must not submit an army")
        }
    }
    val client = runBlocking { RemoteBattleLobby(transport).create("army-budget") }
    val initial = RemoteRoomState(
        scenarioId = "army-budget",
        side = BattleSide.ATTACKER,
        phase = RemoteRoomPhase.WAITING,
        opponentJoined = true,
        attackerReady = false,
        defenderReady = false,
        battle = BattleSessionSnapshot(
            0L, 0L, null,
            BattleStateSnapshot(emptyList(), emptyList(), 0, null, null, false, null)
        ),
        recruitment = BattleRecruitmentRules.from(ArenaBattleSetup.ruleset())
    )
    return NetworkArmySetupScreen(client, initial) { _, _ ->
        error("The layout preview must not start a battle")
    }
}
