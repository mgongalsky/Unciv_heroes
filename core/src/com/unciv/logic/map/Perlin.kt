package com.unciv.logic.map

import com.unciv.pure.domain.mapgeneration.PerlinNoise

/** Совместимый API исторического шума для legacy-вызовов; единственная реализация находится в domain. */
object Perlin {
    fun noise3d(
        x: Double, y: Double, z: Double,
        nOctaves: Int = 3, persistence: Double = 0.5,
        lacunarity: Double = 2.0, scale: Double = 10.0
    ): Double = PerlinNoise.noise3d(x, y, z, nOctaves, persistence, lacunarity, scale)

    fun ridgedNoise3d(
        x: Double, y: Double, z: Double,
        nOctaves: Int = 3, persistence: Double = 0.5,
        lacunarity: Double = 2.0, scale: Double = 10.0
    ): Double = PerlinNoise.ridgedNoise3d(x, y, z, nOctaves, persistence, lacunarity, scale)

    fun noise(x: Double, y: Double, z: Double): Double = PerlinNoise.noise(x, y, z)
}
