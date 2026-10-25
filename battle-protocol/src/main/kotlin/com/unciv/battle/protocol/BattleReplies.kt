package com.unciv.battle.protocol

enum class WireSessionRejection {
    INVALID_SEQUENCE, REQUEST_CONFLICT, OUT_OF_ORDER, STALE_REVISION,
    BATTLE_FINISHED, NOT_YOUR_TURN, WRONG_TROOP
}

enum class WireBattleRejection {
    TOO_FAR, OCCUPIED_BY_ALLY, NOT_IMPLEMENTED, INVALID_TARGET, UNKNOWN_ERROR,
    HEX_OCCUPIED, AI_NO_VALID_MOVE, AI_NO_TARGET, AI_NO_ENEMIES,
    AI_NO_CLOSEST_ENEMY, SHOOTING_BLOCKED_BY_ENEMY
}

data class WireFlanking(
    val flankScore: Int,
    val flankLevel: Int,
    val formedDamageBonusPercent: Int,
    val formationDamageBonusPercent: Int,
    val unformedDamageBonusPercent: Int
)

enum class WireShotType { DIRECT, INDIRECT }
enum class WireIndirectReason { DISTANCE, BLOCKED_LINE, MOVEMENT }

data class WireRangedShot(
    val shotType: WireShotType,
    val reason: WireIndirectReason?,
    val distance: Int,
    val directShotRange: Int,
    val lineBlocked: Boolean?,
    val damageModifierPercent: Int
)

data class WireCommandResult(
    val success: Boolean,
    val movedFrom: WirePoint?,
    val movedTo: WirePoint?,
    val rejection: WireBattleRejection?,
    val isLuck: Boolean,
    val isMorale: Boolean,
    val battleEnded: Boolean,
    val hasFollowUpShot: Boolean,
    val attackFlanking: WireFlanking,
    val retaliationFlanking: WireFlanking,
    val rangedShot: WireRangedShot?
)

enum class WireEventType { TROOP_MOVED, TROOP_ATTACKED, TROOP_SHOT, TURN_ADVANCED, BATTLE_ENDED, TURN_SKIPPED }

/** Tagged payload. JSON codecs must use type, never JVM class names, to select the event shape. */
data class WireBattleEvent(
    val type: WireEventType,
    val troopId: Int? = null,
    val attackerId: Int? = null,
    val defenderId: Int? = null,
    val from: WirePoint? = null,
    val to: WirePoint? = null,
    val defenderRemainingAmount: Int? = null,
    val isLuck: Boolean? = null,
    val isMorale: Boolean? = null,
    val defenderDied: Boolean? = null,
    val attackFlanking: WireFlanking? = null,
    val retaliationFlanking: WireFlanking? = null,
    val rangedShot: WireRangedShot? = null,
    val winner: WireSide? = null
)

data class WireSessionReply(
    val protocolVersion: Int,
    val sequence: Long,
    val revision: Long,
    val rejection: WireSessionRejection?,
    val result: WireCommandResult?,
    val events: List<WireBattleEvent>
)
