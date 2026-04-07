package com.unciv.testing.pure.fakes

import com.unciv.logic.army.TroopInfo
import com.unciv.pure.domain.army.IArmy

class FakeArmy(private val troops: List<TroopInfo?>) : IArmy {
    override fun getAllTroops() = troops.toTypedArray()
}
