package com.unciv.logic.battle

import com.unciv.pure.application.battle.BattleEvent

object LegacyBattleEventAdapter {
    fun map(event: BattleEvent): com.unciv.pure.domain.battle.BattleEvent = when (event) {
        is BattleEvent.TroopMoved -> com.unciv.pure.domain.battle.BattleEvent.TroopMoved(
            event.troopId,
            event.from,
            event.to,
            event.isMorale
        )

        is BattleEvent.TroopAttacked -> com.unciv.pure.domain.battle.BattleEvent.TroopAttacked(
            event.attackerId,
            event.defenderId,
            event.defenderRemainingAmount,
            event.isLuck,
            event.isMorale,
            event.defenderDied
        )

        is BattleEvent.TroopShot -> com.unciv.pure.domain.battle.BattleEvent.TroopShot(
            event.attackerId,
            event.defenderId,
            event.defenderRemainingAmount,
            event.isLuck,
            event.isMorale,
            event.defenderDied
        )

        is BattleEvent.TurnAdvanced ->
            com.unciv.pure.domain.battle.BattleEvent.TurnAdvanced(event.nextTroopId)

        is BattleEvent.BattleEnded ->
            com.unciv.pure.domain.battle.BattleEvent.BattleEnded(event.winnerIsAttacker)

        BattleEvent.TurnSkipped -> com.unciv.pure.domain.battle.BattleEvent.TurnSkipped
    }
}
