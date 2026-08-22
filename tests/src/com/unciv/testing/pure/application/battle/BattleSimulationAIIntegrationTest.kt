package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.battle.BattleTermination
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class BattleSimulationAIIntegrationTest {
    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(luckProbability = 0.0, moraleProbability = 0.0, armySize = 5)
        )
        val ruleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"; unitType = "Melee"; speed = 5; health = 100; damage = 10
            }
        }
        startKoin { allowOverride(true); modules(module { single { ruleset } }, testModule) }
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun `AI versus AI completes headlessly`() {
        val attackerTile = FakeBattleTile(Vector2(0f, 0f))
        val defenderTile = FakeBattleTile(Vector2(1f, 0f))
        attackerTile.addNeighbor(defenderTile)
        defenderTile.addNeighbor(attackerTile)
        val civ = FakeCivilizationInfo()
        val attackerArmy = ArmyInfo(civ, 5).apply { addUnits("Spearman", 4) }
        val defenderArmy = ArmyInfo(civ, 5).apply { addUnits("Spearman", 4) }
        val manager = TestableBattleManager(
            attackerArmy,
            defenderArmy,
            FakeBattleField(listOf(attackerTile, defenderTile)),
            FakeBattleRandom(List(200) { 0.5 })
        )
        manager.placeTroop(attackerArmy.getAllTroops().filterNotNull().first(), attackerTile)
        manager.placeTroop(defenderArmy.getAllTroops().filterNotNull().first(), defenderTile)
        manager.initializeTurnQueue()
        val policy = AIBattlePolicy(manager)

        val result = BattleSimulationRunner(
            manager,
            attackerPolicy = policy,
            defenderPolicy = policy,
            maxTurns = 100,
            maxTurnsWithoutProgress = 10,
            seed = 123L
        ).run()

        assertEquals(BattleTermination.VICTORY, result.termination)
        assertNotNull(result.winnerIsAttacker)
        assertTrue(result.turns in 1..100)
        assertTrue(result.events.any { it is com.unciv.pure.application.battle.BattleEvent.BattleEnded })
        assertEquals(123L, result.seed)
    }
}
