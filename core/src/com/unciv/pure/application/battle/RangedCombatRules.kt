package com.unciv.pure.application.battle

/** Shared rules for ranged troops, independent of battlefield and presentation objects. */
object RangedCombatRules {
    /** Apply once to total damage after luck and before formation absorption. */
    fun meleeDamage(rawDamage: Int, isRanged: Boolean, penaltyPercent: Int = 50): Int {
        require(rawDamage >= 0)
        require(penaltyPercent in 0..100)
        return if (isRanged) (rawDamage.toLong() * (100 - penaltyPercent) / 100).toInt()
        else rawDamage
    }

    fun canShoot(
        isRanged: Boolean,
        isAlive: Boolean,
        isPlaced: Boolean,
        hasAdjacentEnemy: Boolean
    ): Boolean = isRanged && isAlive && isPlaced && !hasAdjacentEnemy
}
