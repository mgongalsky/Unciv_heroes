package com.unciv.battle.server

import com.unciv.battle.protocol.*
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.session.BattleSessionReply
import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.RangedShotRules

object BattleReplyWireMapper {
    private fun Point.wire() = WirePoint(x, y)
    private fun FlankingCalculator.Result.wire() = WireFlanking(
        flankScore, flankLevel, formedDamageBonusPercent, formationDamageBonusPercent, unformedDamageBonusPercent
    )
    private fun RangedShotRules.Result.wire() = WireRangedShot(
        WireShotType.valueOf(shotType.name), reason?.let { WireIndirectReason.valueOf(it.name) },
        distance, directShotRange, lineBlocked, damageModifierPercent
    )
    private fun BattleCommandResult.wire() = WireCommandResult(
        success, movedFrom?.wire(), movedTo?.wire(), rejection?.let { WireBattleRejection.valueOf(it.name) },
        isLuck, isMorale, battleEnded, hasFollowUpShot, attackFlanking.wire(), retaliationFlanking.wire(), rangedShot?.wire()
    )

    fun encode(reply: BattleSessionReply) = WireSessionReply(
        BattleProtocol.VERSION, reply.sequence, reply.revision,
        reply.rejection?.let { WireSessionRejection.valueOf(it.name) }, reply.result?.wire(),
        reply.events.map(::event)
    )

    private fun event(event: BattleEvent): WireBattleEvent = when (event) {
        is BattleEvent.TroopMoved -> WireBattleEvent(
            WireEventType.TROOP_MOVED, troopId = event.troopId,
            from = event.from.wire(), to = event.to.wire(), isMorale = event.isMorale
        )
        is BattleEvent.TroopAttacked -> WireBattleEvent(
            WireEventType.TROOP_ATTACKED, attackerId = event.attackerId, defenderId = event.defenderId,
            defenderRemainingAmount = event.defenderRemainingAmount, isLuck = event.isLuck,
            isMorale = event.isMorale, defenderDied = event.defenderDied,
            attackFlanking = event.attackFlanking.wire(), retaliationFlanking = event.retaliationFlanking.wire()
        )
        is BattleEvent.TroopShot -> WireBattleEvent(
            WireEventType.TROOP_SHOT, attackerId = event.attackerId, defenderId = event.defenderId,
            defenderRemainingAmount = event.defenderRemainingAmount, isLuck = event.isLuck,
            isMorale = event.isMorale, defenderDied = event.defenderDied, rangedShot = event.rangedShot?.wire()
        )
        is BattleEvent.TurnAdvanced -> WireBattleEvent(WireEventType.TURN_ADVANCED, troopId = event.nextTroopId)
        is BattleEvent.BattleEnded -> WireBattleEvent(
            WireEventType.BATTLE_ENDED,
            winner = event.winnerIsAttacker?.let { if (it) WireSide.ATTACKER else WireSide.DEFENDER }
        )
        BattleEvent.TurnSkipped -> WireBattleEvent(WireEventType.TURN_SKIPPED)
    }
}
