package com.unciv.pure.domain.battle

/** Pure flanking calculation from facts about distinct troops neighboring the target. */
object FlankingCalculator {
    data class Neighbor(
        val isEnemy: Boolean,
        val isAlly: Boolean = false,
        val isAlive: Boolean = true,
        val isActive: Boolean = true,
        val isAdjacent: Boolean = true,
        val isMelee: Boolean = true,
        val hasFlanking: Boolean = false,
        val hasSupport: Boolean = false
    )

    /** Both damage rates are retained because formation can break during a single hit. */
    data class Result(
        val flankScore: Int = 0,
        val flankLevel: Int = 0,
        val formedDamageBonusPercent: Int = 0,
        val formationDamageBonusPercent: Int = 0,
        val unformedDamageBonusPercent: Int = 0
    )

    fun calculate(
        neighbors: Iterable<Neighbor>,
        profile: FlankingProfile?,
        isMeleeAttack: Boolean = true,
        threshold: Int = 3
    ): Result {
        require(threshold > 0)
        var enemies = 0
        var allies = 0
        for (neighbor in neighbors) {
            if (!neighbor.isAlive || !neighbor.isActive || !neighbor.isAdjacent) continue
            if (neighbor.isEnemy && neighbor.isMelee && neighbor.hasFlanking) enemies++
            if (neighbor.isAlly && neighbor.hasSupport) allies++
        }
        // Keep score as attackers minus supporting neighbors for existing diagnostics.
        val score = enemies - allies
        // The target itself counts as one defender. Both the minimum attacker count
        // and numerical superiority limit the level; ordinary allies do not contribute.
        val level = minOf(enemies.toLong() - threshold + 1L, score.toLong() - 1L)
            .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

        fun bonus(percent: Int): Int = (level.toLong() * percent)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (profile == null || !isMeleeAttack) return Result(score, level)
        return Result(
            score, level,
            bonus(profile.formedDamagePercent),
            bonus(profile.formationDamagePercent),
            bonus(profile.unformedDamagePercent)
        )
    }
}
