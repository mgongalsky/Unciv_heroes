package com.unciv.pure.application

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.HeroAction
import com.unciv.logic.civilization.NotificationIcon

object HeroFoodWarningUseCase {
    fun execute(
        currentFood: Float,
        dailyConsumption: Float,
        unitDisplayName: String,
        unitName: String,
        civInfo: CivilizationInfo,
        position: Vector2
    ) {
        if (dailyConsumption <= 0) return
        val daysRemaining = (currentFood / dailyConsumption).toInt()
        if (daysRemaining > 3) return
        val warningText = when (daysRemaining) {
            0 -> "[$unitDisplayName] has no food left!"
            1 -> "[$unitDisplayName] has food for only 1 turn!"
            else -> "[$unitDisplayName] has food for only $daysRemaining turns!"
        }
        civInfo.addNotification(warningText, HeroAction(position), unitName, NotificationIcon.Death)
    }
}
