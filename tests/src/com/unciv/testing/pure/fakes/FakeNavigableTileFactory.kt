package com.unciv.testing.pure.fakes

import com.badlogic.gdx.math.Vector2

object FakeNavigableTileFactory {

    // Линейная цепочка: A - B - C - D
    fun createChain(count: Int): List<FakeNavigableTile> {
        val tiles = (0 until count).map {
            FakeNavigableTile(name = "tile_$it", position = Vector2(it.toFloat(), 0f))
        }
        for (i in 0 until tiles.size - 1) {
            tiles[i].addNeighbor(tiles[i + 1])
        }
        return tiles
    }

    // Центр + кольцо из 6 соседей
    fun createHexWithRing(): List<FakeNavigableTile> {
        val center = FakeNavigableTile("center", Vector2(0f, 0f))
        val ring = (0 until 6).map {
            FakeNavigableTile("ring_$it", Vector2(it.toFloat(), 1f))
        }
        ring.forEach { center.addNeighbor(it) }
        for (i in ring.indices) {
            ring[i].addNeighbor(ring[(i + 1) % 6])
        }
        return listOf(center) + ring
    }

    // Остров — тайл окружён непроходимыми
    fun createIsland(): Triple<FakeNavigableTile, FakeNavigableTile, FakeNavigableTile> {
        val start = FakeNavigableTile("start")
        val wall = FakeNavigableTile("wall")
        val island = FakeNavigableTile("island")
        start.addNeighbor(wall)
        wall.addNeighbor(island)
        // wall непроходим — island недостижим
        return Triple(start, wall, island)
    }

    // Два несвязных кластера
    fun createDisconnected(): Pair<List<FakeNavigableTile>, List<FakeNavigableTile>> {
        val cluster1 = createChain(3)
        val cluster2 = createChain(3).map {
            FakeNavigableTile("isolated_${it.name}")
        }
        return Pair(cluster1, cluster2)
    }
}
