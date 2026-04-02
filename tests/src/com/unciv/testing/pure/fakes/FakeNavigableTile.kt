package com.unciv.testing.pure.fakes

import com.badlogic.gdx.math.Vector2
import com.unciv.pure.domain.pathfinding.INavigableTile

class FakeNavigableTile(
    val name: String = "",
    override val position: Vector2 = Vector2.Zero,
    private val neighborList: MutableList<FakeNavigableTile> = mutableListOf()
) : INavigableTile {

    override val neighbors: Sequence<INavigableTile>
        get() = neighborList.asSequence()

    fun addNeighbor(tile: FakeNavigableTile) {
        if (!neighborList.contains(tile)) neighborList.add(tile)
        if (!tile.neighborList.contains(this)) tile.neighborList.add(this)
    }

    override fun toString() = name
}
