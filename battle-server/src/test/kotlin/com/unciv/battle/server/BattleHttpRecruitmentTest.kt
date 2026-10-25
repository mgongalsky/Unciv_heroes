package com.unciv.battle.server

import com.unciv.battle.protocol.WireScenario
import com.unciv.infrastructure.battle.network.*
import com.unciv.pure.application.battle.recruitment.BattleArmyStack
import com.unciv.pure.domain.battle.BattleSide
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress

class BattleHttpRecruitmentTest {
    @Test
    fun purchasesRulesetArmiesAndRecoversLostReadyResponse() = runBlocking {
        val catalog =
                BattleScenarioCatalog(File(requireNotNull(System.getProperty("battle.assets"))))
        assertEquals(
            mapOf(
                "Peasant" to 1, "Archer" to 4, "Spearman" to 9,
                "Swordsman" to 12, "Horseman" to 15
            ), catalog.recruitment.units.associate { it.unitName to it.cost })
        var created = 0
        val rooms = BattleRooms(
            { catalog.create(it, 42L) }, recruitment = catalog.recruitment,
            createPurchasedSession = { a, d -> catalog.createPurchased(a, d).also { created++ } })
        val server = BattleHttpServer(
            InetSocketAddress("127.0.0.1", 0), rooms,
            listOf(WireScenario(BattleRooms.RECRUITMENT_SCENARIO, "Build army", "500 coins"))
        )
        try {
            server.start()
            val delegate = UrlBattleHttpTransport("http://127.0.0.1:${server.port}")
            var sendEightStacks = false
            var loseReady = false
            val transport = object : BattleHttpTransport {
                override suspend fun request(
                    method: String,
                    path: String,
                    body: String?,
                    token: String?
                ): String {
                    val ready = method == "POST" && path.endsWith("/ready")
                    val payload = if (ready && sendEightStacks) {
                        sendEightStacks = false
                        val stacks =
                                List(8) { "{\"unitName\":\"Peasant\",\"count\":1}" }.joinToString(",")
                        "{\"protocolVersion\":${com.unciv.battle.protocol.BattleProtocol.VERSION},\"battleNumber\":1,\"stacks\":[$stacks]}"
                    } else body
                    val result = delegate.request(method, path, payload, token)
                    if (ready && loseReady) {
                        loseReady = false
                        throw IOException("Simulated lost Ready response")
                    }
                    return result
                }
            }
            val a = RemoteBattleLobby(transport).create(BattleRooms.RECRUITMENT_SCENARIO)
            val d = RemoteBattleLobby(transport).join(a.roomCode, "r".repeat(32))
            val waiting = a.room()
            assertEquals(RemoteRoomPhase.WAITING, waiting.phase)
            assertEquals(500, requireNotNull(waiting.recruitment).budget)
            assertEquals(7, requireNotNull(waiting.recruitment).maxStacks)
            assertTrue(waiting.battle.state.troops.isEmpty())
            assertEquals(0, created)
            try {
                a.ready(listOf(BattleArmyStack("Peasant", 501)))
                fail("Over-budget army accepted")
            } catch (failure: BattleHttpException) {
                assertEquals(400, failure.status)
            }
            sendEightStacks = true
            try {
                a.ready(listOf(BattleArmyStack("Peasant", 1)))
                fail("Server accepted eight stacks")
            } catch (failure: BattleHttpException) {
                assertEquals(400, failure.status)
            }
            assertFalse(a.room().attackerReady)
            assertEquals(0, created)
            val armyA = listOf(
                BattleArmyStack("Swordsman", 10),
                BattleArmyStack("Spearman", 10),
                BattleArmyStack("Archer", 10),
                BattleArmyStack("Horseman", 10),
                BattleArmyStack("Peasant", 20),
                BattleArmyStack("Peasant", 40),
                BattleArmyStack("Peasant", 40)
            )
            val armyD = listOf(BattleArmyStack("Horseman", 20), BattleArmyStack("Archer", 20)) +
                    List(5) { BattleArmyStack("Peasant", 24) }
            assertEquals(500L, catalog.recruitment.cost(armyA))
            assertEquals(500L, catalog.recruitment.cost(armyD))
            a.ready(armyA)
            assertEquals(RemoteRoomPhase.WAITING, a.room().phase)
            assertTrue(d.room().ownArmy.isEmpty())
            assertEquals(0, created)
            loseReady = true
            try {
                d.ready(armyD); fail("Expected lost response")
            } catch (failure: IOException) {
                assertEquals("Simulated lost Ready response", failure.message)
            }
            val started = d.ready(armyD)
            assertEquals(RemoteRoomPhase.ACTIVE, started.phase)
            assertEquals(1, created)
            assertEquals(armyD, started.ownArmy)
            assertEquals(armyA, a.room().ownArmy)
            assertEquals(started.battle.state, a.read().state)
            assertNotNull(started.battle.state.field)
            fun actual(side: BattleSide) = started.battle.state.troops.filter { it.side == side }
                .sortedBy { it.slot }.map { BattleArmyStack(it.unitName, it.initialAmount) }
            assertEquals(armyA, actual(BattleSide.ATTACKER))
            assertEquals(armyD, actual(BattleSide.DEFENDER))
            val positions = started.battle.state.troops.map { it.position }
            assertEquals(14, positions.size)
            assertTrue(positions.all { it != null })
            assertEquals(14, positions.distinct().size)
            a.ready(armyA)
            assertEquals(1, created)
            try {
                a.ready(armyD); fail("Changed confirmed army accepted")
            } catch (failure: BattleHttpException) {
                assertEquals(400, failure.status)
            }
            assertEquals(started.battle.state, d.read().state)
        } finally {
            server.close()
        }
    }
}
