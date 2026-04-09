package com.unciv.pure.domain.army

import com.unciv.pure.domain.troop.Troop

class Army(
    val maxSlots: Int,
    private val troops: Array<Troop?> = arrayOfNulls(maxSlots)
) : IArmy {

    fun contains(troop: Troop): Boolean =
            troops.any { it?.id == troop.id }

    override fun getAllTroops(): Array<Troop?> = troops

    fun getTroopAt(index: Int): Troop? = troops.getOrNull(index)

    fun addTroop(troop: Troop): Boolean {
        val emptySlotIndex = troops.indexOfFirst { it == null }
        return if (emptySlotIndex != -1) {
            troops[emptySlotIndex] = troop
            true
        } else {
            false
        }
    }

    override fun removeTroop(troop: Troop): Boolean {
        val index = troops.indexOfFirst { it?.id == troop.id }
        if (index != -1) {
            troops[index] = null
            return true
        }
        return false
    }

    fun isEmpty(): Boolean = troops.all { it == null || it.currentAmount <= 0 }

    fun aliveTroops(): List<Troop> = troops.filterNotNull().filter { it.currentAmount > 0 }
}
