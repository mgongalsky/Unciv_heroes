// testing/pure/application/battle/BattleManagerCharTest.kt
package com.unciv.testing.pure.application.battle

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class BattleManagerCharTest {

    private lateinit var attackerArmy: ArmyInfo
    private lateinit var defenderArmy: ArmyInfo
    private lateinit var manager: BattleManager

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
            val spearman = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                damage = 10
                health = 100
                speed = 5
            }
            units["Spearman"] = spearman
        }

        startKoin {
            allowOverride(true)
            modules(
                module { single { fakeRuleset } },
                testModule
            )
        }

        val civInfo = FakeCivilizationInfo()
        attackerArmy = ArmyInfo(civInfo, maxSlots = 5).apply {
            addUnits("Spearman", 10)
        }
        defenderArmy = ArmyInfo(civInfo, maxSlots = 5).apply {
            addUnits("Spearman", 8)
        }
        manager = BattleManager(
            attackerArmy,
            defenderArmy,
            FakeBattleField(),
            FakeBattleRandom(List(100) { 0.0 })
        )
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun `isBattleOn initial state`() {
        assertTrue(manager.isBattleOn())
    }

    @Test
    fun `initializeTurnQueue`() {
        manager.initializeTurnQueue()
        assertEquals(2, manager.getTurnQueue().size)
        assertNotNull(manager.getCurrentTroop())
        assertEquals("Spearman", manager.getCurrentTroop()?.unitName)
    }

    @Test
    fun `advanceTurn moves to next troop`() {
        manager.initializeTurnQueue()
        val first = manager.getCurrentTroop()
        manager.advanceTurn()
        val second = manager.getCurrentTroop()
        assertNotNull(first)
        assertNotNull(second)
        assertNotSame(first, second)
    }

    @Test
    fun `advanceTurn does not restore formation`() {
        manager.initializeTurnQueue()
        val first = manager.getCurrentTroop()!!
        val next = manager.getTurnQueue().first { it !== first }
        next.formation.current = 20

        manager.advanceTurn()

        assertEquals(next, manager.getCurrentTroop())
        assertEquals(20, next.formation.current)
        assertEquals(first.formation.maximum, first.formation.current)
    }

    @Test
    fun `getBattleResult initial is null`() {
        assertNull(manager.getBattleResult())
    }

    @Test
    fun `getBattleResult after all attacker troops removed`() {
        manager.initializeTurnQueue()
        attackerArmy.getAllTroops().filterNotNull().forEach {
            manager.removeTroop(it)
        }
        assertNotNull(manager.getBattleResult())
        assertFalse(manager.isBattleOn())
    }

    @Test
    fun `attack damages defender soldiers after formation absorbs its share`() {
        manager.initializeTurnQueue()
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        val beforeAmount = defender.currentAmount
        val beforeHealth = defender.currentHealth

        manager.attack(defender, attacker)

        assertTrue(
            defender.currentAmount < beforeAmount || defender.currentHealth < beforeHealth
        )
    }
}
