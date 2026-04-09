package com.unciv.pure.application.army

import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory

object AddUnitsUseCase {
    fun execute(army: IArmy, unitName: String, amount: Int, source: ITroopDefinitionSource): Boolean {
        if (amount <= 0) return false
        for (slot in army.getAllTroops()) {
            if (slot?.unitName == unitName) {
                slot.currentAmount += amount
                return true
            }
        }
        val troops = army.getAllTroops()
        val emptySlotIndex = troops.indexOfFirst { it == null }
        if (emptySlotIndex != -1) {
            army.setTroopAt(emptySlotIndex, TroopFactory.create(unitName, amount, source))
            return true
        }
        return false
    }
}
