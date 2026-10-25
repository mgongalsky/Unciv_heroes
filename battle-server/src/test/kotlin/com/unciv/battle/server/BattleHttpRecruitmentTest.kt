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
    @Test fun purchasesRulesetArmiesAndRecoversLostReadyResponse() = runBlocking {
        val catalog = BattleScenarioCatalog(File(requireNotNull(System.getProperty("battle.assets"))))
        assertEquals(mapOf("Peasant" to 1, "Archer" to 4, "Spearman" to 8,
            "Swordsman" to 11, "Horseman" to 16), catalog.recruitment.units.associate { it.unitName to it.cost })
        var created = 0
        val rooms = BattleRooms({ catalog.create(it, 42L) }, recruitment = catalog.recruitment,
            createPurchasedSession = { a, d -> catalog.createPurchased(a, d).also { created++ } })
        val server = BattleHttpServer(InetSocketAddress("127.0.0.1", 0), rooms,
            listOf(WireScenario(BattleRooms.RECRUITMENT_SCENARIO, "Build army", "500 coins")))
        try {
            server.start()
            val url = "http://127.0.0.1:${server.port}"
            val a = RemoteBattleLobby(UrlBattleHttpTransport(url)).create(BattleRooms.RECRUITMENT_SCENARIO)
            val delegate = UrlBattleHttpTransport(url)
            var loseReady = false
            val transport = object : BattleHttpTransport {
                override suspend fun request(method: String, path: String, body: String?, token: String?): String {
                    val result = delegate.request(method, path, body, token)
                    if (loseReady && method == "POST" && path.endsWith("/ready")) {
                        loseReady = false
                        throw IOException("Simulated lost Ready response")
                    }
                    return result
                }
            }
            val d = RemoteBattleLobby(transport).join(a.roomCode, "r".repeat(32))
            val waiting = a.room()
            assertEquals(RemoteRoomPhase.WAITING, waiting.phase)
            assertEquals(500, requireNotNull(waiting.recruitment).budget)
            assertEquals(5, requireNotNull(waiting.recruitment).maxStacks)
            assertTrue(waiting.battle.state.troops.isEmpty())
            assertEquals(0, created)
            try {
                a.ready(listOf(BattleArmyStack("Peasant", 501)))
                fail("Over-budget army accepted")
            } catch (failure: BattleHttpException) { assertEquals(400, failure.status) }
            assertFalse(a.room().attackerReady)
            val armyA = listOf(BattleArmyStack("Swordsman", 10), BattleArmyStack("Spearman", 10),
                BattleArmyStack("Archer", 10), BattleArmyStack("Peasant", 110), BattleArmyStack("Horseman", 10))
            val armyD = listOf(BattleArmyStack("Horseman", 20), BattleArmyStack("Archer", 20),
                BattleArmyStack("Peasant", 20), BattleArmyStack("Peasant", 40), BattleArmyStack("Peasant", 40))
            a.ready(armyA)
            assertEquals(RemoteRoomPhase.WAITING, a.room().phase)
            assertTrue(d.room().ownArmy.isEmpty())
            assertEquals(0, created)
            loseReady = true
            try { d.ready(armyD); fail("Expected lost response") }
            catch (failure: IOException) { assertEquals("Simulated lost Ready response", failure.message) }
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
            a.ready(armyA)
            assertEquals(1, created)
            try { a.ready(armyD); fail("Changed confirmed army accepted") }
            catch (failure: BattleHttpException) { assertEquals(400, failure.status) }
            assertEquals(started.battle.state, d.read().state)
        } finally { server.close() }
    }
}
