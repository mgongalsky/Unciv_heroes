package com.unciv.pure.domain.battle

/** Selects among troops that have not completed their activation in this round. */
object InitiativeOrder {
    data class Entry(val speed: Int, val isAttacker: Boolean)

    /** Initiative may be negative. Long arithmetic keeps comparisons safe for Int stats. */
    fun initiative(speed: Int, completedTurns: Int, penaltyPerTurn: Int = 1): Long {
        require(completedTurns >= 0)
        require(penaltyPerTurn >= 0)
        return speed.toLong() - completedTurns.toLong() * penaltyPerTurn
    }

    /** Attackers win ties between sides; input order breaks ties within a side. */
    fun nextIndex(
        entries: List<Entry>,
        attackerTurns: Int,
        defenderTurns: Int,
        penaltyPerTurn: Int = 1
    ): Int? {
        require(attackerTurns >= 0 && defenderTurns >= 0)
        require(penaltyPerTurn >= 0)
        var selected: Int? = null
        var bestInitiative = Long.MIN_VALUE
        entries.forEachIndexed { index, entry ->
            val value = initiative(
                entry.speed, if (entry.isAttacker) attackerTurns else defenderTurns,
                penaltyPerTurn
            )
            val previous = selected
            if (previous == null || value > bestInitiative ||
                (value == bestInitiative && entry.isAttacker && !entries[previous].isAttacker)
            ) {
                selected = index
                bestInitiative = value
            }
        }
        return selected
    }
}
