package com.unciv.pure.application.army

import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory

object FillArmyUseCase {
    fun execute(army: IArmy, unitName: String, totalCount: Int, source: ITroopDefinitionSource) {
        if (totalCount <= 0 || unitName.isBlank()) {
            throw IllegalArgumentException("Invalid unit name or total count")
        }
        val troops = army.getAllTroops()
        for (i in troops.indices) {
            army.setTroopAt(i, null)
        }
        val troopsPerSlot = totalCount / troops.size
        val remainder = totalCount % troops.size
        for (i in troops.indices) {
            val countForThisSlot = troopsPerSlot + if (i < remainder) 1 else 0
            if (countForThisSlot > 0) {
                army.setTroopAt(i, TroopFactory.create(unitName, countForThisSlot, source))
            }
        }
    }
}
