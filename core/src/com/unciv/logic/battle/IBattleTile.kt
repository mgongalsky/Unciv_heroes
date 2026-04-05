package com.unciv.logic.battle


// That's the seam for sequence INavigableTile -> TileInfo to avoid TileInfo in domain
import com.unciv.logic.army.TroopInfo
import com.unciv.pure.domain.pathfinding.INavigableTile

interface IBattleTile : INavigableTile {
    fun getTroop(): TroopInfo?
    fun receiveTroop(troop: TroopInfo)  // ← здесь полиморфизм
    fun clearTroop()
}
