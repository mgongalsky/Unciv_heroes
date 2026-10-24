package com.unciv.ui.battlescreen

import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.battle.session.BattleFieldDescription
import com.unciv.pure.domain.battle.Point

/** Builds graphical terrain from authoritative cells without running terrain generation. */
object BattleFieldViewMapFactory {
    fun create(description: BattleFieldDescription, ruleset: Ruleset): TileMap {
        require(description.width > 1 && description.height > 0)
        val count = description.width.toLong() * description.height
        require(count <= 4096L && description.cells.size.toLong() == count)
        val cells = description.cells.associateBy { it.position }
        require(cells.size == description.cells.size) { "Duplicate battlefield coordinates" }
        description.cells.forEach { cell ->
            require(cell.temperature.isFinite() && cell.humidity.isFinite())
            require(ruleset.terrains.containsKey(cell.baseTerrain)) {
                "Missing battlefield terrain: ${cell.baseTerrain}"
            }
            cell.features.forEach { feature ->
                require(ruleset.terrains.containsKey(feature)) { "Missing battlefield feature: $feature" }
            }
        }
        val map = TileMap(description.width, description.height, ruleset, worldWrap = false)
        val positions = map.values.map { Point(it.position.x.toInt(), it.position.y.toInt()) }.toSet()
        require(positions == cells.keys) { "Battlefield coordinates do not match its dimensions" }
        map.values.forEach { tile ->
            val cell = cells.getValue(Point(tile.position.x.toInt(), tile.position.y.toInt()))
            tile.baseTerrain = cell.baseTerrain
            tile.temperature = cell.temperature
            tile.humidity = cell.humidity
            tile.setTerrainFeatures(cell.features.toList())
            tile.setTerrainTransients()
            require(tile.isImpassible() == cell.impassable) {
                "Client terrain rules differ from the server at ${cell.position}"
            }
        }
        return map
    }
}
