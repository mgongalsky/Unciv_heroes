package com.unciv.testing.pure.application.army

import com.unciv.pure.application.army.CalculateArmyFoodMaintenanceUseCase
import com.unciv.logic.army.ArmyInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class CalculateArmyFoodMaintenanceUseCaseCharTest {

    @Before
    fun setUp() {
        val fakeRuleset = Ruleset().apply {
            val unitTypeObj = UnitType().apply { name = "Melee" }
            unitTypes["Melee"] = unitTypeObj
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                health = 100
            }
            units["Archer"] = BaseUnit().apply {
                name = "Archer"
                unitType = "Melee"
                health = 100
            }
        }
        startKoin {
            allowOverride(true)
            modules(module { single { fakeRuleset } })
        }
    }

    @After
    fun tearDown() = stopKoin()

    private fun selfFeedingTroop(unitName: String, amount: Int): Troop =
            TroopFactory.create(
                unitName = unitName,
                amount = amount,
                source = HardcodedTroopDefinitionSource(
                    speed = 5,
                    damage = 10,
                    maxHealth = 100,
                    rangedStrength = 5,
                    isSelfFeeding = true
                )
            )

    @Test
    fun `print food maintenance - single troop not in city`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Spearman", 30)
        }
        val result = CalculateArmyFoodMaintenanceUseCase.execute(army, isInCity = false)
        assertEquals(1.0f, result)
    }

    @Test
    fun `print food maintenance - multiple troops not in city`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Spearman", 30)
            addUnits("Spearman", 60)
            addUnits("Spearman", 90)
        }
        val result = CalculateArmyFoodMaintenanceUseCase.execute(army, isInCity = false)
        assertEquals(6.0f, result)
    }

    @Test
    fun `print food maintenance - self feeding troop not in city`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addTroop(selfFeedingTroop("Archer", 30))
        }
        val result = CalculateArmyFoodMaintenanceUseCase.execute(army, isInCity = false)
        assertEquals(1.0f, result)
    }

    @Test
    fun `print food maintenance - mixed troops not in city`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Spearman", 30)
            addUnits("Spearman", 60)
            addTroop(selfFeedingTroop("Archer", 30))
        }
        val result = CalculateArmyFoodMaintenanceUseCase.execute(army, isInCity = false)
        assertEquals(4.0f, result)
    }

    @Test
    fun `print food maintenance - single troop in city`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Spearman", 30)
        }
        val result = CalculateArmyFoodMaintenanceUseCase.execute(army, isInCity = true)
        assertEquals(1.0f, result)
    }

    @Test
    fun `print food maintenance - multiple troops in city`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Spearman", 30)
            addUnits("Spearman", 60)
            addUnits("Spearman", 90)
        }
        val result = CalculateArmyFoodMaintenanceUseCase.execute(army, isInCity = true)
        assertEquals(6.0f, result)
    }

    @Test
    fun `print food maintenance - self feeding troop in city`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addTroop(selfFeedingTroop("Archer", 30))
        }
        val result = CalculateArmyFoodMaintenanceUseCase.execute(army, isInCity = true)
        assertEquals(0.0f, result)
    }

    @Test
    fun `print food maintenance - mixed troops in city`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Spearman", 30)
            addUnits("Spearman", 60)
            addTroop(selfFeedingTroop("Archer", 30))
        }
        val result = CalculateArmyFoodMaintenanceUseCase.execute(army, isInCity = true)
        assertEquals(3.0f, result)
    }

    @Test
    fun `print food maintenance - empty army`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7)
        val result = CalculateArmyFoodMaintenanceUseCase.execute(army, isInCity = false)
        assertEquals(0.0f, result)
    }

    @Test
    fun `print food maintenance - all self feeding in city`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addTroop(selfFeedingTroop("Archer", 30))
            addTroop(selfFeedingTroop("Archer", 60))
            addTroop(selfFeedingTroop("Archer", 90))
        }
        val result = CalculateArmyFoodMaintenanceUseCase.execute(army, isInCity = true)
        assertEquals(0.0f, result)
    }
}
