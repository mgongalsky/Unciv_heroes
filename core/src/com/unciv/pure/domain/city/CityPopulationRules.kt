package com.unciv.pure.domain.city

import kotlin.math.floor
import kotlin.math.pow

/** Local population arithmetic; world effects are applied by the caller. */
object CityPopulationRules {
    fun foodToNextPopulation(population: Int, isCityState: Boolean, aiGrowthModifier: Float?): Int {
        var required = 5 + 3 * (population - 1) + floor((population - 1).toDouble().pow(1.8))
        if (isCityState) required *= 1.5f
        if (aiGrowthModifier != null) required *= aiGrowthModifier
        return required.toInt()
    }

    /** Returns the applied delta, preserving the existing minimum of zero residents. */
    fun populationChange(population: Int, requestedChange: Int): Int =
        if (population + requestedChange < 0) -population else requestedChange
}
