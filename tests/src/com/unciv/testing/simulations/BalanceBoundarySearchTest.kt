package com.unciv.testing.simulations

import org.junit.Assert.*
import org.junit.Test

class BalanceBoundarySearchTest {
    private fun cell(a: Int, d: Int, wins: Int, losses: Int) = BalanceCell(
        0, 1, a, d, 20, wins, losses, 20 - wins - losses, 0, 0, 5.0
    )

    @Test
    fun `finds one to two point five from measured equal chances`() {
        val result = BalanceBoundarySearch.run(10, 100) { a, d ->
            when {
                d < 25 -> cell(a, d, 18, 2)
                d > 25 -> cell(a, d, 2, 18)
                else -> cell(a, d, 10, 10)
            }
        }
        assertEquals(BalanceBoundarySearch.Status.NEAR_HALF, result.status)
        assertEquals(2.5, result.defendersPerAttacker!!, 0.0)
        assertTrue(result.probes.any { it.defenderAmount == 25 })
    }

    @Test
    fun `sharp crossing yields a bounded estimate instead of an eighty percent army`() {
        val result = BalanceBoundarySearch.run(10, 100) { a, d ->
            if (d <= 25) cell(a, d, 16, 4) else cell(a, d, 4, 16)
        }
        assertEquals(BalanceBoundarySearch.Status.BRACKETED, result.status)
        assertEquals(25, result.lowerDefenders)
        assertEquals(26, result.upperDefenders)
        assertEquals(2.55, result.defendersPerAttacker!!, 0.000001)
    }

    @Test
    fun `one sided outcomes never produce a claimed balance ratio`() {
        val result = BalanceBoundarySearch.run(10, 100) { a, d -> cell(a, d, 16, 4) }
        assertEquals(BalanceBoundarySearch.Status.OUT_OF_RANGE, result.status)
        assertNull(result.defendersPerAttacker)
        assertEquals(100, result.probes.last().defenderAmount)
    }

    @Test
    fun `stronger single defender requires a larger attacker scale`() {
        val result = BalanceBoundarySearch.run(10, 100) { a, d -> cell(a, d, 0, 20) }
        assertEquals(BalanceBoundarySearch.Status.OUT_OF_RANGE, result.status)
        assertNull(result.defendersPerAttacker)
        assertEquals(1, result.probes.size)
    }

    @Test
    fun `no winner cannot be used as fifty percent`() {
        val result = BalanceBoundarySearch.run(10, 100) { a, d -> cell(a, d, 0, 0) }
        assertEquals(BalanceBoundarySearch.Status.INCONCLUSIVE, result.status)
        assertNull(result.defendersPerAttacker)
    }

    @Test
    fun `search stays bounded and does not repeat probes`() {
        val requested = mutableListOf<Int>()
        val result = BalanceBoundarySearch.run(20, 2000) { a, d ->
            requested += d
            if (d < 777) cell(a, d, 20, 0) else cell(a, d, 0, 20)
        }
        assertEquals(776, result.lowerDefenders)
        assertEquals(777, result.upperDefenders)
        assertEquals(requested.size, requested.distinct().size)
        assertTrue(requested.all { it in 1..2000 })
        assertTrue(requested.size < 25)
    }
}
