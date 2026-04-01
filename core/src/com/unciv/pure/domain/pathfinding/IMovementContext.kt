package com.unciv.pure.domain.pathfinding

import com.unciv.logic.map.TileInfo

// pure/domain/pathfinding/IMovementContext.kt
interface IMovementContext {
    fun canPassThrough(tile: TileInfo): Boolean
    fun getMovementCost(from: TileInfo, to: TileInfo): Float
}
