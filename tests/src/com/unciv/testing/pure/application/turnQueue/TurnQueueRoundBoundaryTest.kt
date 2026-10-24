package com.unciv.testing.pure.application.turnQueue

import com.unciv.pure.domain.battle.TurnQueue
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class TurnQueueRoundBoundaryTest {
    @Test
    fun `boundary counts down and returns to end at next round`() {
        val a = List(2) { Troop(speed = 7) }
        val d = List(2) { Troop(speed = 5) }
        val queue = TurnQueue()
        queue.initialize(a, d)
        repeat(2) {
            for (remaining in 4 downTo 1) {
                assertEquals(remaining, queue.remainingInRound())
                val before = queue.getAll()
                assertEquals(remaining, queue.remainingInRound())
                assertEquals(before, queue.getAll())
                queue.advance()
            }
        }
        assertEquals(4, queue.remainingInRound())
    }

    @Test
    fun `casualties move boundary only if their activation was still pending`() {
        val a = List(3) { Troop(speed = 7) }
        val d = listOf(Troop(speed = 5))
        val queue = TurnQueue()
        queue.initialize(a, d)
        queue.advance()
        queue.remove(a[0])
        assertEquals(3, queue.remainingInRound())
        queue.remove(a[2])
        assertEquals(2, queue.remainingInRound())
        queue.remove(a[1])
        assertEquals(1, queue.remainingInRound())
        assertSame(d[0], queue.current())
        queue.remove(d[0])
        assertEquals(0, queue.remainingInRound())
        assertTrue(queue.getAll().isEmpty())
    }
}
