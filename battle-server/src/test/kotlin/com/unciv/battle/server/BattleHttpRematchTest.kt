package com.unciv.battle.server

import com.unciv.battle.protocol.WireScenario
import com.unciv.infrastructure.battle.network.*
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import java.io.IOException
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BattleHttpRematchTest {
    private class Engine : BattleSessionEngine {
        override var isFinished = false
        override val currentSide get() = if (isFinished) null else BattleSide.ATTACKER
        override val currentTroopId get() = if (isFinished) null else 1
        private fun troop(id: Int, side: BattleSide, amount: Int) = BattleTroopSnapshot(
            id, side, 0, "Spearman", 3, amount, 10, 10, 5, 2, 0,
            0, 0, null, 0, 0, false, false, 0, 0)
        override fun snapshot() = BattleStateSnapshot(
            if (isFinished) listOf(troop(1, BattleSide.ATTACKER, 1)) else
                listOf(troop(1, BattleSide.ATTACKER, 3), troop(2, BattleSide.DEFENDER, 3)),
            if (isFinished) emptyList() else listOf(1, 2), if (isFinished) 0 else 2,
            currentTroopId, currentSide, isFinished, if (isFinished) BattleSide.ATTACKER else null,
            availableCommands = if (isFinished) emptyList() else listOf(BattleCommand.Skip(1)))
        override fun execute(command: BattleCommand): BattleSessionExecution {
            isFinished = true
            return BattleSessionExecution(BattleCommandResult(success = true), emptyList())
        }
    }

    @Test
    fun lostRematchReplyRetainsSeatsAndNeverReplaysOldCommandsInNewBattle() = runBlocking {
        var created = 0
        val server = BattleHttpServer(InetSocketAddress("127.0.0.1", 0), BattleRooms({
            created++
            OrderedBattleSession(Engine())
        }), listOf(WireScenario("scenario", "Scenario", "")))
        try {
            server.start()
            val http = UrlBattleHttpTransport("http://127.0.0.1:${server.port}")
            var loseRematchReply = false
            val transport = object : BattleHttpTransport {
                override suspend fun request(method: String, path: String, body: String?, token: String?): String {
                    val response = http.request(method, path, body, token)
                    if (loseRematchReply && path.endsWith("/rematch")) {
                        loseRematchReply = false
                        throw IOException("Simulated lost rematch response")
                    }
                    return response
                }
            }
            val lobby = RemoteBattleLobby(transport)
            val a = lobby.create("scenario")
            val b = lobby.join(a.roomCode, "b".repeat(32))
            a.ready()
            b.ready()
            val first = BattleClientController(a, a.side)
            first.refresh()
            first.submit(BattleCommand.Skip(1))
            val report = a.room().report!!
            assertEquals(2, report.attackerLosses.single().amount)
            assertEquals(3, report.defenderLosses.single().amount)
            assertEquals(report, b.room().report)
            val waiting = a.rematch()
            assertTrue(waiting.attackerRematch)
            assertFalse(waiting.defenderRematch)
            assertEquals(1, waiting.battleNumber)
            loseRematchReply = true
            try {
                b.rematch()
                fail("The response should be lost after the server starts the next battle")
            } catch (_: IOException) { }
            val recovered = b.rematch()
            assertEquals(2, recovered.battleNumber)
            assertFalse(recovered.battle.state.finished)
            assertNull(recovered.report)
            assertEquals(recovered.battle.state, a.room().battle.state)
            assertEquals(2, a.rematch().battleNumber)
            assertEquals(2, created)
            try {
                a.submit(BattleSessionRequest(1, 0, BattleCommand.Skip(1)))
                fail("A stale client must not execute a command in the new battle")
            } catch (ex: BattleHttpException) { assertEquals(400, ex.status) }
            val nextA = a.forBattle(2)
            val nextB = b.forBattle(2)
            assertEquals(0L, nextA.read().revision)
            val second = BattleClientController(nextA, nextA.side)
            second.refresh()
            val finished = second.submit(BattleCommand.Skip(1))
            assertTrue(finished.state.finished)
            assertEquals(1L, finished.revision)
            assertEquals(finished.state, nextB.read().state)
            assertEquals(report, nextA.room().report)
            assertFalse(nextA.room().attackerRematch)
            assertFalse(nextB.room().defenderRematch)
        } finally { server.close() }
    }

    @Test
    fun purchasedBattleScoreIsSharedAndResetForRematch() = runBlocking {
        val selection = com.unciv.pure.application.battle.recruitment.BattleArmySelection(
            listOf(
                com.unciv.pure.application.battle.recruitment.BattleRecruitmentUnit(
                    "Spearman",
                    8
                )
            )
        )
        val rooms = BattleRooms(
            createSession = { OrderedBattleSession(Engine()) },
            recruitment = selection,
            createPurchasedSession = { _, _ -> OrderedBattleSession(Engine()) }
        )
        val server = BattleHttpServer(
            InetSocketAddress("127.0.0.1", 0), rooms,
            listOf(WireScenario(BattleRooms.RECRUITMENT_SCENARIO, "Build army", ""))
        )
        try {
            server.start()
            val lobby = RemoteBattleLobby(UrlBattleHttpTransport("http://127.0.0.1:${server.port}"))
            val a = lobby.create(BattleRooms.RECRUITMENT_SCENARIO)
            val b = lobby.join(a.roomCode, "s".repeat(32))
            val army = listOf(
                com.unciv.pure.application.battle.recruitment.BattleArmyStack(
                    "Spearman",
                    3
                )
            )
            a.ready(army)
            b.ready(army)
            val controller = BattleClientController(a, a.side)
            controller.refresh()
            controller.submit(BattleCommand.Skip(1))
            val report = requireNotNull(a.room().report)
            val score = requireNotNull(report.winnerScore)
            assertEquals(24L, score.initialValue)
            assertEquals(8L, score.remainingValue)
            assertEquals(100.0 / 3.0, score.percent, 0.0001)
            assertEquals(report, b.room().report)
            a.rematch()
            val next = b.rematch()
            assertEquals(2, next.battleNumber)
            assertNull(next.report)
            assertFalse(next.attackerReady)
            assertFalse(next.defenderReady)
            assertNull(a.room().report)
        } finally {
            server.close()
        }
    }
}
