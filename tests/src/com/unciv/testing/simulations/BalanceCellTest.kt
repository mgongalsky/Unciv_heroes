package com.unciv.testing.simulations

import org.junit.Assert.*
import org.junit.Test

class BalanceCellTest {
    private fun cell(wins: Int, losses: Int, stalemates: Int = 0) = BalanceCell(
        0, 1, 1, 1, wins + losses + stalemates, wins, losses, stalemates, 0, 0, 3.0
    )

    @Test
    fun `yellow includes both boundaries and nearby values change color`() {
        assertEquals(-1, cell(44, 56).shade)
        assertEquals(0, cell(45, 55).shade)
        assertEquals(0, cell(55, 45).shade)
        assertEquals(1, cell(56, 44).shade)
    }

    @Test
    fun `no decisive battles is unknown rather than fifty percent`() {
        val result = cell(0, 0, 20)
        assertNull(result.decisiveRate)
        assertEquals(2, result.shade)
        assertEquals("— · 20", result.toString())
    }

    @Test
    fun `rate excludes unresolved battles while sample retains them`() {
        val result = cell(3, 1, 6)
        assertEquals(0.75, result.decisiveRate!!, 0.00001)
        assertEquals(10, result.simulations)
    }

    @Test
    fun `worker record preserves all outcomes`() {
        val result = BalanceCell.parse("BALANCE1\tCELL\t0\t1\t2\t3\t20\t8\t6\t3\t2\t1\t12.5")
        assertEquals(listOf(0, 1, 2, 3), result.key)
        assertEquals(3, result.stalemates)
        assertEquals(2, result.maxTurns)
        assertEquals(1, result.other)
        assertEquals(12.5, result.averageTurns, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `inconsistent outcome totals are rejected`() {
        BalanceCell.parse("BALANCE1\tCELL\t0\t1\t2\t3\t20\t8\t6\t3\t2\t2\t12.5")
    }
}
