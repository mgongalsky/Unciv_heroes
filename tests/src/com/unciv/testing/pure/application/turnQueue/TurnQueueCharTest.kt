package com.unciv.testing.pure.application.turnQueue

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class TurnQueueCharTest {

    private lateinit var civInfo: FakeCivilizationInfo

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(
                luckProbability = 0.0,
                moraleProbability = 0.0,
                armySize = 5
            )
        )

        val fakeRuleset = Ruleset().apply {
            val unitTypeObj = UnitType().apply { name = "Melee" }
            unitTypes["Melee"] = unitTypeObj
            units["Fast"] = BaseUnit().apply {
                name = "Fast"; unitType = "Melee"
                damage = 10; health = 100; speed = 10
            }
            units["Slow"] = BaseUnit().apply {
                name = "Slow"; unitType = "Melee"
                damage = 10; health = 100; speed = 3
            }
            units["Medium"] = BaseUnit().apply {
                name = "Medium"; unitType = "Melee"
                damage = 10; health = 100; speed = 5
            }
        }

        startKoin {
            allowOverride(true)
            modules(module { single { fakeRuleset } }, testModule)
        }

        civInfo = FakeCivilizationInfo()
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    private fun makeManager(
        attackerArmy: ArmyInfo,
        defenderArmy: ArmyInfo
    ): BattleManager = BattleManager(
        attackerArmy, defenderArmy,
        FakeBattleField(),
        FakeBattleRandom(List(100) { 0.0 }),
        moraleProbability = 0.0,
        luckProbability = 0.0
    )

    @Test
    fun `faster troop goes before slower troop`() {
        val attacker = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Fast", 10) }
        val defender = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Slow", 10) }
        val manager = makeManager(attacker, defender)
        manager.initializeTurnQueue()

        assertEquals("Fast", manager.getCurrentTroop()?.unitName)
    }

    @Test
    fun `equal speed — attacker goes before defender`() {
        val attacker = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Medium", 10) }
        val defender = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Medium", 8) }
        val manager = makeManager(attacker, defender)
        manager.initializeTurnQueue()

        assertTrue(attacker.contains(manager.getTurnQueue()[0]))
    }

    @Test
    fun `queue size equals total number of troops`() {
        val attacker = ArmyInfo(civInfo, maxSlots = 5).apply {
            addUnits("Fast", 5)
            addUnits("Slow", 3)
        }
        val defender = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Medium", 8) }
        val manager = makeManager(attacker, defender)
        manager.initializeTurnQueue()

        assertEquals(3, manager.getTurnQueue().size)
    }

    @Test
    fun `advanceTurn moves to next troop`() {
        val attacker = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Fast", 10) }
        val defender = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Slow", 8) }
        val manager = makeManager(attacker, defender)
        manager.initializeTurnQueue()

        val first = manager.getCurrentTroop()
        manager.advanceTurn()
        val second = manager.getCurrentTroop()

        assertNotSame(first, second)
    }

    @Test
    fun `advanceTurn wraps around after last troop`() {
        val attacker = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Fast", 10) }
        val defender = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Slow", 8) }
        val manager = makeManager(attacker, defender)
        manager.initializeTurnQueue()

        val first = manager.getCurrentTroop()
        manager.advanceTurn()
        manager.advanceTurn()

        assertEquals(first, manager.getCurrentTroop())
    }

    @Test
    fun `remove troop after current — current stays the same`() {
        val attacker = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Fast", 10) }
        val defender = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Slow", 8) }
        val manager = makeManager(attacker, defender)
        manager.initializeTurnQueue()

        val current = manager.getCurrentTroop()
        val next = manager.getTurnQueue()[1]
        manager.removeTroop(next)

        assertEquals(current, manager.getCurrentTroop())
    }

    @Test
    fun `remove troop before current — current stays the same`() {
        val attacker = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Fast", 10) }
        val defender = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Slow", 8) }
        val manager = makeManager(attacker, defender)
        manager.initializeTurnQueue()

        manager.advanceTurn()
        val current = manager.getCurrentTroop()
        val before = manager.getTurnQueue()[0]
        manager.removeTroop(before)

        assertEquals(current, manager.getCurrentTroop())
    }

    @Test
    fun `remove last troop — queue is empty and getCurrentTroop returns null`() {
        val attacker = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Fast", 10) }
        val defender = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Slow", 8) }
        val manager = makeManager(attacker, defender)
        manager.initializeTurnQueue()

        val all = manager.getTurnQueue().toList()
        all.forEach { manager.removeTroop(it) }

        assertTrue(manager.getTurnQueue().isEmpty())
        assertNull(manager.getCurrentTroop())
    }
}
