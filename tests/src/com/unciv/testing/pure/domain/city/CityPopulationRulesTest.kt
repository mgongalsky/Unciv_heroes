package com.unciv.testing.pure.domain.city

import com.unciv.pure.domain.city.CityPopulationRules
import org.junit.Assert.assertEquals
import org.junit.Test

/** Защищает прежнюю арифметику населения после её извлечения из игрового менеджера. */
class CityPopulationRulesTest {
    /** Рост порога нелинеен; проверяем несколько соседних значений населения. */
    @Test
    fun growthThresholdIncludesNonlinearTerm() {
        assertEquals(listOf(5, 9, 14, 21), (1..4).map {
            CityPopulationRules.foodToNextPopulation(it, false, null)
        })
    }

    /** Поправки города-государства и AI применяются до итогового усечения дробной части. */
    @Test
    fun modifiersApplyBeforeFinalTruncation() {
        assertEquals(13, CityPopulationRules.foodToNextPopulation(2, true, null))
        assertEquals(6, CityPopulationRules.foodToNextPopulation(2, false, 0.75f))
        assertEquals(10, CityPopulationRules.foodToNextPopulation(2, true, 0.75f))
    }

    /** Чрезмерная убыль ограничивается нулём жителей, а не отрицательным населением. */
    @Test
    fun populationLossStopsAtZero() {
        assertEquals(-3, CityPopulationRules.populationChange(3, -8))
        assertEquals(-3, CityPopulationRules.populationChange(3, -3))
        assertEquals(-1, CityPopulationRules.populationChange(3, -1))
    }

    /** Для роста и нулевого изменения правило возвращает исходную дельту без поправок. */
    @Test
    fun growthAndNoChangeKeepRequestedDelta() {
        assertEquals(2, CityPopulationRules.populationChange(0, 2))
        assertEquals(0, CityPopulationRules.populationChange(3, 0))
    }
}
