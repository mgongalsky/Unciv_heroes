package com.unciv.pure.domain.city

/** Resolves the editor's optional capital preference without changing the authored map. */
object ScenarioCityCapitals {
    fun resolve(cities: List<ScenarioCity>): List<ScenarioCity> {
        val result = cities.map { it.deepCopy() }
        for (city in result.filter { it.owner == ScenarioCityOwner.Neutral }) {
            city.isCapital = false
            city.isOriginalCapital = false
        }
        for (owned in result.filter { it.owner == ScenarioCityOwner.PlayerSlot }.groupBy { it.ownerSlotId }.values) {
            val capital = owned.firstOrNull { it.isCapital }
                ?: owned.firstOrNull { it.isOriginalCapital }
                ?: owned.first()
            for (city in owned) {
                city.isCapital = city === capital
                city.isOriginalCapital = city === capital
            }
        }
        return result
    }
}
