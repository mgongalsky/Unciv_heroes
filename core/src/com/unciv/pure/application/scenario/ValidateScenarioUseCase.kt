package com.unciv.pure.application.scenario

import com.unciv.pure.domain.city.*

/** Проверяет согласованность всего сценария, не создавая игровые объекты и не меняя входные данные. */
object ValidateScenarioUseCase {
    fun execute(cities: List<ScenarioCity>, slots: List<ScenarioPlayerSlot>, mapPositions: Set<CityPosition>) {
        val slotIds = HashSet<String>()
        for (slot in slots) {
            require(slot.id.isNotBlank()) { "Scenario player slot has an empty ID" }
            require(slotIds.add(slot.id)) { "Duplicate scenario player slot ${slot.id}" }
        }
        val cityIds = HashSet<String>()
        val claimed = HashMap<CityPosition, String>()
        for (city in cities) {
            ScenarioCityRules.validate(city)
            val label = "Scenario city ${city.id}"
            require(cityIds.add(city.id)) { "$label: empty or duplicate ID" }
            require(city.position in mapPositions) { "$label: center is outside the map" }
            if (city.owner == ScenarioCityOwner.PlayerSlot) {
                require(city.ownerSlotId in slotIds) { "$label: unknown player slot ${city.ownerSlotId}" }
            }
            // Связи между городами и картой проверяются на уровне операции подготовки сценария.
            for (position in city.territory.ifEmpty { listOf(city.position) }) {
                require(position in mapPositions) { "$label: territory is outside the map at $position" }
                val previous = claimed.put(position, city.id)
                require(previous == null) { "$label: territory at $position is already claimed by $previous" }
            }
        }
    }
}
