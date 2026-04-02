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

class MovementRangeUseCaseUnitTest {

    // --- Базовое движение ---

    @Test
    fun `start tile is always in result with distance 0`() {
        val tiles = FakeNavigableTileFactory.createChain(3)
        val start = tiles[0]
        val result = MovementRangeUseCase.execute(start, 2f, FakeMovementContext())

        assertTrue(result.containsKey(start))
        assertEquals(0f, result[start]!!.totalDistance, 0.001f)
    }

    @Test
    fun `movement 0 returns only start tile`() {
        val tiles = FakeNavigableTileFactory.createChain(3)
        val start = tiles[0]
        val result = MovementRangeUseCase.execute(start, 0f, FakeMovementContext())

        assertEquals(0, result.size)
    }

    @Test
    fun `tile exactly at movement limit is reachable`() {
        val tiles = FakeNavigableTileFactory.createChain(4)
        val result = MovementRangeUseCase.execute(tiles[0], 2f, FakeMovementContext())

        assertTrue(result.containsKey(tiles[2]))
        assertEquals(2f, result[tiles[2]]!!.totalDistance, 0.001f)
    }

    @Test
    fun `tile beyond movement limit is not reachable`() {
        val tiles = FakeNavigableTileFactory.createChain(4)
        val result = MovementRangeUseCase.execute(tiles[0], 2f, FakeMovementContext())

        assertFalse(result.containsKey(tiles[3]))
    }

    // --- Непроходимые тайлы ---

    @Test
    fun `impassable tile blocks path to tiles behind it`() {
        val tiles = FakeNavigableTileFactory.createChain(4)
        val wall = tiles[1]

        val context = object : FakeMovementContext() {
            override fun canPassThrough(tile: INavigableTile) =
                    (tile as FakeNavigableTile).name != wall.name
        }

        val result = MovementRangeUseCase.execute(tiles[0], 5f, context)

        assertFalse(result.containsKey(tiles[2]))
        assertFalse(result.containsKey(tiles[3]))
    }

    @Test
    fun `impassable tile itself is in result with max distance`() {
        val tiles = FakeNavigableTileFactory.createChain(3)
        val wall = tiles[1]

        val context = object : FakeMovementContext() {
            override fun canPassThrough(tile: INavigableTile) =
                    (tile as FakeNavigableTile).name != wall.name
        }

        val result = MovementRangeUseCase.execute(tiles[0], 5f, context)

        assertTrue(result.containsKey(wall))
        assertEquals(5f, result[wall]!!.totalDistance, 0.001f)
    }

    // --- Остров ---

    @Test
    fun `tile behind wall of impassable tiles is unreachable`() {
        val start = FakeNavigableTile("start")
        val wall1 = FakeNavigableTile("wall1")
        val wall2 = FakeNavigableTile("wall2")
        val island = FakeNavigableTile("island")

        start.addNeighbor(wall1)
        start.addNeighbor(wall2)
        wall1.addNeighbor(island)
        wall2.addNeighbor(island)

        val context = object : FakeMovementContext() {
            override fun canPassThrough(tile: INavigableTile) =
                    (tile as FakeNavigableTile).name != "wall1" &&
                            tile.name != "wall2"
        }

        val result = MovementRangeUseCase.execute(start, 10f, context)

        assertFalse(result.containsKey(island))
    }

    // --- Несвязный граф ---

    @Test
    fun `tiles in disconnected cluster are unreachable`() {
        val cluster1 = FakeNavigableTileFactory.createChain(3)

        val isolated1 = FakeNavigableTile("isolated_1")
        val isolated2 = FakeNavigableTile("isolated_2")
        isolated1.addNeighbor(isolated2)
        // cluster1 и isolated не связаны

        val result = MovementRangeUseCase.execute(cluster1[0], 10f, FakeMovementContext())

        assertFalse(result.containsKey(isolated1))
        assertFalse(result.containsKey(isolated2))
    }

    // --- Стоимость переходов ---

    @Test
    fun `expensive tile reachable only with enough movement`() {
        val tiles = FakeNavigableTileFactory.createChain(3)
        val expensiveTile = tiles[1]

        val context = object : FakeMovementContext() {
            override fun getMovementCost(from: INavigableTile, to: INavigableTile): Float =
                    if ((to as FakeNavigableTile).name == expensiveTile.name) 3f else 1f
        }

        val resultLow = MovementRangeUseCase.execute(tiles[0], 2f, context)
        val resultHigh = MovementRangeUseCase.execute(tiles[0], 3f, context)

        // при movement=2 дорогой тайл есть но недостижим для дальнейшего движения
        assertTrue(resultLow.containsKey(expensiveTile))
        assertEquals(3f, resultLow[expensiveTile]!!.totalDistance, 0.001f)
        assertFalse(resultLow.containsKey(tiles[2]))

        // при movement=3 дорогой тайл достижим
        assertTrue(resultHigh.containsKey(expensiveTile))
        assertEquals(3f, resultHigh[expensiveTile]!!.totalDistance, 0.001f)
    }

    // --- Hex карта ---

    @Test
    fun `hex - all ring tiles equidistant from center`() {
        val tiles = FakeNavigableTileFactory.createHexWithRing()
        val center = tiles.first { it.name == "center" }
        val result = MovementRangeUseCase.execute(center, 1f, FakeMovementContext())

        tiles.filter { it.name.startsWith("ring") }.forEach {
            assertEquals(1f, result[it]!!.totalDistance, 0.001f)
        }
    }

    @Test
    fun `hex - movement 0 from ring reaches nothing`() {
        val tiles = FakeNavigableTileFactory.createHexWithRing()
        val start = tiles.first { it.name == "ring_0" }
        val result = MovementRangeUseCase.execute(start, 0f, FakeMovementContext())

        assertEquals(0, result.size)
    }
}
