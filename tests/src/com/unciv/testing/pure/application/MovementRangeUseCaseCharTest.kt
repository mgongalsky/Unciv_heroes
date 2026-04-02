package com.unciv.testing.pure.application.pathfinding

import com.unciv.pure.application.pathfinding.MovementRangeUseCase
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.testing.pure.fakes.FakeMovementContext
import com.unciv.testing.pure.fakes.FakeNavigableTile
import com.unciv.testing.pure.fakes.FakeNavigableTileFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementRangeUseCaseCharTest {

    @Test
    fun `chain - unit with movement 1 reaches only first neighbor`() {
        val tiles = FakeNavigableTileFactory.createChain(4)
        val start = tiles[0]
        val context = FakeMovementContext()

        val result = MovementRangeUseCase.execute(start, 1f, context)

        assertTrue(result.containsKey(tiles[1]))
        assertEquals(1f, result[tiles[1]]!!.totalDistance, 0.001f)
        assertFalse(result.containsKey(tiles[2]))
        assertFalse(result.containsKey(tiles[3]))
    }

    @Test
    fun `chain - unit with movement 2 reaches two neighbors`() {
        val tiles = FakeNavigableTileFactory.createChain(4)
        val start = tiles[0]
        val context = FakeMovementContext()

        val result = MovementRangeUseCase.execute(start, 2f, context)

        assertTrue(result.containsKey(tiles[1]))
        assertEquals(1f, result[tiles[1]]!!.totalDistance, 0.001f)
        assertTrue(result.containsKey(tiles[2]))
        assertEquals(2f, result[tiles[2]]!!.totalDistance, 0.001f)
        assertFalse(result.containsKey(tiles[3]))
    }

    @Test
    fun `chain - movement 0 reaches nothing`() {
        val tiles = FakeNavigableTileFactory.createChain(4)
        val start = tiles[0]
        val context = FakeMovementContext()

        val result = MovementRangeUseCase.execute(start, 0f, context)

        assertFalse(result.containsKey(tiles[1]))
    }

    @Test
    fun `chain - impassable tile is in result with max distance but blocks path`() {
        val tiles = FakeNavigableTileFactory.createChain(4)
        val start = tiles[0]
        val blocked = tiles[1]

        val context = object : FakeMovementContext() {
            override fun canPassThrough(tile: INavigableTile) =
                    (tile as FakeNavigableTile).name != blocked.name
        }

        val result = MovementRangeUseCase.execute(start, 3f, context)

        assertTrue(result.containsKey(blocked))
        assertEquals(3f, result[blocked]!!.totalDistance, 0.001f)
        assertFalse(result.containsKey(tiles[2]))
        assertFalse(result.containsKey(tiles[3]))
    }

    @Test
    fun `hex - unit with movement 1 reaches all ring tiles`() {
        val tiles = FakeNavigableTileFactory.createHexWithRing()
        val center = tiles.first { it.name == "center" }
        val context = FakeMovementContext()

        val result = MovementRangeUseCase.execute(center, 1f, context)

        val ringTiles = tiles.filter { it.name.startsWith("ring") }
        ringTiles.forEach {
            assertTrue(result.containsKey(it))
            assertEquals(1f, result[it]!!.totalDistance, 0.001f)
        }
    }

    @Test
    fun `hex - unit from ring reaches center and two neighbors`() {
        val tiles = FakeNavigableTileFactory.createHexWithRing()
        val start = tiles.first { it.name == "ring_0" }
        val context = FakeMovementContext()

        val result = MovementRangeUseCase.execute(start, 1f, context)

        assertTrue(result.containsKey(tiles.first { it.name == "center" }))
        assertTrue(result.containsKey(tiles.first { it.name == "ring_1" }))
        assertTrue(result.containsKey(tiles.first { it.name == "ring_5" }))
    }

    @Test
    fun `hex - high movement cost tiles visited but not traversable further`() {
        val tiles = FakeNavigableTileFactory.createHexWithRing()
        val center = tiles.first { it.name == "center" }
        val context = FakeMovementContext(cost = 2f)

        val result = MovementRangeUseCase.execute(center, 1f, context)

        val ringTiles = tiles.filter { it.name.startsWith("ring") }
        ringTiles.forEach {
            assertTrue(result.containsKey(it))
            assertEquals(2f, result[it]!!.totalDistance, 0.001f)
        }
    }
}
