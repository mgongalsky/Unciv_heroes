package com.unciv.pure.domain.army

import com.unciv.pure.domain.troop.Troop

class Army(
    val maxSlots: Int,
    private val troops: Array<Troop?> = arrayOfNulls(maxSlots)
) {
    fun contains(troop: Troop): Boolean = troops.any { it?.id == troop.id }
    fun getAllTroops(): Array<Troop?> = troops
    //fun addTroop(troop: Troop): Boolean
    //fun removeTroop(troop: Troop): Boolean
    fun isEmpty(): Boolean = troops.all { it == null || it.currentAmount <= 0 }
}
