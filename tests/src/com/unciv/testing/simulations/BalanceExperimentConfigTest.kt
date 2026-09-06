package com.unciv.testing.simulations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BalanceExperimentConfigTest {
    @Test
    fun `full overview includes both attack directions and mirrors`() {
        val config = BalanceExperimentConfig()
        assertEquals(10000, config.totalCells(5))
        assertEquals(200000, config.totalBattles(5))
        assertEquals((0L until 20L).toList(), config.seeds())
    }

    @Test
    fun `seed offset is reproducible`() {
        assertEquals(listOf(42L, 43L, 44L), BalanceExperimentConfig(3, 1, 42).seeds())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty sample is rejected`() {
        BalanceExperimentConfig(battlesPerCell = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `overflowing seed range is rejected`() {
        BalanceExperimentConfig(firstSeed = Long.MAX_VALUE)
    }

    @Test
    fun `unit file values replace preset characteristics`() {
        val text = BalanceUnitSources.preset.joinToString(",", "[", "]") {
            """{"name":"${it.name}","speed":${it.speed},"health":${it.health + 1},"damage":${it.damage},"rangedStrength":${it.rangedStrength}}"""
        }
        val loaded = BalanceUnitSources.parse(text)
        assertEquals(5, loaded.size)
        assertEquals(BalanceUnitSources.preset.first().health + 1, loaded.first().health)
        assertNotEquals(BalanceUnitSources.preset, loaded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing unit is reported instead of silently using preset`() {
        BalanceUnitSources.parse("[]")
    }
}
