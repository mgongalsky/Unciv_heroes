package com.unciv.testing.pure.domain.mapgeneration

import com.unciv.pure.domain.mapgeneration.PerlinNoise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверки числовых свойств шума без TileMap, движка и файлов. */
class PerlinNoiseTest {
    /** На целочисленной решётке градиентный шум равен нулю, в том числе при отрицательных координатах. */
    @Test
    fun integerLatticeHasZeroNoise() {
        for (x in listOf(-257.0, -1.0, 0.0, 1.0, 256.0))
            assertEquals(0.0, PerlinNoise.noise(x, -3.0, 17.0), 0.0)
    }

    /** Таблица перестановок повторяется через 256; дробные двоичные координаты не вносят округления. */
    @Test
    fun permutationRepeatsAcrossPositiveAndNegativePeriods() {
        val expected = PerlinNoise.noise(0.125, -0.25, 0.5)
        assertEquals(expected, PerlinNoise.noise(256.125, -256.25, 256.5), 0.0)
    }

    /** Масштаб применяется к трём координатам, включая срез z; это сохраняемый контракт эксперимента. */
    @Test
    fun scaleDividesAllCoordinates() {
        assertEquals(
            PerlinNoise.noise(0.125, -0.25, 0.5),
            PerlinNoise.noise3d(0.25, -0.5, 1.0, nOctaves = 1, scale = 2.0),
            0.0
        )
    }

    /** Ridged-режим неотрицателен и воспроизводим для дробных и отрицательных входов. */
    @Test
    fun ridgedNoiseIsNonnegativeAndRepeatable() {
        for (x in listOf(-13.25, 0.125, 71.5)) {
            val value = PerlinNoise.ridgedNoise3d(x, -0.25, 19.75)
            assertTrue(value.isFinite() && value >= 0.0)
            assertEquals(value, PerlinNoise.ridgedNoise3d(x, -0.25, 19.75), 0.0)
        }
    }
}
