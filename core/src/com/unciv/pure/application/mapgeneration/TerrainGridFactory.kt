package com.unciv.pure.application.mapgeneration

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Создаёт геометрию стенда в историческом порядке TileMap, без Vector2 и движка. */
object TerrainGridFactory {
    fun rectangular(width: Int, height: Int, terrain: String, worldWrap: Boolean = false): List<TerrainSurfaceCell> {
        require(!worldWrap) { "Generated experiment grids do not support worldWrap; provide an explicit snapshot graph" }
        require(width in 1..500 && height in 1..500) { "Grid dimensions must be in 1..500" }
        val coordinates = mutableListOf<Pair<Int, Int>>()
        for (column in -width / 2..(width - 1) / 2) {
            for (row in -height / 2..(height - 1) / 2) {
                // Исторический evenQ фактически является odd-row; сохраняем Float-порядок преобразования.
                val cubeX = column.toFloat() - (row.toFloat() - (row and 1)) / 2
                val hexX = -cubeX - row.toFloat()
                coordinates.add(hexX.toInt() to row)
            }
        }
        return build(coordinates, terrain)
    }

    fun hexagonal(radius: Int, terrain: String, worldWrap: Boolean = false): List<TerrainSurfaceCell> {
        require(!worldWrap) { "Generated experiment grids do not support worldWrap; provide an explicit snapshot graph" }
        require(radius in 0..200) { "Grid radius must be in 0..200" }
        val coordinates = mutableListOf(0 to 0)
        for (distance in 1..radius) {
            var x = distance
            var y = -distance
            repeat(distance) {
                coordinates.add(x to y); coordinates.add(-x to -y)
                y++
            }
            repeat(distance) {
                coordinates.add(x to y); coordinates.add(-x to -y)
                x--; y++
            }
            repeat(distance) {
                coordinates.add(x to y); coordinates.add(-x to -y)
                x--
            }
        }
        return build(coordinates, terrain)
    }

    private fun build(coordinates: List<Pair<Int, Int>>, terrain: String): List<TerrainSurfaceCell> {
        require(terrain.isNotBlank()) { "Initial terrain must not be blank" }
        val indices = coordinates.withIndex().associate { it.value to it.index }
        val offsets = listOf(1 to -1, -1 to 1, 1 to 0, -1 to 0, 0 to 1, 0 to -1)
        val length = sqrt(3.0).toFloat()
        fun vector(hour: Int): Pair<Float, Float> {
            val angle = (2 * Math.PI * (hour / 12f)).toFloat()
            return sin(angle.toDouble()).toFloat() * length to cos(angle.toDouble()).toFloat() * length
        }
        val vx = vector(9)
        val vy = vector(11)
        return coordinates.map { (x, y) ->
            val xf = x.toFloat()
            val yf = y.toFloat()
            TerrainSurfaceCell(x, y,
                (vx.first * xf + vy.first * yf).toDouble(),
                (vx.second * xf + vy.second * yf).toDouble(),
                yf, xf - yf / 2,
                offsets.mapNotNull { (dx, dy) -> indices[(x + dx) to (y + dy)] }, terrain)
        }
    }
}
