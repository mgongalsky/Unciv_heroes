package com.unciv.testing.pure.fakes

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop

class FakeBattleTile(
    override val position: Vector2,
    private var troop: Troop? = null,
    var impassable: Boolean = false
) : IBattleTile {
    private val neighborTiles = mutableListOf<IBattleTile>()
    override val neighbors: Sequence<INavigableTile> get() = neighborTiles.asSequence()
    override fun isImpassible(): Boolean = impassable
    override fun getTroop(): Troop? = troop
    override fun receiveTroop(troop: Troop) {
        this.troop = troop
    }

    override fun clearTroop() {
        troop = null
    }

    fun setTroop(t: Troop?) {
        troop = t
    }

    fun addNeighbor(tile: FakeBattleTile) {
        neighborTiles.add(tile)
    }
}
