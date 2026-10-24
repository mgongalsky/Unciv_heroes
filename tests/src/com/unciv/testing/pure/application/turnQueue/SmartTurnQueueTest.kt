package com.unciv.testing.pure.application.turnQueue

import com.unciv.pure.domain.battle.TurnQueue
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class SmartTurnQueueTest {
    private fun troops(count: Int, speed: Int) = List(count) {
        Troop(unitName = "Unit$it", amount = 1, speed = speed, maxHealth = 10)
    }

    @Test
    fun `forecast and actual turns match across three rounds`() {
        val a = troops(4, 7)
        val d = troops(4, 5)
        val expected = listOf(a[0], a[1], a[2], d[0], a[3], d[1], d[2], d[3])
        val queue = TurnQueue()
        queue.initialize(a, d)
        repeat(24) { turn ->
            val forecast = List(8) { expected[(turn + it) % 8] }
            assertEquals(forecast, queue.getAll())
            assertEquals(forecast, queue.getAll())
            assertSame(expected[turn % 8], queue.current())
            queue.advance()
        }
        assertTrue(a.all { it.speed == 7 })
        assertTrue(d.all { it.speed == 5 })
    }

    @Test
    fun `one point speed advantage allows two attackers before first defender`() {
        val a = troops(5, 6)
        val d = troops(5, 5)
        val queue = TurnQueue()
        queue.initialize(a, d)
        assertEquals(
            listOf(a[0], a[1], d[0], a[2], d[1], a[3], d[2], a[4], d[3], d[4]),
            queue.getAll()
        )
    }

    @Test
    fun `negative initiative still alternates and resets next round`() {
        val a = troops(4, 0)
        val d = troops(4, 0)
        val expected = a.indices.flatMap { listOf(a[it], d[it]) }
        val queue = TurnQueue()
        queue.initialize(a, d)
        repeat(2) {
            expected.forEach {
                assertSame(it, queue.current())
                queue.advance()
            }
        }
    }

    @Test
    fun `death before activation does not charge side penalty`() {
        val a = troops(4, 7)
        val d = troops(4, 5)
        val queue = TurnQueue()
        queue.initialize(a, d)
        queue.advance()
        queue.remove(a[2])
        assertSame(a[1], queue.current())
        queue.advance()
        assertSame(a[3], queue.current())
        assertEquals(listOf(a[3], d[0], d[1], d[2], d[3], a[0], a[1]), queue.getAll())
    }

    @Test
    fun `active death charges its side once before choosing successor`() {
        val a = troops(3, 6)
        val d = troops(2, 5)
        val queue = TurnQueue()
        queue.initialize(a, d)
        queue.advance()
        queue.remove(a[1])
        assertSame(d[0], queue.current())
        queue.remove(a[1])
        assertSame(d[0], queue.current())
        queue.advance()
        assertSame(a[2], queue.current())
    }

    @Test
    fun `death of already activated troop keeps earned penalty`() {
        val a = troops(3, 6)
        val d = troops(2, 5)
        val queue = TurnQueue()
        queue.initialize(a, d)
        queue.advance()
        queue.remove(a[0])
        queue.advance()
        assertSame(d[0], queue.current())
    }

    @Test
    fun `removing last pending troop starts fresh round`() {
        val a = troops(1, 7)
        val d = troops(1, 5)
        val queue = TurnQueue()
        queue.initialize(a, d)
        queue.advance()
        queue.remove(d[0])
        assertSame(a[0], queue.current())
        assertEquals(a, queue.getAll())
        queue.remove(a[0])
        queue.advance()
        assertTrue(queue.isEmpty())
        assertNull(queue.current())
        assertTrue(queue.getAll().isEmpty())
    }

    @Test
    fun `initialization discards previous participants and penalties`() {
        val queue = TurnQueue()
        queue.initialize(troops(4, 7), troops(4, 5))
        repeat(3) { queue.advance() }
        val a = troops(2, 5)
        val d = troops(2, 5)
        queue.initialize(a, d)
        assertEquals(listOf(a[0], d[0], a[1], d[1]), queue.getAll())
        queue.initialize(emptyList(), emptyList())
        assertTrue(queue.isEmpty())
        assertNull(queue.current())
    }

    @Test
    fun `single side completes every activation before repeating`() {
        val d = troops(3, 0)
        val queue = TurnQueue()
        queue.initialize(emptyList(), d)
        repeat(2) {
            d.forEach {
                assertSame(it, queue.current())
                queue.advance()
            }
        }
    }
}
