package com.unciv.pure.domain.mapgeneration

/** Широты участка мира: положительные значения — север, отрицательные — юг. */
data class LatitudeRange(val top: Double, val bottom: Double) {
    init {
        require(top.isFinite() && bottom.isFinite() && top in -90.0..90.0 && bottom in -90.0..90.0) {
            "Latitudes must be finite and between -90 and 90 degrees"
        }
        require(top >= bottom) { "Top latitude must be greater than or equal to bottom latitude" }
    }

    /** Максимум мировой Y — верх изображения; единственная строка получает середину диапазона. */
    fun at(y: Double, minY: Double, maxY: Double): Double {
        require(y.isFinite() && minY.isFinite() && maxY.isFinite() && minY <= maxY && y in minY..maxY) {
            "Invalid vertical extent for latitude projection"
        }
        // Float-тригонометрия HexMath даёт небольшой дрейф Y даже в одной горизонтальной строке.
        val fraction = if (maxY - minY <= 0.0001) 0.5 else (y - minY) / (maxY - minY)
        return bottom + (top - bottom) * fraction
    }
}
