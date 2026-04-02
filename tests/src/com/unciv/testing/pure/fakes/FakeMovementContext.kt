package com.unciv.testing.pure.fakes

import com.unciv.pure.domain.pathfinding.IMovementContext
import com.unciv.pure.domain.pathfinding.INavigableTile

open class FakeMovementContext(
    private val cost: Float = 1f,
    private val passable: Boolean = true,
    private val explored: Boolean = true
) : IMovementContext {
    override fun canPassThrough(tile: INavigableTile) = passable
    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = cost
    override fun hasExplored(tile: INavigableTile) = explored
    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) = false
}
