package com.unciv.pure.domain.army

import com.unciv.pure.domain.troop.Troop

interface IArmy {
    fun getAllTroops(): Array<Troop?>
    fun removeTroop(troop: Troop): Boolean
    fun setTroopAt(index: Int, troop: Troop?)

    fun contains(troop: Troop): Boolean = getAllTroops().any { it?.id == troop.id }
    fun getTroopAt(index: Int): Troop? = getAllTroops().getOrNull(index)

    /** Combat inputs only; no dependency on a world-map hero or civilization. */
    fun getBattleMorale(): Int = 0
    fun getBattleLuck(): Int = 1

    /** Post-battle recovery, invoked separately from combat command execution. */
    fun finishBattle() {
        getAllTroops().forEachIndexed { index, troop ->
            if (troop != null) {
                if (troop.currentAmount <= 0) {
                    setTroopAt(index, null)
                } else {
                    troop.amount = troop.currentAmount
                    troop.currentHealth = troop.maxHealth
                    troop.resetFormation()
                }
            }
        }
    }
}
