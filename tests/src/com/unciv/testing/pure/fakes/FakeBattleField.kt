package com.unciv.testing.pure.fakes

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.Direction
import com.unciv.pure.domain.battle.IBattleField
import com.unciv.pure.domain.pathfinding.INavigableTile

class FakeBattleField(private val tiles: Iterable<FakeBattleTile> = emptyList()) : IBattleField {
    override fun contains(tile: INavigableTile) = tiles.none() || tile in tiles
    override fun getNeighborTile(tile: INavigableTile, direction: Direction): INavigableTile? = null
    override fun getTileAt(position: Vector2): INavigableTile? =
            tiles.firstOrNull { it.position == position }
}
