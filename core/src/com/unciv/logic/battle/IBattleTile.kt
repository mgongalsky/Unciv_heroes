package com.unciv.logic.battle

import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop

interface IBattleTile : INavigableTile {
    /** Plain tiles are passable; terrain-backed tiles supply their terrain rule. */
    fun isImpassible(): Boolean = false
    fun getTroop(): Troop?
    fun receiveTroop(troop: Troop)
    fun clearTroop()
    fun toPoint() = Point(position.x.toInt(), position.y.toInt())
}
