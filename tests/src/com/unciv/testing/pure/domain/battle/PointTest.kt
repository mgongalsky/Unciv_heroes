package com.unciv.testing.pure.domain.battle

import com.unciv.pure.domain.battle.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PointTest {
    @Test
    fun `equal coordinates have value equality and equal hash codes`() {
        val first = Point(3, -2)
        val second = Point(3, -2)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `copy changes only the selected coordinate`() {
        val original = Point(3, -2)
        val moved = original.copy(y = 4)

        assertEquals(Point(3, 4), moved)
        assertNotEquals(original, moved)
    }
}
