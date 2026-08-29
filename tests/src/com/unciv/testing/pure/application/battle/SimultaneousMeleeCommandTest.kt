package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.execute
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SimultaneousMeleeCommandTest {
    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(luckProbability = 0.0, moraleProbability = 0.0, armySize = 5)
        )
        val ruleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                damage = 10
                health = 100
                speed = 5
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
    fun `lethally hit melee defender still retaliates from its pre-attack snapshot`() {
        var randomCalls = 0
        val random = object : IBattleRandom {
            override fun nextDouble(): Double {
                randomCalls++
                return 1.0
            }
        }
        val scenario = scenario(attackerAmount = 10, defenderAmount = 1, random = random)
        val attackerHealthBefore = scenario.attacker.currentHealth

        val result = scenario.manager.execute(
            BattleCommand.Attack(scenario.attacker.id, Point(1, 0), Point(0, 0))
        )

        assertTrue(result.success)
        assertEquals(3, randomCalls)
        assertEquals(
            attackerHealthBefore - scenario.defender.damage,
            scenario.attacker.currentHealth
        )
        assertEquals(0, scenario.defender.currentAmount)
    }

    @Test
    fun `simultaneous death of both last troops ends in mutual defeat`() {
        val scenario = scenario(1, 1, FakeBattleRandom(List(10) { 1.0 }))
        scenario.attacker.currentHealth = 10
        scenario.defender.currentHealth = 10
        val events = mutableListOf<BattleEvent>()

        val result = scenario.manager.execute(
            BattleCommand.Attack(scenario.attacker.id, Point(1, 0), Point(0, 0))
        ) { events.add(it) }

        assertTrue(result.success)
        assertTrue(result.battleEnded)
        assertFalse(scenario.attackerArmy.contains(scenario.attacker))
        assertFalse(scenario.defenderArmy.contains(scenario.defender))
        assertNull(scenario.manager.getBattleResult()!!.winningArmy)
        assertNull((events.last() as BattleEvent.BattleEnded).winnerIsAttacker)
    }

    private fun scenario(
        attackerAmount: Int,
        defenderAmount: Int,
        random: IBattleRandom
    ): Scenario {
        val attackerArmy = ArmyInfo(FakeCivilizationInfo(), 5).apply {
            addUnits("Spearman", attackerAmount)
        }
        val defenderArmy = ArmyInfo(FakeCivilizationInfo(), 5).apply {
            addUnits("Spearman", defenderAmount)
        }
        val start = FakeBattleTile(Vector2(0f, 0f))
        val target = FakeBattleTile(Vector2(1f, 0f))
        start.addNeighbor(target)
        target.addNeighbor(start)
        val manager = TestableBattleManager(
            attackerArmy,
            defenderArmy,
            FakeBattleField(listOf(start, target)),
            random
        )
        val attacker = attackerArmy.getAllTroops().filterNotNull().first().apply {
            formation.current = 0
        }
        val defender = defenderArmy.getAllTroops().filterNotNull().first().apply {
            formation.current = 0
        }
        manager.initializeTurnQueue()
        manager.placeTroop(attacker, start)
        manager.placeTroop(defender, target)
        return Scenario(manager, attackerArmy, defenderArmy, attacker, defender)
    }

    private data class Scenario(
        val manager: TestableBattleManager,
        val attackerArmy: ArmyInfo,
        val defenderArmy: ArmyInfo,
        val attacker: com.unciv.pure.domain.troop.Troop,
        val defender: com.unciv.pure.domain.troop.Troop
    )
}
