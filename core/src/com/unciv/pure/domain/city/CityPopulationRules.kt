package com.unciv.pure.domain.city

import kotlin.math.floor
import kotlin.math.pow

/** Чистые расчёты населения. Изменение пищи, назначений и религии выполняет PopulationManager. */
object CityPopulationRules {
    /** Прежняя формула порога роста; null-модификатор означает отсутствие поправки AI. */
    fun foodToNextPopulation(population: Int, isCityState: Boolean, aiGrowthModifier: Float?): Int {
        var required = 5 + 3 * (population - 1) + floor((population - 1).toDouble().pow(1.8))
        if (isCityState) required *= 1.5f
        if (aiGrowthModifier != null) required *= aiGrowthModifier
        return required.toInt()
    }

    /** Возвращает допустимое изменение, а не новое население: число жителей не опускается ниже нуля. */
    fun populationChange(population: Int, requestedChange: Int): Int =
        if (population + requestedChange < 0) -population else requestedChange
}
