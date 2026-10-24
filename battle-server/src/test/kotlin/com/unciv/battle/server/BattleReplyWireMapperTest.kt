package com.unciv.battle.server

import com.unciv.battle.protocol.*
import com.unciv.pure.application.battle.*
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.*
import org.junit.Assert.*
import org.junit.Test

class BattleReplyWireMapperTest {
    @Test
    fun preservesCombatDetailsAndEventOrder() {
        val flank = FlankingCalculator.Result(4, 2, 10, 20, 30)
        val retaliation = FlankingCalculator.Result(3, 1, 5, 10, 15)
        val shot = RangedShotRules.Result(
            RangedShotRules.ShotType.INDIRECT, RangedShotRules.IndirectReason.DISTANCE, 8, 4, null, -50
        )
        val reply = BattleSessionReply(
            7, 12, result = BattleCommandResult(
                true, Point(0, 0), Point(1, 0), isLuck = true, isMorale = true,
                battleEnded = true, hasFollowUpShot = true,
                attackFlanking = flank, retaliationFlanking = retaliation, rangedShot = shot
            ),
            events = listOf(
                com.unciv.pure.application.battle.BattleEvent.TroopMoved(1, Point(0, 0), Point(1, 0), true),
                com.unciv.pure.application.battle.BattleEvent.TroopAttacked(1, 2, 0, true, true, true, flank, retaliation),
                com.unciv.pure.application.battle.BattleEvent.TroopShot(1, 2, 0, true, false, true, shot),
                com.unciv.pure.application.battle.BattleEvent.TurnAdvanced(3),
                com.unciv.pure.application.battle.BattleEvent.TurnSkipped,
                com.unciv.pure.application.battle.BattleEvent.BattleEnded(true)
            )
        )
        val wire = BattleReplyWireMapper.encode(reply)
        assertEquals(1, wire.protocolVersion)
        assertEquals(7L, wire.sequence)
        assertEquals(12L, wire.revision)
        val result = requireNotNull(wire.result)
        assertEquals(WirePoint(1, 0), result.movedTo)
        assertTrue(result.isLuck && result.isMorale && result.battleEnded && result.hasFollowUpShot)
        assertEquals(WireFlanking(4, 2, 10, 20, 30), result.attackFlanking)
        assertEquals(WireFlanking(3, 1, 5, 10, 15), result.retaliationFlanking)
        assertEquals(WireRangedShot(WireShotType.INDIRECT, WireIndirectReason.DISTANCE, 8, 4, null, -50), result.rangedShot)
        assertEquals(WireEventType.values().toList().filter { it != WireEventType.BATTLE_ENDED } + WireEventType.BATTLE_ENDED,
            wire.events.map { it.type })
        assertEquals(result.attackFlanking, wire.events[1].attackFlanking)
        assertEquals(result.retaliationFlanking, wire.events[1].retaliationFlanking)
        assertEquals(result.rangedShot, wire.events[2].rangedShot)
        assertEquals(WireSide.ATTACKER, wire.events.last().winner)
    }

    @Test
    fun keepsBoundaryRejectionSeparateFromRuleRejection() {
        val boundary = BattleReplyWireMapper.encode(BattleSessionReply(1, 0, BattleSessionRejection.NOT_YOUR_TURN))
        assertEquals(WireSessionRejection.NOT_YOUR_TURN, boundary.rejection)
        assertNull(boundary.result)
        val rule = BattleReplyWireMapper.encode(BattleSessionReply(
            2, 1, result = BattleCommandResult(false, rejection = BattleRejection.SHOOTING_BLOCKED_BY_ENEMY)
        ))
        assertNull(rule.rejection)
        assertEquals(WireBattleRejection.SHOOTING_BLOCKED_BY_ENEMY, rule.result!!.rejection)
        assertFalse(rule.result!!.success)
    }

    @Test
    fun clientDecodesServerJsonWithoutLosingResultsOrEvents() {
        val flank = FlankingCalculator.Result(4, 2, 10, 20, 30)
        val shot = RangedShotRules.Result(
            RangedShotRules.ShotType.INDIRECT,
            RangedShotRules.IndirectReason.BLOCKED_LINE,
            3,
            4,
            true,
            -50
        )
        val original = BattleSessionReply(
            5, 9, result = BattleCommandResult(
                true,
                Point(-1, 0),
                Point(0, 0),
                isLuck = true,
                isMorale = true,
                hasFollowUpShot = true,
                attackFlanking = flank,
                retaliationFlanking = flank,
                rangedShot = shot
            ), events = listOf(
                com.unciv.pure.application.battle.BattleEvent.TroopMoved(
                    1,
                    Point(-1, 0),
                    Point(0, 0),
                    true
                ),
                com.unciv.pure.application.battle.BattleEvent.TroopAttacked(
                    1,
                    2,
                    4,
                    true,
                    false,
                    false,
                    flank,
                    flank
                ),
                com.unciv.pure.application.battle.BattleEvent.TroopShot(
                    1,
                    2,
                    0,
                    false,
                    true,
                    true,
                    shot
                ),
                com.unciv.pure.application.battle.BattleEvent.TurnAdvanced(3),
                com.unciv.pure.application.battle.BattleEvent.TurnSkipped,
                com.unciv.pure.application.battle.BattleEvent.BattleEnded(null)
            )
        )
        val cases = listOf(
            original,
            BattleSessionReply(6, 9, BattleSessionRejection.STALE_REVISION),
            BattleSessionReply(
                7,
                10,
                result = BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
            )
        )
        for (reply in cases) {
            val json = BattleHttpJson.encode(BattleReplyWireMapper.encode(reply))
            assertEquals(
                reply,
                com.unciv.infrastructure.battle.network.BattleReplyJson.decode(json)
            )
        }
    }
}
