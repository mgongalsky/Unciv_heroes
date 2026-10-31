package com.unciv.pure.domain.city

/** Локальные инварианты описания одного города, независимо от карты и участников запуска. */
object ScenarioCityRules {
    fun validate(city: ScenarioCity) {
        val label = "Scenario city ${city.id}"
        require(city.id.isNotBlank()) { "$label: empty or duplicate ID" }
        require(city.name.isNotBlank()) { "$label: name is empty" }
        require(city.population > 0 && city.foodStored >= 0) { "$label: invalid population or food" }
        if (city.owner == ScenarioCityOwner.Neutral) {
            require(city.ownerSlotId.isEmpty()) { "$label: neutral city refers to a player slot" }
            require(city.typeMode == ScenarioCityTypeMode.Fixed) { "$label: neutral city requires a fixed type" }
        }
        require(
            (city.typeMode == ScenarioCityTypeMode.Fixed && city.fixedType.isNotBlank()) ||
                (city.typeMode == ScenarioCityTypeMode.FromStartingPlayer && city.fixedType.isEmpty())
        ) { "$label: invalid city type configuration" }
        require(city.buildings.distinct().size == city.buildings.size && city.buildings.none { it.isBlank() }) {
            "$label: empty or duplicate building"
        }
        for (troop in city.garrison) require(troop.unitName.isNotBlank() && troop.amount > 0) {
            "$label: invalid garrison stack"
        }
        val territory = city.territory.ifEmpty { listOf(city.position) }
        require(city.position in territory) { "$label: territory does not contain its center" }
        require(territory.distinct().size == territory.size) { "$label: duplicate territory position" }
    }
}
