package com.unciv.testing.pure.fakes

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.pathfinding.INavigableTile

class FakeBattleTile(
    override val position: Vector2,
    private var troop: TroopInfo? = null
) : IBattleTile {

    private val neighborTiles: MutableList<IBattleTile> = mutableListOf()

    override val neighbors: Sequence<INavigableTile>
        get() = neighborTiles.asSequence()

    override fun getTroop(): TroopInfo? = troop

    override fun receiveTroop(troop: TroopInfo) { this.troop = troop }
    override fun clearTroop() { troop = null }

    fun setTroop(t: TroopInfo?) { troop = t }
    fun addNeighbor(tile: FakeBattleTile) { neighborTiles.add(tile) }
}
