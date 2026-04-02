package com.unciv.pure.domain.pathfinding

import com.badlogic.gdx.math.Vector2

interface INavigableTile {
    val neighbors: Sequence<INavigableTile>
    val position: Vector2
}
