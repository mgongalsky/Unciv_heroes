package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.unciv.Constants
import com.unciv.logic.map.TileInfo
import com.unciv.models.ruleset.unique.UniqueType

/** Режимы отображения текущей карты; выбор режима не является командой изменения террейна. */
enum class MapEditorTerrainLayer(val caption: String, val legend: String) {
    GAME("Game terrain", "Game textures; editing stays available"),
    BASE("Base terrain", "Terrain colours from the current ruleset"),
    TEMPERATURE("Temperature", "Blue: -1 cold; red: +1 warm; grey: no data. Last generated values."),
    HUMIDITY("Humidity", "Blue: 0 dry; red: 1 wet; grey: no data. Last generated values."),
    ELEVATION("Elevation", "Beige: flat; brown: hills; grey: mountains; blue: water"),
    VEGETATION("Vegetation", "Green: forest; dark green: jungle; beige: none; blue: water");

    /** Возвращает новый цвет, чтобы отрисовка не изменяла палитру или цвет террейна ruleset. */
    fun colorFor(tile: TileInfo): Color? = when (this) {
        GAME -> null
        BASE -> tile.getBaseTerrain().getColor().apply { a = 1f }
        TEMPERATURE -> heat(tile.temperature?.let { (it + 1.0) / 2.0 })
        HUMIDITY -> heat(tile.humidity)
        ELEVATION -> when {
            tile.isWater -> rgb(0x2864a0)
            tile.baseTerrain == Constants.mountain || tile.getBaseTerrain().hasUnique(UniqueType.OccursInChains) -> rgb(0xaaaaaa)
            tile.isHill() || tile.terrainFeatureObjects.any { it.hasUnique(UniqueType.OccursInGroups) } -> rgb(0x88704c)
            else -> rgb(0xc9c3a0)
        }
        VEGETATION -> {
            val vegetation = tile.terrainFeatureObjects.lastOrNull {
                it.name in Constants.vegetation || it.hasUnique(UniqueType.Vegetation)
            }
            when {
                tile.isWater -> rgb(0x2864a0)
                vegetation == null -> rgb(0xc9c3a0)
                vegetation.name == "Forest" -> rgb(0x36834b)
                vegetation.name == "Jungle" -> rgb(0x165638)
                else -> vegetation.getColor().apply { a = 1f }
            }
        }
    }

    private fun heat(value: Double?): Color {
        if (value == null || !value.isFinite()) return rgb(0x707070)
        val normalized = value.coerceIn(0.0, 1.0).toFloat()
        return Color(normalized, 64f / 255f, 1f - normalized, 1f)
    }
    private fun rgb(value: Int) = Color(((value shr 16) and 255) / 255f,
        ((value shr 8) and 255) / 255f, (value and 255) / 255f, 1f)
}
