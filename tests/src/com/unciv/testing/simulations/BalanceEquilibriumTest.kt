package com.unciv.testing.simulations

import org.junit.Assert.*
import org.junit.Test

class BalanceEquilibriumTest {
    private fun cell(a: Int, d: Int, wins: Int, losses: Int, unresolved: Int = 0) = BalanceCell(
        0, 1, a, d, wins + losses + unresolved, wins, losses, unresolved, 0, 0, 5.0
    )

    @Test
    fun `selects balancing counts rather than equal armies`() {
        val balanced = cell(7, 2, 10, 10)
        val result =
            BalanceEquilibrium.select(listOf(cell(2, 2, 0, 20), balanced, cell(8, 2, 18, 2)), 3)
        assertEquals(balanced, result.closest)
        assertTrue(result.complete)
        assertTrue(result.found)
    }

    @Test
    fun `symmetric win rates tie and prefer smaller army sum`() {
        val smaller = cell(2, 1, 6, 4)
        assertEquals(
            smaller,
            BalanceEquilibrium.select(listOf(cell(5, 4, 4, 6), smaller), 2).closest
        )
    }

    @Test
    fun `equal sums prefer fewer attackers regardless of arrival order`() {
        val fewerAttackers = cell(2, 3, 5, 5)
        val other = cell(3, 2, 10, 10)
        assertEquals(
            fewerAttackers,
            BalanceEquilibrium.select(listOf(other, fewerAttackers), 2).closest
        )
        assertEquals(
            fewerAttackers,
            BalanceEquilibrium.select(listOf(fewerAttackers, other), 2).closest
        )
    }

    @Test
    fun `unresolved battles cannot manufacture equilibrium`() {
        val result = BalanceEquilibrium.select(listOf(cell(1, 1, 0, 0, 20), cell(7, 2, 8, 2)), 2)
        assertEquals(7, result.closest!!.attackerAmount)
        assertFalse(result.found)
        assertEquals("Равновесие не найдено", result.description)
    }

    @Test
    fun `no decisive results stays unknown`() {
        val result = BalanceEquilibrium.select(listOf(cell(1, 1, 0, 0, 20)), 1)
        assertNull(result.closest)
        assertFalse(result.found)
        assertEquals("Нет решающих боёв", result.description)
    }

    @Test
    fun `partial equilibrium remains preliminary`() {
        val result = BalanceEquilibrium.select(listOf(cell(7, 2, 10, 10)), 400)
        assertFalse(result.complete)
        assertTrue(result.found)
        assertTrue(result.description.contains("Предварительное"))
        assertEquals(1, result.completedCells)
    }

    @Test
    fun `empty pair waits for data`() {
        val result = BalanceEquilibrium.select(emptyList(), 400)
        assertNull(result.closest)
        assertFalse(result.complete)
        assertEquals("Ожидает расчёта", result.description)
    }

    @Test
    fun `equilibrium band includes both boundaries`() {
        assertTrue(BalanceEquilibrium.select(listOf(cell(2, 3, 45, 55)), 1).found)
        assertTrue(BalanceEquilibrium.select(listOf(cell(2, 3, 55, 45)), 1).found)
        assertFalse(BalanceEquilibrium.select(listOf(cell(2, 3, 44, 56)), 1).found)
    }
}
