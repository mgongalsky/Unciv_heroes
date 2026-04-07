package com.unciv.testing.pure.fakes

import com.unciv.logic.army.TroopInfo
import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.troop.Troop

class FakeArmy(private val troops: List<Troop?>) : IArmy {
    override fun getAllTroops() = troops.toTypedArray()
}
