package com.unciv.infrastructure.battle.network

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.unciv.battle.protocol.BattleProtocol
import com.unciv.battle.protocol.WireEventType
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.application.battle.session.BattleSessionRejection
import com.unciv.pure.application.battle.session.BattleSessionReply
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.RangedShotRules

/** Explicit decoding of values only; no reflection or class names from the response. */
object BattleReplyJson {
    fun decode(text: String): BattleSessionReply = decode(JsonReader().parse(text))

    internal fun decode(root: JsonValue): BattleSessionReply {
        require(root.isObject)
        BattleProtocol.requireVersion(root.int("protocolVersion"))
        val events = root.required("events").also { require(it.isArray) }
        return BattleSessionReply(
            root.long("sequence"), root.long("revision"),
            root.optional("rejection")?.let { BattleSessionRejection.valueOf(it.text()) },
            root.optional("result")?.let(::result), events.map(::event)
        )
    }

    internal fun JsonValue.optional(name: String): JsonValue? = get(name)?.takeUnless { it.isNull }
    internal fun JsonValue.required(name: String): JsonValue = requireNotNull(optional(name)) { "Missing $name" }
    internal fun JsonValue.text(): String { require(isString); return asString() }
    internal fun JsonValue.long(name: String): Long = required(name).let { require(it.isLong); it.asLong() }
    internal fun JsonValue.int(name: String): Int = long(name).also {
        require(it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
    }.toInt()
    internal fun JsonValue.bool(name: String): Boolean = required(name).let { require(it.isBoolean); it.asBoolean() }
    private fun point(value: JsonValue): Point {
        require(value.isObject)
        return Point(value.int("x"), value.int("y"))
    }
    private fun flank(value: JsonValue): FlankingCalculator.Result {
        require(value.isObject)
        return FlankingCalculator.Result(
            value.int("flankScore"), value.int("flankLevel"), value.int("formedDamageBonusPercent"),
            value.int("formationDamageBonusPercent"), value.int("unformedDamageBonusPercent")
        )
    }
    private fun shot(value: JsonValue): RangedShotRules.Result {
        require(value.isObject)
        return RangedShotRules.Result(
            RangedShotRules.ShotType.valueOf(value.required("shotType").text()),
            value.optional("reason")?.let { RangedShotRules.IndirectReason.valueOf(it.text()) },
            value.int("distance"), value.int("directShotRange"),
            value.optional("lineBlocked")?.let { require(it.isBoolean); it.asBoolean() },
            value.int("damageModifierPercent")
        )
    }
    private fun result(value: JsonValue): BattleCommandResult {
        require(value.isObject)
        return BattleCommandResult(
            success = value.bool("success"), movedFrom = value.optional("movedFrom")?.let(::point),
            movedTo = value.optional("movedTo")?.let(::point),
            rejection = value.optional("rejection")?.let { BattleRejection.valueOf(it.text()) },
            isLuck = value.bool("isLuck"), isMorale = value.bool("isMorale"),
            battleEnded = value.bool("battleEnded"), hasFollowUpShot = value.bool("hasFollowUpShot"),
            attackFlanking = flank(value.required("attackFlanking")),
            retaliationFlanking = flank(value.required("retaliationFlanking")),
            rangedShot = value.optional("rangedShot")?.let(::shot)
        )
    }
    private fun event(value: JsonValue): BattleEvent {
        require(value.isObject)
        return when (WireEventType.valueOf(value.required("type").text())) {
            WireEventType.TROOP_MOVED -> BattleEvent.TroopMoved(
                value.int("troopId"), point(value.required("from")), point(value.required("to")), value.bool("isMorale")
            )
            WireEventType.TROOP_ATTACKED -> BattleEvent.TroopAttacked(
                value.int("attackerId"), value.int("defenderId"), value.int("defenderRemainingAmount"),
                value.bool("isLuck"), value.bool("isMorale"), value.bool("defenderDied"),
                flank(value.required("attackFlanking")), flank(value.required("retaliationFlanking"))
            )
            WireEventType.TROOP_SHOT -> BattleEvent.TroopShot(
                value.int("attackerId"), value.int("defenderId"), value.int("defenderRemainingAmount"),
                value.bool("isLuck"), value.bool("isMorale"), value.bool("defenderDied"),
                value.optional("rangedShot")?.let(::shot)
            )
            WireEventType.TURN_ADVANCED -> BattleEvent.TurnAdvanced(value.int("troopId"))
            WireEventType.TURN_SKIPPED -> BattleEvent.TurnSkipped
            WireEventType.BATTLE_ENDED -> BattleEvent.BattleEnded(value.optional("winner")?.let {
                BattleSide.valueOf(it.text()) == BattleSide.ATTACKER
            })
        }
    }
}
