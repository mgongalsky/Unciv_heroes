package com.unciv.pure.application.battle

import com.unciv.pure.domain.army.IArmy

object GetBattleResultUseCase {
    data class Result(val winnerIsAttacker: Boolean)

    fun execute(attackerArmy: IArmy, defenderArmy: IArmy): Result? {
        val attackersAlive = attackerArmy.getAllTroops().filterNotNull().any { it.currentAmount > 0 }
        val defendersAlive = defenderArmy.getAllTroops().filterNotNull().any { it.currentAmount > 0 }

        return when {
            attackersAlive && !defendersAlive -> Result(winnerIsAttacker = true)
            defendersAlive && !attackersAlive -> Result(winnerIsAttacker = false)
            else -> null
        }
    }
}
