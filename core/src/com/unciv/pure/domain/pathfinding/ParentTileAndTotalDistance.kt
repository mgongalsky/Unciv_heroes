package com.unciv.pure.domain.pathfinding

import com.unciv.pure.application.pathfinding.INavigableTile

class ParentTileAndTotalDistance<T : INavigableTile>(
    val parentTile: T,
    val totalDistance: Float
)
