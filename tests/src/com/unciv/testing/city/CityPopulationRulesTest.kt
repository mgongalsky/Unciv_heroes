package com.unciv.testing.city

import com.unciv.pure.domain.city.CityPopulationRules
import org.junit.Assert.assertEquals
import org.junit.Test

class CityPopulationRulesTest {
    @Test
    fun growthThresholdIncludesNonlinearTerm() {
        assertEquals(listOf(5, 9, 14, 21), (1..4).map {
            CityPopulationRules.foodToNextPopulation(it, false, null)
        })
    }

    @Test
    fun modifiersApplyBeforeFinalTruncation() {
        assertEquals(13, CityPopulationRules.foodToNextPopulation(2, true, null))
        assertEquals(6, CityPopulationRules.foodToNextPopulation(2, false, 0.75f))
        assertEquals(10, CityPopulationRules.foodToNextPopulation(2, true, 0.75f))
    }

    @Test
    fun populationLossStopsAtZero() {
        assertEquals(-3, CityPopulationRules.populationChange(3, -8))
        assertEquals(-3, CityPopulationRules.populationChange(3, -3))
        assertEquals(-1, CityPopulationRules.populationChange(3, -1))
    }

    @Test
    fun growthAndNoChangeKeepRequestedDelta() {
        assertEquals(2, CityPopulationRules.populationChange(0, 2))
        assertEquals(0, CityPopulationRules.populationChange(3, 0))
    }
}
