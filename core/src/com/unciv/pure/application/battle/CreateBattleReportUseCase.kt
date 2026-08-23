package com.unciv.pure.application.battle

import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.battle.BattleLoss
import com.unciv.pure.domain.battle.BattleReport
import com.unciv.pure.domain.battle.BattleSide

object CreateBattleReportUseCase {
    data class TroopSnapshot(
        val id: Int,
        val unitName: String,
        val amount: Int
    )

    data class InitialState(
        val attackers: List<TroopSnapshot>,
        val defenders: List<TroopSnapshot>
    )

    fun capture(attackerArmy: IArmy, defenderArmy: IArmy): InitialState = InitialState(
        attackers = attackerArmy.snapshots(),
        defenders = defenderArmy.snapshots()
    )

    fun execute(
        initialState: InitialState,
        attackerArmy: IArmy,
        defenderArmy: IArmy,
        winnerIsAttacker: Boolean?
    ): BattleReport = BattleReport(
        winner = when (winnerIsAttacker) {
            true -> BattleSide.ATTACKER
            false -> BattleSide.DEFENDER
            null -> null
        },
        attackerLosses = losses(initialState.attackers, attackerArmy),
        defenderLosses = losses(initialState.defenders, defenderArmy)
    )

    private fun losses(initial: List<TroopSnapshot>, currentArmy: IArmy): List<BattleLoss> {
        val remainingById = currentArmy.getAllTroops()
            .filterNotNull()
            .associate { it.id to it.currentAmount }

        return initial.mapNotNull { troop ->
            val lost = (troop.amount - (remainingById[troop.id] ?: 0)).coerceAtLeast(0)
            if (lost == 0) null else BattleLoss(troop.id, troop.unitName, lost)
        }
    }

    private fun IArmy.snapshots(): List<TroopSnapshot> =
        getAllTroops().filterNotNull().map {
            TroopSnapshot(it.id, it.unitName, it.amount)
        }
}
