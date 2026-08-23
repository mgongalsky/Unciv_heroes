package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattlePolicy
import com.unciv.pure.application.battle.BattleTermination
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

class BattleSimulationRunnerTest {
    private lateinit var attackerArmy: ArmyInfo
    private lateinit var defenderArmy: ArmyInfo
    private lateinit var manager: TestableBattleManager
    private lateinit var attackerTile: FakeBattleTile
    private lateinit var defenderTile: FakeBattleTile

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

        attackerTile = FakeBattleTile(Vector2(0f, 0f))
        defenderTile = FakeBattleTile(Vector2(1f, 0f))
        attackerTile.addNeighbor(defenderTile)
        defenderTile.addNeighbor(attackerTile)
        val civ = FakeCivilizationInfo()
        attackerArmy = ArmyInfo(civ, 5).apply { addUnits("Spearman", 10) }
        defenderArmy = ArmyInfo(civ, 5).apply { addUnits("Spearman", 1) }
        manager = TestableBattleManager(
            attackerArmy,
            defenderArmy,
            FakeBattleField(listOf(attackerTile, defenderTile)),
            FakeBattleRandom(List(100) { 0.0 })
        )
        manager.placeTroop(attackerArmy.getAllTroops().filterNotNull().first(), attackerTile)
        manager.placeTroop(defenderArmy.getAllTroops().filterNotNull().first(), defenderTile)
        manager.initializeTurnQueue()
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun `headless battle returns victory and ordered transcript`() {
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        val attack = BattlePolicy {
            BattleCommand.Attack(it, Point(1, 0), Point(0, 0))
        }
        val noAction = BattlePolicy { null }

        val result = BattleSimulationRunner(manager, attack, noAction).run()

        assertEquals(BattleTermination.VICTORY, result.termination)
        assertEquals(true, result.winnerIsAttacker)
        assertEquals(3, result.turns)
        assertEquals(2, result.commands.size)
        assertTrue(result.commands.all { it.troopId == attacker.id })
        assertEquals(
            listOf("TroopAttacked", "TroopAttacked", "BattleEnded"),
            result.events.map { it.javaClass.simpleName }
        )
        val attacks = result.events.filterIsInstance<BattleEvent.TroopAttacked>()
        assertFalse(attacks.first().defenderDied)
        assertTrue(attacks.last().defenderDied)
    }

    @Test
    fun `no commands terminate as stalemate`() {
        val noAction = BattlePolicy { null }

        val result = BattleSimulationRunner(
            manager,
            noAction,
            noAction,
            maxTurns = 10,
            maxTurnsWithoutProgress = 2
        ).run()

        assertEquals(BattleTermination.STALEMATE, result.termination)
        assertNull(result.winnerIsAttacker)
        assertEquals(2, result.turns)
        assertTrue(result.commands.isEmpty())
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `max turns protects simulation from endless skipping`() {
        val skip = BattlePolicy { BattleCommand.Skip(it) }

        val result = BattleSimulationRunner(
            manager,
            skip,
            skip,
            maxTurns = 3,
            maxTurnsWithoutProgress = 10,
            seed = 42L
        ).run()

        assertEquals(BattleTermination.MAX_TURNS, result.termination)
        assertEquals(3, result.turns)
        assertEquals(42L, result.seed)
        assertFalse(result.events.isEmpty())
    }
}
