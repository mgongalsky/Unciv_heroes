package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.logic.army.ArmyInfo
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class AIBattleRetaliationPolicyTest {
    private lateinit var manager: TestableBattleManager
    private lateinit var attacker: Troop
    private lateinit var armedTarget: Troop
    private lateinit var exhaustedTarget: Troop

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(luckProbability = 0.0, moraleProbability = 0.0, armySize = 5)
        )
        val ruleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            unitTypes["Ranged"] = UnitType().apply { name = "Ranged" }
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"; unitType = "Melee"; speed = 5; health = 40; damage = 10
            }
            units["Archer"] = BaseUnit().apply {
                name = "Archer"; unitType = "Ranged"; speed = 3; health = 15; damage = 5
                rangedStrength = 7
            }
        }
        startKoin { allowOverride(true); modules(module { single { ruleset } }, testModule) }

        val civ = FakeCivilizationInfo()
        val attackerArmy = ArmyInfo(civ, 5).apply { addUnits("Spearman", 10) }
        val defenderArmy = ArmyInfo(civ, 5).apply {
            addUnits("Spearman", 5)
            addUnits("Archer", 5)
        }
        val attackerTile = FakeBattleTile(Vector2(0f, 0f))
        val armedTile = FakeBattleTile(Vector2(1f, 0f))
        val exhaustedTile = FakeBattleTile(Vector2(0f, 1f))
        attackerTile.addNeighbor(armedTile)
        attackerTile.addNeighbor(exhaustedTile)
        armedTile.addNeighbor(attackerTile)
        exhaustedTile.addNeighbor(attackerTile)

        manager = TestableBattleManager(
            attackerArmy,
            defenderArmy,
            FakeBattleField(listOf(attackerTile, armedTile, exhaustedTile)),
            FakeBattleRandom(List(100) { 0.0 })
        )
        attacker = attackerArmy.getAllTroops().filterNotNull().first()
        armedTarget = defenderArmy.getAllTroops().filterNotNull().first { !it.isRanged }
        exhaustedTarget = defenderArmy.getAllTroops().filterNotNull().first { it.isRanged }
        manager.placeTroop(attacker, attackerTile)
        manager.placeTroop(armedTarget, armedTile)
        manager.placeTroop(exhaustedTarget, exhaustedTile)
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun `melee prefers equal target with exhausted retaliation`() {
        val command = policy().chooseCommand(attacker.id)

        assertEquals(BattleCommand.Attack(attacker.id, Point(0, 1), Point(0, 0)), command)
        assertEquals(5, armedTarget.currentAmount)
        assertEquals(5, exhaustedTarget.currentAmount)
    }

    @Test
    fun `broken formation remains more important than exhausted retaliation`() {
        armedTarget.formation.current = 0

        val command = policy().chooseCommand(attacker.id)

        assertEquals(BattleCommand.Attack(attacker.id, Point(1, 0), Point(0, 0)), command)
    }

    private fun policy() = AIBattlePolicy(manager) { target -> target !== exhaustedTarget }
}
