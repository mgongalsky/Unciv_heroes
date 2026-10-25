package com.unciv.pure.domain.battle

/** Target-specific bonus applied to a hit, never stored in the retaliation reserve. */
object AntiCavalryDamageRules {
    fun damage(baseDamage: Int, bonusPercent: Int, targetIsCavalry: Boolean): Int {
        require(baseDamage >= 0)
        require(bonusPercent >= 0)
        if (!targetIsCavalry || bonusPercent == 0) return baseDamage
        return (baseDamage.toLong() * (100L + bonusPercent) / 100L)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
