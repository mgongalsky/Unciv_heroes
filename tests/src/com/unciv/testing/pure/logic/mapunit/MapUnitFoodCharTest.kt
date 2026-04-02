package com.unciv.testing.pure.logic.mapunit

import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class MapUnitFoodCharTest {

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())

        val fakeRuleset = Ruleset().apply {
            val unitTypeObj = UnitType().apply { name = "Melee" }
            unitTypes["Melee"] = unitTypeObj
            val spearman = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
            }
            units["Spearman"] = spearman
        }

        startKoin {
            allowOverride(true)
            modules(module { single { fakeRuleset } })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun characterize_mapUnit_food() {
        val unit = TestableMapUnit()

        // Initial state
        assertEquals(3.0f, unit.hero.currentFood, 0.01f)
        assertEquals(15.0f, unit.basicFoodCapacity, 0.01f)
        assertEquals(0.0f, unit.foodCapacityBonus, 0.01f)

        // After addFood(5f)
        unit.addFood(5f)
        assertEquals(8.0f, unit.hero.currentFood, 0.01f)

        // After addFood(-10f) — уходит в минус, ограничений нет
        unit.addFood(-10f)
        assertEquals(-2.0f, unit.hero.currentFood, 0.01f)

        // Army population с пустой FakeArmyInfo
        assertEquals(0, unit.calculateArmyPopulation())
    }
}
