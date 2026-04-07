package com.unciv.testing.pure.application.army

import com.unciv.logic.army.ArmyInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ArcherRangedStrengthTest {

    private lateinit var fakeRuleset: Ruleset

    @Before
    fun setUp() {
        fakeRuleset = Ruleset().apply {
            val meleeType = UnitType().apply { name = "Melee" }
            val archeryType = UnitType().apply { name = "Archery" }
            unitTypes["Melee"] = meleeType
            unitTypes["Archery"] = archeryType

            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                health = 100
                damage = 10
                speed = 5
                rangedStrength = 0
            }
            units["Archer"] = BaseUnit().apply {
                name = "Archer"
                unitType = "Archery"
                health = 80
                damage = 8
                speed = 4
                rangedStrength = 10
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

    // --- Layer 1: Ruleset itself ---

    @Test
    fun `ruleset contains Archer with rangedStrength 10`() {
        val baseUnit = fakeRuleset.units["Archer"]!!
        assertEquals(10, baseUnit.rangedStrength)
    }

    @Test
    fun `ruleset contains Spearman with rangedStrength 0`() {
        val baseUnit = fakeRuleset.units["Spearman"]!!
        assertEquals(0, baseUnit.rangedStrength)
    }

    // --- Layer 2: RulesetTroopDefinitionSource ---

    @Test
    fun `RulesetTroopDefinitionSource returns rangedStrength 10 for Archer`() {
        val source = RulesetTroopDefinitionSource(fakeRuleset)
        assertEquals(10, source.getRangedStrength("Archer"))
    }

    @Test
    fun `RulesetTroopDefinitionSource returns rangedStrength 0 for Spearman`() {
        val source = RulesetTroopDefinitionSource(fakeRuleset)
        assertEquals(0, source.getRangedStrength("Spearman"))
    }

    // --- Layer 3: TroopFactory ---

    @Test
    fun `TroopFactory creates Archer with rangedStrength 10`() {
        val source = RulesetTroopDefinitionSource(fakeRuleset)
        val troop = TroopFactory.create("Archer", 10, source)
        assertEquals(10, troop.rangedStrength)
    }

    @Test
    fun `TroopFactory creates Spearman with rangedStrength 0`() {
        val source = RulesetTroopDefinitionSource(fakeRuleset)
        val troop = TroopFactory.create("Spearman", 10, source)
        assertEquals(0, troop.rangedStrength)
    }

    @Test
    fun `TroopFactory Archer isRanged returns true`() {
        val source = RulesetTroopDefinitionSource(fakeRuleset)
        val troop = TroopFactory.create("Archer", 10, source)
        assertTrue(troop.isRanged)
    }

    @Test
    fun `TroopFactory Spearman isRanged returns false`() {
        val source = RulesetTroopDefinitionSource(fakeRuleset)
        val troop = TroopFactory.create("Spearman", 10, source)
        assertTrue(!troop.isRanged)
    }

    // --- Layer 4: ArmyInfo ---

    @Test
    fun `ArmyInfo Archer troop has rangedStrength 10`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Archer", 10)
        }
        val troop = army.getAllTroops().filterNotNull().first()
        assertEquals(10, troop.rangedStrength)
    }

    @Test
    fun `ArmyInfo Spearman troop has rangedStrength 0`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Spearman", 10)
        }
        val troop = army.getAllTroops().filterNotNull().first()
        assertEquals(0, troop.rangedStrength)
    }

    @Test
    fun `ArmyInfo Archer troop isRanged returns true`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Archer", 10)
        }
        val troop = army.getAllTroops().filterNotNull().first()
        assertTrue(troop.isRanged)
    }

    @Test
    fun `ArmyInfo mixed army - Archer isRanged Spearman is not`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply {
            addUnits("Archer", 10)
            addUnits("Spearman", 10)
        }
        val troops = army.getAllTroops().filterNotNull()
        val archer = troops.first { it.unitName == "Archer" }
        val spearman = troops.first { it.unitName == "Spearman" }
        assertTrue(archer.isRanged)
        assertTrue(!spearman.isRanged)
    }
}
