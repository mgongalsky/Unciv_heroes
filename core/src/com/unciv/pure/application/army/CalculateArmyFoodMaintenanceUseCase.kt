package com.unciv.pure.application.army

import com.unciv.logic.army.ArmyInfo

object CalculateArmyFoodMaintenanceUseCase {
    fun execute(army: ArmyInfo, isInCity: Boolean): Float {
        var foodMaintenance = 0f
        army.getAllTroops().filterNotNull().forEach {
            if (!isInCity || !it.isSelfFeeding)
                foodMaintenance += it.amount.toFloat() / 30f
        }
        return foodMaintenance
    }
}
