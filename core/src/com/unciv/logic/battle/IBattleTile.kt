package com.unciv.logic.battle


// That's the seam for sequence INavigableTile -> TileInfo to avoid TileInfo in domain
import com.unciv.logic.army.TroopInfo
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop

interface IBattleTile : INavigableTile {
    fun getTroop(): Troop?
    fun receiveTroop(troop: Troop)  // ← здесь полиморфизм
    fun clearTroop()
    fun toPoint() = Point(position.x.toInt(), position.y.toInt())
}
