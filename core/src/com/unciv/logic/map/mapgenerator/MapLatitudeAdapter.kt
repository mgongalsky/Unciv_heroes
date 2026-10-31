package com.unciv.logic.map.mapgenerator

import com.unciv.logic.HexMath
import com.unciv.logic.map.TileInfo
import com.unciv.logic.map.TileMap
import kotlin.math.abs

/** Единая проекция широты для климата, льда и чудес; геометрия клеток не изменяется. */
internal class MapLatitudeAdapter(private val map: TileMap) {
    private val range = map.mapParameters.validatedLatitudeRange()
    // Границы включают воду и горы: маска обрабатываемого слоя не меняет охват карты.
    private val worldY = if (range == null) emptyMap() else map.values.associateWith {
        HexMath.hex2WorldCoords(it.position).y.toDouble()
    }
    private val minY = worldY.values.minOrNull() ?: 0.0
    private val maxY = worldY.values.maxOrNull() ?: 0.0

    val maximum: Float = if (range == null) map.maxLatitude else 90f

    /** Float сохраняет точность входа общего ClimateGenerator и экспериментального пути. */
    fun latitude(tile: TileInfo): Float = range?.at(worldY.getValue(tile), minY, maxY)?.toFloat()
        ?: tile.latitude

    /** Фон льда использует ту же широту, но сохраняет собственное смешивание с шумом. */
    fun temperatureBackground(tile: TileInfo): Double =
        1.0 - 2.0 * abs(latitude(tile)) / maximum

    /** Проценты правила чудес относятся к полюсу; в Legacy — к историческому максимуму карты. */
    fun matchesPercentRange(tile: TileInfo, lowerPercent: Int, upperPercent: Int): Boolean {
        val lower = maximum * lowerPercent * 0.01f
        val upper = maximum * upperPercent * 0.01f
        return abs(latitude(tile)) in lower..upper
    }
}
