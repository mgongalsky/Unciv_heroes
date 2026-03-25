package com.unciv.testing.pure.logic.mapunit

import com.unciv.logic.map.MapUnit
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapUnitFoodCharTest {

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())
    }

    @Test
    fun characterize_mapUnit_food() {
        val unit = TestableMapUnit()

        // Initial state
        assertEquals(3.0f, unit.getCurrentFood(), 0.01f)
        assertEquals(15.0f, unit.basicFoodCapacity, 0.01f)
        assertEquals(0.0f, unit.foodCapacityBonus, 0.01f)

        // After addFood(5f)
        unit.addFood(5f)
        assertEquals(8.0f, unit.getCurrentFood(), 0.01f)

        // After addFood(-10f) — уходит в минус, ограничений нет
        unit.addFood(-10f)
        assertEquals(-2.0f, unit.getCurrentFood(), 0.01f)

        // Army population с пустой FakeArmyInfo
        assertEquals(0, unit.calculateArmyPopulation())
    }
}
