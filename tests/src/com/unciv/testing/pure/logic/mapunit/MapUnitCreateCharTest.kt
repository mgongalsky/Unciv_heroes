package com.unciv.testing.pure.logic.mapunit

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull

class FakeArmyInfo : ArmyInfo(FakeCivilizationInfo(), maxSlots = 1)

class MapUnitCreateCharTest {

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())
        val fakeRuleset = Ruleset().apply {
            val warrior = BaseUnit().apply { name = "Warrior" }
            units["Warrior"] = warrior
        }
        startKoin {
            allowOverride(true)
            modules(
                module { single { fakeRuleset } },
                testModule
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testCreate() {
        val unit = TestableMapUnit()
    }

    @Test
    fun testCreateWithAmountAndName() {
        val unit = object : MapUnit(1, "Warrior") {
            override fun createArmy() = FakeArmyInfo()
        }
        assert(unit.name == "Warrior")
        assert(unit.amount == 1)
    }

    @Test
    fun characterize_emptyConstructor() {
        val unit = TestableMapUnit()

        assertEquals(100, unit.health)
        assertNull(unit.action)
        assertEquals(0, unit.attacksThisTurn)
        assertEquals(3.0f, unit.hero.currentFood, 0.01f)
        assertEquals(15.0f, unit.basicFoodCapacity, 0.01f)
        assertEquals(3, unit.morale)
        assertEquals(3, unit.luck)
        assertFalse(unit.isDestroyed)
        assertFalse(unit.isTransported)
        assertEquals(0, unit.amount)
    }

    @Test
    fun characterize_createMonster() {
        val monster = object : MapUnit(1, "Warrior") {
            override fun createArmy() = FakeArmyInfo()
        }

        assertEquals("Warrior", monster.name)
        assertEquals(1, monster.amount)
        assertEquals(100, monster.health)
        assertEquals("Warrior", monster.baseUnit().name)
        assertEquals("[Warrior]", monster.displayName())
        assertFalse(monster.isFortified())
        assertFalse(monster.isSleeping())
        assertFalse(monster.isMoving())
        assertEquals(0, monster.calculateArmyPopulation())
    }
}
