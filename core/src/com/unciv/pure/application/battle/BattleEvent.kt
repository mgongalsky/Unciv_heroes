package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.Point

sealed class BattleEvent {
    data class TroopMoved(val troopId: Int, val from: Point, val to: Point, val isMorale: Boolean) :
        BattleEvent()

    data class TroopAttacked(
        val attackerId: Int,
        val defenderId: Int,
        val defenderRemainingAmount: Int,
        val isLuck: Boolean,
        val isMorale: Boolean,
        val defenderDied: Boolean,
        val attackFlanking: FlankingCalculator.Result = FlankingCalculator.Result(),
        val retaliationFlanking: FlankingCalculator.Result = FlankingCalculator.Result()
    ) : BattleEvent()

    data class TroopShot(
        val attackerId: Int,
        val defenderId: Int,
        val defenderRemainingAmount: Int,
        val isLuck: Boolean,
        val isMorale: Boolean,
        val defenderDied: Boolean
    ) : BattleEvent()

    data class TurnAdvanced(val nextTroopId: Int) : BattleEvent()
    data class BattleEnded(val winnerIsAttacker: Boolean?) : BattleEvent()
    object TurnSkipped : BattleEvent()
}
