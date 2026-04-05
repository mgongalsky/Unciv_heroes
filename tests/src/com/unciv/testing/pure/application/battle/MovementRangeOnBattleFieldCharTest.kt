package com.unciv.testing.pure.application.pathfinding

import com.unciv.pure.application.pathfinding.BattleMovementContext
import com.unciv.pure.application.pathfinding.MovementRangeUseCase
import com.unciv.testing.pure.fakes.FakeBattleFieldBuilder
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MovementRangeOnBattleFieldCharTest {

    private lateinit var grid: Array<Array<FakeBattleTile>>
    private val context = BattleMovementContext()

    @Before
    fun setUp() {
        grid = FakeBattleFieldBuilder.buildGrid(14, 8)
    }

    private fun tile(x: Int, y: Int) = FakeBattleFieldBuilder.tileAt(grid, x, y)

    private fun reachableFrom(x: Int, y: Int, movement: Float) =
            MovementRangeUseCase.execute(
                startTile = tile(x, y),
                unitMovement = movement,
                context = context
            )

    @Test
    fun `movement 0 returns empty result`() {
        val reachable = reachableFrom(7, 4, movement = 0f)
        assertTrue(reachable.isEmpty())
    }

    @Test
    fun `movement 1 from center returns start tile and 4 neighbors`() {
        val reachable = reachableFrom(7, 4, movement = 1f)
        assertEquals(5, reachable.size)
        assertTrue(reachable.containsKey(tile(7, 4)))
        assertTrue(reachable.containsKey(tile(6, 4)))
        assertTrue(reachable.containsKey(tile(8, 4)))
        assertTrue(reachable.containsKey(tile(7, 3)))
        assertTrue(reachable.containsKey(tile(7, 5)))
    }

    @Test
    fun `movement 3 from center returns 25 tiles`() {
        val reachable = reachableFrom(7, 4, movement = 3f)
        assertEquals(25, reachable.size)
    }

    @Test
    fun `movement 3 from center - tiles at boundary distance are included`() {
        val reachable = reachableFrom(7, 4, movement = 3f)
        // distance == movement — включается
        assertTrue(reachable.containsKey(tile(10, 4)))
        assertTrue(reachable.containsKey(tile(7, 1)))
        assertTrue(reachable.containsKey(tile(7, 7)))
    }

    @Test
    fun `movement 3 from center - tiles beyond boundary are not included`() {
        val reachable = reachableFrom(7, 4, movement = 3f)
        assertFalse(reachable.containsKey(tile(11, 4))) // distance 4 — не включается
        assertFalse(reachable.containsKey(tile(3, 4)))  // distance 4 — не включается
        // tile(4,4) НЕ проверяем — distance==3, включается
    }

    @Test
    fun `distances from center are correct`() {
        val reachable = reachableFrom(7, 4, movement = 5f)
        assertEquals(0f, reachable[tile(7, 4)]?.totalDistance)
        assertEquals(1f, reachable[tile(8, 4)]?.totalDistance)
        assertEquals(2f, reachable[tile(9, 4)]?.totalDistance)
        assertEquals(3f, reachable[tile(10, 4)]?.totalDistance)
        assertEquals(4f, reachable[tile(11, 4)]?.totalDistance)
        assertEquals(5f, reachable[tile(12, 4)]?.totalDistance)
    }

    @Test
    fun `movement 3 from corner returns 10 tiles`() {
        val reachable = reachableFrom(0, 0, movement = 3f)
        assertEquals(10, reachable.size)
    }

    @Test
    fun `movement 3 from corner - boundary tiles included`() {
        val reachable = reachableFrom(0, 0, movement = 3f)
        assertTrue(reachable.containsKey(tile(3, 0)))
        assertTrue(reachable.containsKey(tile(0, 3)))
    }

    @Test
    fun `movement 3 from corner - tiles beyond boundary not included`() {
        val reachable = reachableFrom(0, 0, movement = 3f)
        assertFalse(reachable.containsKey(tile(4, 0)))
        assertFalse(reachable.containsKey(tile(0, 4)))
    }
}
