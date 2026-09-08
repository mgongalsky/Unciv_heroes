package com.unciv.pure.domain.battle

/** Mutual support uses explicit battlefield facts and never changes troop state. */
object TroopSupport {
    data class Neighbor(
        val isOtherTroop: Boolean,
        val isAlly: Boolean,
        val isAlive: Boolean,
        val hasSupport: Boolean,
        val isAdjacent: Boolean
    )

    fun bonusPercent(
        configuredPercent: Int,
        isAlive: Boolean,
        isPlaced: Boolean,
        neighbors: Iterable<Neighbor>
    ): Int {
        if (configuredPercent <= 0 || !isAlive || !isPlaced) return 0
        return if (neighbors.any {
                it.isOtherTroop && it.isAlly && it.isAlive && it.hasSupport && it.isAdjacent
            }) configuredPercent else 0
    }

    /** Apply to total outgoing damage, before formation absorption; round down once. */
    fun damage(baseDamage: Int, bonusPercent: Int): Int =
        (baseDamage.coerceAtLeast(0).toLong() *
                (100L + bonusPercent.coerceAtLeast(0)) / 100L)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
