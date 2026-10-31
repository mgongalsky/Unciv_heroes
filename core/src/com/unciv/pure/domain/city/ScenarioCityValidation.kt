package com.unciv.pure.domain.city

/**
 * Проверяет целостность описаний карты без GameInfo, графики и DI.
 * Знание конкретных зданий, войск и пригодности рельефа оставлено адаптеру ScenarioMapValidation.
 */
object ScenarioCityValidation {
    /** Проверка только читает входные данные; неполные столицы разрешаются отдельно при запуске. */
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
        // Общая таблица занятых клеток ловит и повтор внутри города, и пересечение разных городов.
        val claimed = HashMap<CityPosition, String>()
        for (city in cities) {
            val label = "Scenario city ${city.id}"
            require(city.id.isNotBlank() && cityIds.add(city.id)) { "$label: empty or duplicate ID" }
            require(city.name.isNotBlank()) { "$label: name is empty" }
            require(city.population > 0 && city.foodStored >= 0) { "$label: invalid population or food" }
            require(city.position in mapPositions) { "$label: center is outside the map" }
            when (city.owner) {
                ScenarioCityOwner.Neutral -> {
                    // Тип нейтрального города нельзя выводить из отсутствующего стартового игрока.
                    require(city.ownerSlotId.isEmpty()) { "$label: neutral city refers to a player slot" }
                    require(city.typeMode == ScenarioCityTypeMode.Fixed) { "$label: neutral city requires a fixed type" }
                }

                ScenarioCityOwner.PlayerSlot -> {
                    require(city.ownerSlotId in slotIds) { "$label: unknown player slot ${city.ownerSlotId}" }

                }
            }
            require(
                (city.typeMode == ScenarioCityTypeMode.Fixed && city.fixedType.isNotBlank()) ||
                        (city.typeMode == ScenarioCityTypeMode.FromStartingPlayer && city.fixedType.isEmpty())
            ) {
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
}
