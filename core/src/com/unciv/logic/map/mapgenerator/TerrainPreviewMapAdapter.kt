package com.unciv.logic.map.mapgenerator

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.logic.HexMath
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.mapgeneration.TerrainExperimentResult

/** Создаёт отдельную карту для игровых спрайтов, не создавая GameInfo и не меняя снимок. */
object TerrainPreviewMapAdapter {
    fun create(result: TerrainExperimentResult, ruleset: Ruleset): TileMap {
        val cells = result.cells
        require(cells.isNotEmpty()) { "No cells to display" }
        require(cells.map { it.column to it.row }.toSet().size == cells.size) { "Duplicate preview coordinates" }
        // TileMap хранит прямоугольную матрицу: ограничиваем её размер до выделения памяти.
        require(cells.all { it.column in -500..500 && it.row in -500..500 }) {
            "Game view supports coordinates within -500..500"
        }
        for (cell in cells) {
            require(cell.baseTerrain in ruleset.terrains && cell.features.all { it in ruleset.terrains }) {
                "Game view requires terrain definitions from the loaded graphical ruleset: ${cell.baseTerrain}, ${cell.features}"
            }
            val world = HexMath.hex2WorldCoords(Vector2(cell.column.toFloat(), cell.row.toFloat()))
            require(world.x.toDouble() == cell.x && world.y.toDouble() == cell.y) {
                "Game view requires standard hex coordinates"
            }
            require(cell.neighbors.all { it in cells.indices }) { "Invalid preview neighbor" }
        }
        val root = JsonValue(JsonValue.ValueType.`object`)
        val tiles = JsonValue(JsonValue.ValueType.array)
        root.addChild("tileList", tiles)
        for (cell in cells) {
            val tile = JsonValue(JsonValue.ValueType.`object`)
            val position = JsonValue(JsonValue.ValueType.`object`)
            position.addChild("x", JsonValue(cell.column.toLong()))
            position.addChild("y", JsonValue(cell.row.toLong()))
            tile.addChild("position", position)
            tile.addChild("baseTerrain", JsonValue(cell.baseTerrain))
            val features = JsonValue(JsonValue.ValueType.array)
            cell.features.forEach { features.addChild(JsonValue(it)) }
            tile.addChild("terrainFeatures", features)
            tiles.addChild(tile)
        }
        // Используем существующий контракт загрузки карты, не открывая изменяемый tileList наружу.
        val map: TileMap = Json().fromJson(TileMap::class.java, root.toJson(JsonWriter.OutputType.json))
        map.mapParameters.seed = result.input.settings.seed
        map.mapParameters.worldWrap = false
        map.setTransients(ruleset, false)
        map.clearStartingLocations()
        val mapped = map.values.toList()
        for ((index, tile) in mapped.withIndex()) {
            val cell = cells[index]
            require(tile.baseTerrain == cell.baseTerrain && tile.terrainFeatures == cell.features) {
                "Loaded graphical ruleset would normalize the displayed terrain"
            }
            val expectedNeighbors = cell.neighbors.map { cells[it].column to cells[it].row }.toSet()
            val actualNeighbors = tile.neighbors.map { it.position.x.toInt() to it.position.y.toInt() }.toSet()
            require(actualNeighbors == expectedNeighbors) {
                "Game view requires an unwrapped hex grid with standard adjacency"
            }
            tile.temperature = cell.temperature
            tile.humidity = cell.humidity
        }
        return map
    }
}
