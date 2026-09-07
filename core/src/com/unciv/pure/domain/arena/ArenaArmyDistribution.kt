package com.unciv.pure.domain.arena

/** Stable, nonempty stacks whose total equals the original army size. */
object ArenaArmyDistribution {
    fun split(total: Int, requestedSlots: Int): List<Int> {
        require(total > 0)
        require(requestedSlots in 1..5)
        val slots = minOf(total, requestedSlots)
        val base = total / slots
        val remainder = total % slots
        return List(slots) { index -> base + if (index < remainder) 1 else 0 }
    }
}
