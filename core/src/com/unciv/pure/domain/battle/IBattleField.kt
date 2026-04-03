package com.unciv.pure.domain.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.Direction
import com.unciv.pure.domain.pathfinding.INavigableTile

interface IBattleField {
    fun contains(tile: INavigableTile): Boolean
    fun getNeighborTile(tile: INavigableTile, direction: Direction): INavigableTile?
    fun getTileAt(position: Vector2): INavigableTile?
}
