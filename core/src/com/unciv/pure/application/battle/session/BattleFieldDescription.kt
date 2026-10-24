package com.unciv.pure.application.battle.session

import com.unciv.pure.domain.battle.Point

/** Terrain values for presentation; contains no mutable battlefield tiles or occupants. */
data class BattleFieldDescription(
    val width: Int,
    val height: Int,
    val cells: List<BattleFieldCell>
)

data class BattleFieldCell(
    val position: Point,
    val baseTerrain: String,
    val features: List<String>,
    val temperature: Double,
    val humidity: Double,
    val impassable: Boolean
)
