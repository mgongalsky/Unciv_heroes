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
        winnerIsAttacker: Boolean?,
        unitPrice: (String) -> Int? = { null }
    ): BattleReport {
        val score = if (winnerIsAttacker == null) {
            com.unciv.pure.domain.battle.BattleScore(0, 0)
        } else {
            val initial = if (winnerIsAttacker) initialState.attackers else initialState.defenders
            val army = if (winnerIsAttacker) attackerArmy else defenderArmy
            val remaining =
                army.getAllTroops().filterNotNull().associate { it.id to it.currentAmount }
            CalculateBattleScoreUseCase.execute(initial.map {
                CalculateBattleScoreUseCase.Stack(it.unitName, it.amount, remaining[it.id] ?: 0)
            }, unitPrice)
        }
        return BattleReport(
            winner = when (winnerIsAttacker) {
                true -> BattleSide.ATTACKER
                false -> BattleSide.DEFENDER
                null -> null
            },
            attackerLosses = losses(initialState.attackers, attackerArmy),
            defenderLosses = losses(initialState.defenders, defenderArmy),
            winnerScore = score
        )
    }

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
