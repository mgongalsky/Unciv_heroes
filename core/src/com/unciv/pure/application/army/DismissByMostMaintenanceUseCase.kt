package com.unciv.pure.application.army

import com.unciv.pure.domain.army.IArmy

object DismissByMostMaintenanceUseCase {
    fun execute(army: IArmy) {
        val troopToDismiss = army.getAllTroops().maxBy { it?.amount ?: 0 } ?: return
        army.removeTroop(troopToDismiss)
    }
}
