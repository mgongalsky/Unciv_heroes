package com.unciv.battle.server

import com.badlogic.gdx.Gdx
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.BattleSessionRequest
import com.unciv.pure.domain.battle.BattleSide
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class BattleScenarioCatalogTest {
    private fun catalog(): BattleScenarioCatalog {
        val configured = System.getProperty("battle.assets")
        val directory = if (configured != null) File(configured) else
            generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
                .flatMap { sequenceOf(it, File(it, "android/assets")) }
                .first { File(it, "jsons/BattleScenarios.json").isFile }
        return BattleScenarioCatalog(directory)
    }

    @Test
    fun everyScenarioCreatesAPlayableBattleWithoutGraphics() {
        assertNull(Gdx.app)
        assertNull(Gdx.graphics)
        val catalog = catalog()
        assertEquals(6, catalog.scenarios.size)
        for (scenario in catalog.scenarios) {
            val session = catalog.create(scenario.id, 42)
            val snapshot = session.read(BattleSide.ATTACKER)
            assertFalse(snapshot.state.finished)
            assertEquals(8, snapshot.state.troops.size)
            assertTrue(snapshot.state.troops.all { it.position != null })
            val side = requireNotNull(snapshot.state.currentSide)
            val troop = requireNotNull(snapshot.state.currentTroopId)
            val reply = session.submit(side, BattleSessionRequest(1, 0, BattleCommand.Skip(troop)))
            assertTrue("Scenario ${scenario.id}", reply.result!!.success)
        }
        assertNull(Gdx.app)
        assertNull(Gdx.graphics)
    }

    @Test
    fun preservesSandboxDistributionAndEffectOverrides() {
        val catalog = catalog()
        val state = catalog.create("spearmen-vs-archers", 42).read(BattleSide.ATTACKER).state
        assertEquals(listOf(13, 13, 12, 12), state.troops.filter { it.side == BattleSide.ATTACKER }.map { it.currentAmount })
        assertEquals(listOf(10, 10, 10, 10), state.troops.filter { it.side == BattleSide.DEFENDER }.map { it.currentAmount })
        val luck = catalog.scenarios.single { it.id == "maximum-luck" }
        assertEquals(3, luck.attacker.luck)
        assertEquals(3, luck.defender.luck)
        assertEquals(1.0, luck.luckProbability, 0.0)
        assertEquals(0.0, luck.moraleProbability, 0.0)
        val morale = catalog.scenarios.single { it.id == "maximum-morale" }
        assertEquals(3, morale.attacker.morale)
        assertEquals(3, morale.defender.morale)
        assertEquals(1.0, morale.moraleProbability, 0.0)
        assertEquals(0.0, morale.luckProbability, 0.0)
    }

    @Test
    fun identicalSeedAndScenarioProduceIdenticalInitialStateAndCommandResult() {
        val catalog = catalog()
        val first = catalog.create("maximum-morale", 123)
        val second = catalog.create("maximum-morale", 123)
        val state = first.read(BattleSide.ATTACKER)
        assertEquals(state, second.read(BattleSide.ATTACKER))
        val request = BattleSessionRequest(1, 0, BattleCommand.Skip(requireNotNull(state.state.currentTroopId)))
        val side = requireNotNull(state.state.currentSide)
        assertEquals(first.submit(side, request), second.submit(side, request))
        assertEquals(first.read(side), second.read(side))
    }
}
