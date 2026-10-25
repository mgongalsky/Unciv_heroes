package com.unciv.pure.domain.battle

/** Deterministic shot classification, independent of targeting, UI and randomness. */
object RangedShotRules {
    enum class ShotType { DIRECT, INDIRECT }
    enum class IndirectReason { DISTANCE, BLOCKED_LINE, MOVEMENT }

    /** Only intermediate cells belong here; the shooter and target are excluded by geometry. */
    data class Blocker(
        val isAlive: Boolean = true,
        val isCombatUnit: Boolean = true,
        val isMelee: Boolean = true,
        val elevation: Int? = null
    )

    data class Result(
        val shotType: ShotType,
        val reason: IndirectReason?,
        val distance: Int,
        val directShotRange: Int,
        /** Null means the line was irrelevant because the target was beyond direct range. */
        val lineBlocked: Boolean?,
        val damageModifierPercent: Int
    )

    /** Missing capabilities leave legacy attacks unchanged. Distance is measured in hex steps. */
    fun evaluate(
        isRanged: Boolean,
        profile: RangedShotProfile?,
        distance: Int,
        blockers: Iterable<Blocker> = emptyList(),
        shooterElevation: Int? = null
    ): Result? {
        if (!isRanged || profile == null) return null
        require(distance > 0) { "A shot requires distinct attacker and target cells" }
        require(profile.directShotRange >= 0)
        require(profile.indirectPenaltyPercent in 0..100)
        if (distance > profile.directShotRange) return Result(
            ShotType.INDIRECT, IndirectReason.DISTANCE, distance,
            profile.directShotRange, null, -profile.indirectPenaltyPercent
        )
        val blocked = blockers.any { blocker ->
            blocker.isAlive && blocker.isCombatUnit &&
                !(blocker.isMelee && shooterElevation != null &&
                    blocker.elevation != null && shooterElevation > blocker.elevation)
        }
        return Result(
            if (blocked) ShotType.INDIRECT else ShotType.DIRECT,
            if (blocked) IndirectReason.BLOCKED_LINE else null,
            distance, profile.directShotRange, blocked,
            if (blocked) -profile.indirectPenaltyPercent else 0
        )
    }

    /** Apply once, before formation absorption; integer damage rounds down. */
    fun damage(baseDamage: Int, shot: Result?): Int {
        require(baseDamage >= 0)
        val modifier = shot?.damageModifierPercent ?: 0
        require(modifier in -100..0)
        return (baseDamage.toLong() * (100 + modifier) / 100).toInt()
    }

    /** Marks a clear follow-up shot as indirect. Movement damage is applied separately,
     * so this classification preserves the existing modifier and any line or distance penalty.
     */
    fun afterMovement(shot: Result?, afterMovement: Boolean): Result? =
            if (afterMovement && shot?.shotType == ShotType.DIRECT)
                shot.copy(shotType = ShotType.INDIRECT, reason = IndirectReason.MOVEMENT)
            else shot
}
