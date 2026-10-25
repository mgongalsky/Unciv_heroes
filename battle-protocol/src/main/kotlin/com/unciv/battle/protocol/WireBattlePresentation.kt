package com.unciv.battle.protocol

/** Explicit constructor values prevent prototype serialization from omitting empty lists. */
data class WireBattlePresentation(
    val tiles: List<WireTilePresentation>,
    val movementRanges: List<WireTroopMovementPresentation>,
    val attacks: List<WireAttackPresentation>
)

enum class WireControlStrength { NONE, NORMAL, REINFORCED }
enum class WireMovementStyle { HIDDEN, SAFE, FORMATION_PENALTY }

data class WireTilePresentation(
    val position: WirePoint,
    val terrainAlpha: Float,
    val control: WireControlStrength
)

data class WireMovementTilePresentation(
    val position: WirePoint,
    val style: WireMovementStyle
)

data class WireTroopMovementPresentation(
    val troopId: Int,
    val tiles: List<WireMovementTilePresentation>
)

data class WireAttackPresentation(
    val command: WireAvailableCommand,
    val preview: WireAttackPreview
)

data class WireAttackPreview(
    val attack: WirePreviewHit,
    val retaliation: WirePreviewHit? = null,
    val retaliationBudget: Int? = null,
    val remainingRetaliationBudget: Int? = null,
    val usesStoredRetaliation: Boolean = false,
    val isRanged: Boolean = false,
    val rangedShot: WireRangedShot? = null
)

data class WirePreviewHit(
    val preparation: WirePreparedDamage?,
    val flanking: WireFlanking,
    val formationBefore: Int,
    val formationReductionPercent: Int,
    val remainingFormation: Int,
    val result: WirePreviewDamageResult,
    val antiCavalryDamageBonusPercent: Int = 0
)

data class WirePreparedDamage(
    val amount: Int,
    val damagePerSoldier: Int,
    val luckMultiplier: Int,
    val afterLuck: Int,
    val supportBonusPercent: Int,
    val afterSupport: Int,
    val penaltyPercent: Int,
    val incomingDamage: Int
)

data class WirePreviewDamageResult(
    val damageDealt: Int,
    val perished: Int,
    val remainingAmount: Int,
    val remainingHealth: Int,
    val isLuck: Boolean
)
