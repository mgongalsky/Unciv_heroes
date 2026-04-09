package com.unciv.pure.domain.army

import com.unciv.logic.army.TroopInfo
import com.unciv.pure.domain.troop.Troop

interface IArmy {
    fun getAllTroops(): Array<Troop?>
    fun removeTroop(troop: Troop): Boolean
    fun setTroopAt(index: Int, troop: Troop?)
}
