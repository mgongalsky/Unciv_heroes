package com.unciv.logic.city

import com.unciv.logic.map.TileMap
import com.unciv.pure.domain.city.CityPosition

/** Проверки сохранённых городов до восстановления ссылок клеток и игровых менеджеров. */
object CitySaveValidation {
    /** Неизвестные определения отклоняются с диагностикой; сохранённый прогресс не удаляется молча. */
    fun validateDefinitions(cities: List<CityInfo>, ruleset: com.unciv.models.ruleset.Ruleset) {
        // Пустая очередь допустима; постоянные проекты не являются обычными зданиями или юнитами.
        fun knownConstruction(name: String) = name.isEmpty() || name in ruleset.buildings ||
            name in ruleset.units || name in ruleset.cityEvents ||
            name in PerpetualConstruction.perpetualConstructionsMap
        for (city in cities) {
            val constructions = city.cityConstructions
            for (name in constructions.builtBuildings) {
                require(name in ruleset.buildings) { "City ${city.id}: unknown building $name" }
            }
            for (name in constructions.preparedCityEvents) {
                require(name in ruleset.cityEvents) { "City ${city.id}: unknown city event $name" }
            }
            for (name in constructions.constructionQueue) {
                require(knownConstruction(name)) { "City ${city.id}: unknown queued construction $name" }
            }
            for (name in constructions.inProgressConstructions.keys) {
                require(knownConstruction(name)) { "City ${city.id}: unknown construction with stored production $name" }
            }
        }
    }

    /** Центр и назначенные клетки должны существовать; территории разных городов не пересекаются. */
    fun validateTerritory(cities: List<CityInfo>, tileMap: TileMap) {
        val positions = tileMap.values.mapTo(HashSet()) { CityPosition(it.position.x, it.position.y) }
        val claimed = HashMap<CityPosition, String>()
        for (city in cities) {
            require(city.state.position in positions) { "City ${city.id}: center ${city.state.position} is outside the map" }
            require(city.location in city.tiles) { "City ${city.id}: center is missing from its territory" }
            for (tile in city.tiles) {
                val position = CityPosition(tile.x, tile.y)
                require(position in positions) { "City ${city.id}: territory tile $position is outside the map" }
                val previous = claimed.put(position, city.id)
                require(previous == null) { "City ${city.id}: territory tile $position is also claimed by city $previous" }
            }
            // Назначения проверяются отдельно от владения: здесь контролируются границы карты.
            for (tile in city.workedTiles + city.lockedTiles) {
                val position = CityPosition(tile.x, tile.y)
                require(position in positions) { "City ${city.id}: assigned tile $position is outside the map" }
            }
        }
    }
}
