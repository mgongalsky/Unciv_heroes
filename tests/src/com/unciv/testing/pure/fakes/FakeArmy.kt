package com.unciv.testing.pure.fakes

import com.unciv.logic.army.TroopInfo
import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.troop.Troop

class FakeArmy(troops: List<Troop?>) : IArmy {

    private val troops: MutableList<Troop?> = troops.toMutableList()

    constructor(vararg troops: Troop?) : this(troops.toList())

    override fun getAllTroops() = troops.toTypedArray()

    override fun removeTroop(troop: Troop): Boolean {
        val index = troops.indexOfFirst { it?.id == troop.id }
        if (index == -1) return false
        troops[index] = null
        return true
    }
}
