package com.unciv.testing.pure.domain.army

import com.unciv.logic.army.ArmyInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ArmyInfoCharTest {

    @Before
    fun setUp() {
        TroopFactory.resetIdCounter()
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

    private fun makeTroop(unitName: String = "Spearman", amount: Int = 10) =
            TroopFactory.create(
                unitName = unitName,
                amount = amount,
                source = HardcodedTroopDefinitionSource(
                    speed = 5, damage = 10, maxHealth = 100, rangedStrength = 0
                )
            )

    // ===== getAllTroops =====

    @Test
    fun `char - getAllTroops on empty army`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7)
        val troops = army.getAllTroops()
        assertEquals(7, troops.size)
        assertTrue(troops.all { it == null })
    }

    @Test
    fun `char - getAllTroops after addTroop`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7)
        army.addTroop(makeTroop("Spearman", 10))
        army.addTroop(makeTroop("Spearman", 20))
        val troops = army.getAllTroops()
        assertEquals(7, troops.size)
        assertEquals("Spearman", troops[0]?.unitName)
        assertEquals(10, troops[0]?.currentAmount)
        assertEquals("Spearman", troops[1]?.unitName)
        assertEquals(20, troops[1]?.currentAmount)
        assertTrue(troops.drop(2).all { it == null })
    }

    @Test
    fun `char - addTroop to empty army`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 3)
        val result = army.addTroop(makeTroop())
        assertTrue(result)
        assertEquals("Spearman", army.getAllTroops()[0]?.unitName)
        assertEquals(10, army.getAllTroops()[0]?.currentAmount)
        assertTrue(army.getAllTroops().drop(1).all { it == null })
    }

    @Test
    fun `char - addTroop until full`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 2)
        val r1 = army.addTroop(makeTroop())
        val r2 = army.addTroop(makeTroop())
        val r3 = army.addTroop(makeTroop())
        assertTrue(r1)
        assertTrue(r2)
        assertFalse(r3)
        assertTrue(army.getAllTroops().all { it != null })
    }

    @Test
    fun `char - removeTroop existing`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 3)
        val troop = makeTroop()
        army.addTroop(troop)
        val result = army.removeTroop(troop)
        assertTrue(result)
        assertTrue(army.getAllTroops().all { it == null })
    }

    @Test
    fun `char - removeTroop foreign troop`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 3)
        army.addTroop(makeTroop())
        val foreign = makeTroop()
        val result = army.removeTroop(foreign)
        assertFalse(result)
        assertNotNull(army.getAllTroops()[0])
    }

    @Test
    fun `char - removeTroop middle slot`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 3)
        val t1 = makeTroop(); val t2 = makeTroop(); val t3 = makeTroop()
        army.addTroop(t1); army.addTroop(t2); army.addTroop(t3)
        val result = army.removeTroop(t2)
        assertTrue(result)
        assertEquals(1, army.getAllTroops()[0]?.id)
        assertNull(army.getAllTroops()[1])
        assertEquals(3, army.getAllTroops()[2]?.id)
    }
}
