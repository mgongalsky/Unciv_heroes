package com.unciv.testing.pure.fakes

import com.badlogic.gdx.math.Vector2

object FakeBattleFieldBuilder {

    fun buildGrid(width: Int, height: Int): Array<Array<FakeBattleTile>> {
        val grid = Array(height) { y ->
            Array(width) { x ->
                FakeBattleTile(Vector2(x.toFloat(), y.toFloat()))
            }
        }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val tile = grid[y][x]
                if (x > 0)          tile.addNeighbor(grid[y][x - 1])
                if (x < width - 1)  tile.addNeighbor(grid[y][x + 1])
                if (y > 0)          tile.addNeighbor(grid[y - 1][x])
                if (y < height - 1) tile.addNeighbor(grid[y + 1][x])
            }
        }
        return grid
    }

    fun tileAt(grid: Array<Array<FakeBattleTile>>, x: Int, y: Int): FakeBattleTile = grid[y][x]
}
