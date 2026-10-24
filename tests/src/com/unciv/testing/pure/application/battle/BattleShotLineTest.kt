package com.unciv.testing.pure.application.battle

import com.unciv.logic.battle.BattleShotLine
import com.unciv.pure.domain.battle.Point
import org.junit.Assert.*
import org.junit.Test

class BattleShotLineTest {
    @Test
    fun `straight shots in all six hex directions contain only intermediate cells`() {
        for (direction in listOf(Point(1, 0), Point(0, 1), Point(-1, 1),
            Point(-1, 0), Point(0, -1), Point(1, -1))) {
            val target = Point(direction.x * 3, direction.y * 3)
            val expected = setOf(direction, Point(direction.x * 2, direction.y * 2))
            assertEquals(expected, BattleShotLine.intermediateCells(Point(0, 0), target))
            assertEquals(expected, BattleShotLine.intermediateCells(target, Point(0, 0)))
        }
    }

    @Test
    fun `same cell and adjacent cells have no intermediate blockers`() {
        assertTrue(BattleShotLine.intermediateCells(Point(0, 0), Point(0, 0)).isEmpty())
        assertTrue(BattleShotLine.intermediateCells(Point(0, 0), Point(1, 0)).isEmpty())
    }

    @Test
    fun `line on a hex edge includes both possible blocking cells`() {
        assertEquals(setOf(Point(1, 0), Point(0, 1)),
            BattleShotLine.intermediateCells(Point(0, 0), Point(1, 1)))
    }

    @Test
    fun `oblique lines are symmetric and exclude endpoints`() {
        val origin = Point(0, 0)
        for (x in -6..6) for (y in -6..6) {
            val target = Point(x, y)
            val cells = BattleShotLine.intermediateCells(origin, target)
            assertEquals(cells, BattleShotLine.intermediateCells(target, origin))
            assertFalse(cells.contains(origin))
            assertFalse(cells.contains(target))
        }
    }

    @Test
    fun `translating a line preserves its blocking cells`() {
        val offset = Point(-3, 2)
        for (x in -4..4) for (y in -4..4) {
            val expected = BattleShotLine.intermediateCells(Point(0, 0), Point(x, y))
                .map { Point(it.x + offset.x, it.y + offset.y) }.toSet()
            assertEquals(expected, BattleShotLine.intermediateCells(
                offset, Point(x + offset.x, y + offset.y)))
        }
    }
}
