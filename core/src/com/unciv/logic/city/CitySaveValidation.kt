package com.unciv.logic.city

import com.unciv.logic.map.TileMap
import com.unciv.pure.domain.city.CityPosition

/** Checks serialized territory without requiring initialized tile or city adapters. */
object CitySaveValidation {
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
            for (tile in city.workedTiles + city.lockedTiles) {
                val position = CityPosition(tile.x, tile.y)
                require(position in positions) { "City ${city.id}: assigned tile $position is outside the map" }
            }
        }
    }
}
