package com.unciv.testing.pure.application.army

import com.unciv.logic.army.ArmyInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.testModule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.After
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class FillArmyCharTest {

    @Before
    fun setUp() {
        startKoin {
            allowOverride(true)
            modules(
                module {
                    single {
                        Ruleset().apply {
                            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
                            units["Spearman"] = BaseUnit().apply {
                                name = "Spearman"
                                unitType = "Melee"
                                health = 100
                            }
                        }
                    }
                },
                testModule
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun `char - fillArmy even distribution`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 4)
        army.fillArmy("Spearman", 40)
        val troops = army.getAllTroops()
        assertEquals(4, troops.size)
        troops.forEach { assertEquals(10, it?.currentAmount) }
        troops.forEach { assertEquals("Spearman", it?.unitName) }
    }

    @Test
    fun `char - fillArmy uneven distribution`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 4)
        army.fillArmy("Spearman", 10)
        val troops = army.getAllTroops()
        assertEquals(3, troops[0]?.currentAmount)
        assertEquals(3, troops[1]?.currentAmount)
        assertEquals(2, troops[2]?.currentAmount)
        assertEquals(2, troops[3]?.currentAmount)
    }

    @Test
    fun `char - fillArmy totalCount less than slots`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 4)
        army.fillArmy("Spearman", 3)
        val troops = army.getAllTroops()
        assertEquals(1, troops[0]?.currentAmount)
        assertEquals(1, troops[1]?.currentAmount)
        assertEquals(1, troops[2]?.currentAmount)
        assertNull(troops[3])
    }

    @Test
    fun `char - fillArmy invalid totalCount throws`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 4)
        try {
            army.fillArmy("Spearman", 0)
            fail("expected exception")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid unit name or total count", e.message)
        }
    }

    @Test
    fun `char - fillArmy blank unitName throws`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 4)
        try {
            army.fillArmy("", 10)
            fail("expected exception")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid unit name or total count", e.message)
        }
    }}
