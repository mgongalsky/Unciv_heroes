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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class FormationCommandIntegrationTest {
    private lateinit var manager: TestableBattleManager
    private lateinit var troop: com.unciv.pure.domain.troop.Troop
    private lateinit var tiles: List<FakeBattleTile>

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(luckProbability = 0.0, moraleProbability = 0.0, armySize = 5)
        )
        val ruleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            units["Runner"] = BaseUnit().apply {
                name = "Runner"; unitType = "Melee"; speed = 5; health = 100; damage = 10
                formationHealthPercent = 10
                formationDamageReductionPercent = 50
            }
        }
        startKoin { allowOverride(true); modules(module { single { ruleset } }, testModule) }
        tiles = (0..3).map { FakeBattleTile(Vector2(it.toFloat(), 0f)) }
        tiles.zipWithNext().forEach { (left, right) ->
            left.addNeighbor(right)
            right.addNeighbor(left)
        }
        val attacker = ArmyInfo(FakeCivilizationInfo(), 5).apply { addUnits("Runner", 10) }
        val defender = ArmyInfo(FakeCivilizationInfo(), 5).apply { addUnits("Runner", 10) }
        manager = TestableBattleManager(
            attacker, defender, FakeBattleField(tiles),
            FakeBattleRandom(List(100) { 0.0 }), useRealMovement = true
        )
        troop = attacker.getAllTroops().filterNotNull().first()
        manager.placeTroop(troop, tiles.first())
        manager.initializeTurnQueue()
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun `moving three cells with speed five reduces formation before event`() {
        var formationWhenEventArrived = -1

        val result = manager.execute(BattleCommand.Move(troop.id, Point(3, 0))) {
            formationWhenEventArrived = troop.formation.current
        }

        assertTrue(result.success)
        assertEquals(75, troop.formation.current)
        assertEquals(75, formationWhenEventArrived)
    }

    @Test
    fun `moving one cell rebuilds depleted formation before event`() {
        troop.formation.current = 0
        var formationWhenEventArrived = -1

        val result = manager.execute(BattleCommand.Move(troop.id, Point(1, 0))) {
            formationWhenEventArrived = troop.formation.current
        }

        assertTrue(result.success)
        assertEquals(15, troop.formation.current)
        assertEquals(15, formationWhenEventArrived)
    }

    @Test
    fun `skip rebuilds depleted formation before event`() {
        troop.formation.current = 0
        var formationWhenEventArrived = -1

        val result = manager.execute(BattleCommand.Skip(troop.id)) {
            formationWhenEventArrived = troop.formation.current
        }

        assertTrue(result.success)
        assertEquals(25, troop.formation.current)
        assertEquals(25, formationWhenEventArrived)
    }

    @Test
    fun `rejected long move does not damage formation`() {
        val result = manager.execute(BattleCommand.Move(troop.id, Point(9, 0)))

        assertFalse(result.success)
        assertEquals(100, troop.formation.current)
    }
}
