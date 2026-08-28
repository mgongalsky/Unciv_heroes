package com.unciv.pure.domain.pathfinding

import com.unciv.pure.domain.pathfinding.INavigableTile

interface IMovementContext {
    fun canPassThrough(tile: INavigableTile): Boolean
    fun getMovementCost(from: INavigableTile, to: INavigableTile): Float
    fun hasExplored(tile: INavigableTile): Boolean
    fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?): Boolean

    /** Зона контроля выражается запретом выхода, а не стоимостью: достижимость читается как ключи результата, поэтому дорогой переход клетку не отсекает. */
    fun canLeaveTile(tile: INavigableTile): Boolean = true
}
