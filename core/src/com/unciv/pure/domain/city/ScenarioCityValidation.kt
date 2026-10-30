package com.unciv.pure.domain.city

/** Structural validation without a running game, graphics, or dependency injection. */
object ScenarioCityValidation {
    fun validate(
        cities: List<ScenarioCity>,
        slots: List<ScenarioPlayerSlot>,
        mapPositions: Set<CityPosition>
    ) {
        val slotIds = HashSet<String>()
        for (slot in slots) {
            require(slot.id.isNotBlank()) { "Scenario player slot has an empty ID" }
            require(slotIds.add(slot.id)) { "Duplicate scenario player slot ${slot.id}" }
        }
        val cityIds = HashSet<String>()
        val claimed = HashMap<CityPosition, String>()
        val capitals = HashSet<String>()
        val originalCapitals = HashSet<String>()
        for (city in cities) {
            val label = "Scenario city ${city.id}"
            require(city.id.isNotBlank() && cityIds.add(city.id)) { "$label: empty or duplicate ID" }
            require(city.name.isNotBlank()) { "$label: name is empty" }
            require(city.population > 0 && city.foodStored >= 0) { "$label: invalid population or food" }
            require(city.position in mapPositions) { "$label: center is outside the map" }
            when (city.owner) {
                ScenarioCityOwner.Neutral -> {
                    require(city.ownerSlotId.isEmpty()) { "$label: neutral city refers to a player slot" }
                    require(!city.isCapital && !city.isOriginalCapital) { "$label: neutral city cannot be a capital" }
                    require(city.typeMode == ScenarioCityTypeMode.Fixed) { "$label: neutral city requires a fixed type" }
                }
                ScenarioCityOwner.PlayerSlot -> {
                    require(city.ownerSlotId in slotIds) { "$label: unknown player slot ${city.ownerSlotId}" }
                    if (city.isCapital) require(capitals.add(city.ownerSlotId)) { "$label: duplicate capital for slot ${city.ownerSlotId}" }
                    if (city.isOriginalCapital) require(originalCapitals.add(city.ownerSlotId)) { "$label: duplicate original capital for slot ${city.ownerSlotId}" }
                }
            }
            require((city.typeMode == ScenarioCityTypeMode.Fixed && city.fixedType.isNotBlank()) ||
                (city.typeMode == ScenarioCityTypeMode.FromStartingPlayer && city.fixedType.isEmpty())) {
                "$label: invalid city type configuration"
            }
            require(city.buildings.distinct().size == city.buildings.size && city.buildings.none { it.isBlank() }) {
                "$label: empty or duplicate building"
            }
            for (troop in city.garrison) require(troop.unitName.isNotBlank() && troop.amount > 0) {
                "$label: invalid garrison stack"
            }
            val territory = city.territory.ifEmpty { listOf(city.position) }
            require(city.position in territory) { "$label: territory does not contain its center" }
            for (position in territory) {
                require(position in mapPositions) { "$label: territory is outside the map at $position" }
                val previous = claimed.put(position, city.id)
                require(previous == null) { "$label: territory at $position is already claimed by $previous" }
            }
        }
    }

fun validateCapitalsForStartup(cities: List<ScenarioCity>, slots: List<ScenarioPlayerSlot>) {
    for (slot in slots) {
        val owned =
                cities.filter { it.owner == ScenarioCityOwner.PlayerSlot && it.ownerSlotId == slot.id }
        if (owned.isEmpty()) continue
        val label = slot.name.ifBlank { slot.id }
        require(owned.count { it.isCapital } == 1) {
            "Scenario player slot '$label': select exactly one Current capital in the city editor (${owned.joinToString { it.name }})."
        }
        require(owned.count { it.isOriginalCapital } == 1) {
            "Scenario player slot '$label': select exactly one Original capital in the city editor (${owned.joinToString { it.name }})."
        }
    }
}
}
