package com.unciv.pure.domain.battle

sealed class BattleEvent {
    data class TroopMoved(
        val troopId: Int,
        val from: Point,
        val to: Point,
        val isMorale: Boolean
    ) : BattleEvent()

    data class TroopAttacked(
        val attackerId: Int,
        val defenderId: Int,
        val defenderRemainingAmount: Int,
        val isLuck: Boolean,
        val isMorale: Boolean,
        val defenderDied: Boolean
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
    data class BattleEnded(val winnerIsAttacker: Boolean) : BattleEvent()
    object TurnSkipped : BattleEvent()
}
