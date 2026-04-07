package com.unciv.pure.domain.army

import com.unciv.logic.army.TroopInfo

interface IArmy {
    fun getAllTroops(): Array<TroopInfo?>
}
