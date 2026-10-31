package com.unciv.pure.application.scenario

import com.unciv.pure.domain.city.*

/** Проверяет согласованность всего сценария, не создавая игровые объекты и не меняя входные данные. */
object ValidateScenarioUseCase {
    fun execute(
        cities: List<ScenarioCity>,
        slots: List<ScenarioPlayerSlot>,
        mapPositions: Set<CityPosition>,
        victorySettings: com.unciv.pure.domain.scenario.ScenarioVictorySettings? = null
    ) {
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
            // Связи между городами и картой проверяются на уровне подготовки сценария.
            for (position in city.territory.ifEmpty { listOf(city.position) }) {
                require(position in mapPositions) { "$label: territory is outside the map at $position" }
                val previous = claimed.put(position, city.id)
                require(previous == null) { "$label: territory at $position is already claimed by $previous" }
            }
        }
        if (victorySettings == null) return
        val ruleNames = HashSet<String>()
        for (rule in victorySettings.rules) {
            require(rule.victoryType.isNotBlank()) { "Scenario victory type has an empty name" }
            require(ruleNames.add(rule.victoryType)) { "Duplicate scenario victory type ${rule.victoryType}" }
        }
        val objectiveCities = HashSet<String>()
        for (objective in victorySettings.captureObjectives) {
            require(objective.cityId in cityIds) { "Capture objective refers to unknown city ${objective.cityId}" }
            require(objectiveCities.add(objective.cityId)) { "Duplicate capture objective ${objective.cityId}" }
            require(objective.eligibleSlotIds.isNotEmpty()) { "Capture objective ${objective.cityId} has no eligible slots" }
            require(objective.eligibleSlotIds.distinct().size == objective.eligibleSlotIds.size) {
                "Capture objective ${objective.cityId} has duplicate slots"
            }
            require(objective.eligibleSlotIds.all { it in slotIds }) {
                "Capture objective ${objective.cityId} refers to unknown player slot"
            }
        }
        val captureRequired = victorySettings.rules.any {
            it.victoryType == com.unciv.pure.domain.victory.CaptureCityObjective.VICTORY_TYPE &&
                    it.mode == com.unciv.pure.domain.scenario.ScenarioVictoryMode.Required
        }
        require(!captureRequired || victorySettings.captureObjectives.isNotEmpty()) {
            "Required CaptureCity victory has no objectives"
        }
    }
}
