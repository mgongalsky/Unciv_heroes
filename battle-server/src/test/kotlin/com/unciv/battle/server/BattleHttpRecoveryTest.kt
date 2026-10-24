package com.unciv.battle.server

import com.unciv.battle.protocol.WireScenario
import com.unciv.infrastructure.battle.network.BattleHttpTransport
import com.unciv.infrastructure.battle.network.RemoteBattleLobby
import com.unciv.infrastructure.battle.network.UrlBattleHttpTransport
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.BattleClientController
import com.unciv.pure.domain.battle.BattleSide
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress

class BattleHttpRecoveryTest {
    private class UnreliableTransport(private val delegate: BattleHttpTransport) : BattleHttpTransport {
        var loseBefore = false
        var loseAfter = false
        val commandBodies = mutableListOf<String>()
        override suspend fun request(method: String, path: String, body: String?, token: String?): String {
            val command = method == "POST" && path.endsWith("/commands")
            if (command) {
                commandBodies.add(requireNotNull(body))
                if (loseBefore) {
                    loseBefore = false
                    throw IOException("Simulated failure before delivery")
                }
            }
            val response = delegate.request(method, path, body, token)
            if (command && loseAfter) {
                loseAfter = false
                throw IOException("Simulated lost response after delivery")
            }
            return response
        }
    }

    @Test
    fun twoClientsRecoverLostAndUndeliveredCommandsWithoutDuplicateTurns() = runBlocking {
        val catalog =
                BattleScenarioCatalog(File(requireNotNull(System.getProperty("battle.assets"))))
        val server = BattleHttpServer(
            InetSocketAddress("127.0.0.1", 0), BattleRooms({ catalog.create(it, 42L) }),
            catalog.scenarios.map { WireScenario(it.id, it.title, it.description) }
        )
        try {
            server.start()
            val url = "http://127.0.0.1:${server.port}"
            val attackerTransport = UnreliableTransport(UrlBattleHttpTransport(url))
            val defenderTransport = UnreliableTransport(UrlBattleHttpTransport(url))
            val attacker = RemoteBattleLobby(attackerTransport).create("spearmen-vs-archers")
            val defender =
                    RemoteBattleLobby(defenderTransport).join(attacker.roomCode, "r".repeat(32))
            attacker.ready()
            defender.ready()
            val ports = mapOf(BattleSide.ATTACKER to attacker, BattleSide.DEFENDER to defender)
            val transports = mapOf(
                BattleSide.ATTACKER to attackerTransport,
                BattleSide.DEFENDER to defenderTransport
            )
            val controllers = ports.mapValues { (side, port) -> BattleClientController(port, side) }
            controllers.values.forEach { it.refresh() }
            val initial = attacker.read()
            assertEquals(initial.state, defender.read().state)
            assertEquals(112, requireNotNull(initial.state.field).cells.size)

            val firstSide = requireNotNull(initial.state.currentSide)
            val first = controllers.getValue(firstSide)
            val firstTransport = transports.getValue(firstSide)
            firstTransport.loseAfter = true
            val firstCommand =
                    initial.state.availableCommands.filterIsInstance<BattleCommand.Skip>().single()
            try {
                first.submit(firstCommand)
                fail("Expected lost response")
            } catch (_: IOException) {
            }
            val firstRequest = requireNotNull(first.pendingRequest)
            val recovered = first.refresh()
            val savedFirstReply = requireNotNull(first.lastReply)
            assertNull(first.pendingRequest)
            assertEquals(1L, recovered.revision)
            assertTrue(requireNotNull(savedFirstReply.result).success)
            assertEquals(1, firstTransport.commandBodies.size)
            assertEquals(recovered.state, attacker.read().state)
            assertEquals(recovered.state, defender.read().state)
            assertEquals(savedFirstReply, ports.getValue(firstSide).submit(firstRequest))
            assertEquals(1L, attacker.read().revision)

            val secondSide = requireNotNull(recovered.state.currentSide)
            val second = controllers.getValue(secondSide)
            val secondTransport = transports.getValue(secondSide)
            val beforeSecond = second.refresh()
            val requestsBefore = secondTransport.commandBodies.size
            secondTransport.loseBefore = true
            val secondCommand =
                    beforeSecond.state.availableCommands.filterIsInstance<BattleCommand.Skip>()
                        .single()
            try {
                second.submit(secondCommand)
                fail("Expected undelivered command")
            } catch (_: IOException) {
            }
            val pending = requireNotNull(second.pendingRequest)
            assertEquals(1L, second.refresh().revision)
            assertEquals(pending, second.pendingRequest)
            val afterRetry = second.retry()
            val savedSecondReply = requireNotNull(second.lastReply)
            assertNull(second.pendingRequest)
            assertEquals(2L, afterRetry.revision)
            val bodies = secondTransport.commandBodies.drop(requestsBefore)
            assertEquals(2, bodies.size)
            assertEquals(bodies[0], bodies[1])
            assertEquals(afterRetry.state, attacker.read().state)
            assertEquals(afterRetry.state, defender.read().state)
            assertEquals(initial.state.field, afterRetry.state.field)
            assertEquals(savedSecondReply, ports.getValue(secondSide).submit(pending))

            // Each seat retains its latest receipt, not the entire request history.
            val delayed = ports.getValue(firstSide).submit(firstRequest)
            if (firstSide == secondSide) {
                assertEquals(
                    com.unciv.pure.application.battle.session.BattleSessionRejection.OUT_OF_ORDER,
                    delayed.rejection
                )
                assertNull(delayed.result)
            } else {
                assertEquals(savedFirstReply, delayed)
            }
            assertEquals(2L, attacker.read().revision)
            assertEquals(2L, defender.read().revision)
            assertEquals(afterRetry.state, attacker.read().state)
            assertEquals(afterRetry.state, defender.read().state)
        } finally {
            server.close()
        }
    }
}
