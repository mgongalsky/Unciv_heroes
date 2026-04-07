package com.unciv.testing.pure.application.army

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ArmyInfoContainsTest {

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
    fun tearDown() = stopKoin()

    private fun makeTroop(unitName: String, amount: Int) =
            TroopFactory.create(
                unitName = unitName,
                amount = amount,
                source = HardcodedTroopDefinitionSource(
                    speed = 5,
                    damage = 10,
                    maxHealth = 100,
                    rangedStrength = 0,
                    isSelfFeeding = false
                )
            )

    @Test
    fun `contains returns true when troop is in army`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Spearman", 10)
        }
        val troop = army.getAllTroops().filterNotNull().first()
        assertTrue(army.contains(troop))
    }

    @Test
    fun `contains returns false when troop is not in army`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Spearman", 10)
        }
        val foreignTroop = makeTroop("Spearman", 10)
        assertFalse(army.contains(foreignTroop))
    }

    @Test
    fun `contains returns false for empty army`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7)
        val troop = makeTroop("Spearman", 10)
        assertFalse(army.contains(troop))
    }

    @Test
    fun `contains returns true for correct troop among multiple`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Spearman", 10)
            addUnits("Spearman", 20)
            addUnits("Spearman", 30)
        }
        val troop = army.getAllTroops().filterNotNull().last()
        assertTrue(army.contains(troop))
    }
}
