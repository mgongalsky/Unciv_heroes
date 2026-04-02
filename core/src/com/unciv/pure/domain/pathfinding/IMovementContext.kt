package com.unciv.pure.domain.pathfinding

import com.unciv.pure.domain.pathfinding.INavigableTile

interface IMovementContext {
    fun canPassThrough(tile: INavigableTile): Boolean
    fun getMovementCost(from: INavigableTile, to: INavigableTile): Float
    fun hasExplored(tile: INavigableTile): Boolean
    fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?): Boolean
}
