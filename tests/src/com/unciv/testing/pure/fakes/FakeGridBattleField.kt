package com.unciv.testing.pure.fakes

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.Direction
import com.unciv.pure.domain.battle.IBattleField
import com.unciv.pure.domain.pathfinding.INavigableTile

class FakeGridBattleField(
    private val grid: Array<Array<FakeBattleTile>>
) : IBattleField {

    private val allTiles = grid.flatten()

    override fun contains(tile: INavigableTile) = allTiles.contains(tile)

    override fun getTileAt(position: Vector2): INavigableTile? =
            allTiles.firstOrNull { it.position == position }

    override fun getNeighborTile(tile: INavigableTile, direction: Direction): INavigableTile? = null
}
