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
import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.domain.battle.Point
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class BattleManagerCommandFacadeTest {
    private lateinit var manager: TestableBattleManager
    private lateinit var attackerArmy: ArmyInfo
    private lateinit var defenderArmy: ArmyInfo
    private lateinit var start: FakeBattleTile
    private lateinit var target: FakeBattleTile

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(
                luckProbability = 0.0,
                moraleProbability = 0.0,
                armySize = 5
            )
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
        attackerArmy = ArmyInfo(FakeCivilizationInfo(), 5).apply { addUnits("Spearman", 10) }
        defenderArmy = ArmyInfo(FakeCivilizationInfo(), 5).apply { addUnits("Spearman", 8) }
        start = FakeBattleTile(Vector2(0f, 0f))
        target = FakeBattleTile(Vector2(1f, 0f))
        start.addNeighbor(target)
        target.addNeighbor(start)
        manager = TestableBattleManager(
            attackerArmy,
            defenderArmy,
            FakeBattleField(listOf(start, target)),
            FakeBattleRandom(List(100) { 0.0 })
        )
        manager.initializeTurnQueue()
        manager.placeTroop(attackerArmy.getAllTroops().filterNotNull().first(), start)
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun `MOVE command resolves coordinates and returns value-only result`() {
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        val result = manager.execute(BattleCommand.Move(attacker.id, Point(1, 0)))
        assertTrue(result.success)
        assertEquals(Point(0, 0), result.movedFrom)
        assertEquals(Point(1, 0), result.movedTo)
        assertNull(result.rejection)
    }

    @Test
    fun `unknown troop is rejected before field mutation`() {
        val result = manager.execute(BattleCommand.Skip(Int.MAX_VALUE))
        assertFalse(result.success)
        assertEquals(BattleRejection.INVALID_TARGET, result.rejection)
        assertNull(result.movedFrom)
        assertNull(result.movedTo)
    }

    @Test
    fun skipCommandDelegatesAndReturnsValueOnlySuccess() {
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        val result = manager.execute(BattleCommand.Skip(attacker.id))
        assertTrue(result.success)
        assertNull(result.rejection)
        assertNull(result.movedFrom)
        assertNull(result.movedTo)
    }

    @Test
    fun attackCommandResolvesTargetAndAttackPosition() {
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()
        manager.placeTroop(defender, target)

        val result = manager.execute(
            BattleCommand.Attack(attacker.id, Point(1, 0), Point(0, 0))
        )

        assertTrue(result.success)
        assertEquals(Point(0, 0), result.movedFrom)
        assertEquals(Point(0, 0), result.movedTo)
        assertNull(result.rejection)
    }

    @Test
    fun shootCommandMapsLegacyRejection() {
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()
        manager.placeTroop(defender, target)

        val result = manager.execute(BattleCommand.Shoot(attacker.id, Point(1, 0)))

        assertFalse(result.success)
        assertEquals(BattleRejection.NOT_IMPLEMENTED, result.rejection)
    }
}
