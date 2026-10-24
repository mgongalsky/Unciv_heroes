package com.unciv.testing.pure.application.turnQueue

import com.unciv.pure.domain.battle.InitiativeOrder
import com.unciv.pure.domain.battle.InitiativeOrder.Entry
import org.junit.Assert.*
import org.junit.Test

class InitiativeOrderTest {
    @Test
    fun `four speed seven attackers interleave with four speed five defenders`() {
        val remaining = (List(4) { Entry(7, true) } + List(4) { Entry(5, false) })
            .mapIndexed { index, entry -> index to entry }.toMutableList()
        val order = mutableListOf<Int>()
        var attackerTurns = 0
        var defenderTurns = 0
        while (remaining.isNotEmpty()) {
            val index = InitiativeOrder.nextIndex(
                remaining.map { it.second }, attackerTurns, defenderTurns
            )!!
            val selected = remaining.removeAt(index)
            order.add(selected.first)
            if (selected.second.isAttacker) attackerTurns++ else defenderTurns++
        }
        assertEquals(listOf(0, 1, 2, 4, 3, 5, 6, 7), order)
    }

    @Test
    fun `attacker wins equal initiative even when defender has greater speed`() {
        val entries = listOf(Entry(7, false), Entry(5, true))
        assertEquals(1, InitiativeOrder.nextIndex(entries, 0, 2))
        assertEquals(0, InitiativeOrder.nextIndex(entries, 0, 1))
        assertEquals(1, InitiativeOrder.nextIndex(entries, 0, 3))
    }

    @Test
    fun `negative initiative is compared without clamping`() {
        val entries = listOf(Entry(1, true), Entry(1, false))
        assertEquals(-3L, InitiativeOrder.initiative(1, 4))
        assertEquals(1, InitiativeOrder.nextIndex(entries, 4, 3))
        assertEquals(0, InitiativeOrder.nextIndex(entries, 4, 4))
    }

    @Test
    fun `same side ties preserve input order`() {
        assertEquals(0, InitiativeOrder.nextIndex(List(3) { Entry(5, false) }, 0, 2))
    }

    @Test
    fun `empty candidates have no next troop`() {
        assertNull(InitiativeOrder.nextIndex(emptyList(), 0, 0))
    }

    @Test
    fun `penalty is explicit and selection does not mutate values`() {
        val entries = listOf(Entry(7, true), Entry(5, false))
        repeat(3) {
            assertEquals(0, InitiativeOrder.nextIndex(entries, 2, 0, penaltyPerTurn = 0))
            assertEquals(1, InitiativeOrder.nextIndex(entries, 2, 0, penaltyPerTurn = 2))
        }
        assertEquals(listOf(Entry(7, true), Entry(5, false)), entries)
    }

    @Test
    fun `large penalties do not overflow initiative comparison`() {
        assertEquals(
            Int.MIN_VALUE.toLong() - Int.MAX_VALUE.toLong() * Int.MAX_VALUE,
            InitiativeOrder.initiative(Int.MIN_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative penalty is rejected`() {
        InitiativeOrder.nextIndex(listOf(Entry(5, true)), 0, 0, -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative completed turn count is rejected`() {
        InitiativeOrder.nextIndex(listOf(Entry(5, true)), -1, 0)
    }
}
