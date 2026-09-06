package com.unciv.testing.simulations

/** Finds a 50% crossing at a fixed attacker army size. All probes use actual integer armies. */
object BalanceBoundarySearch {
    enum class Status { NEAR_HALF, BRACKETED, OUT_OF_RANGE, INCONCLUSIVE }

    data class Result(
        val status: Status,
        val attackerAmount: Int,
        val lowerDefenders: Int?,
        val upperDefenders: Int?,
        val probes: List<BalanceCell>
    ) {
        /** Midpoint of a measured crossing, or the measured near-50% army ratio. */
        val defendersPerAttacker: Double?
            get() = if (status == Status.NEAR_HALF || status == Status.BRACKETED) {
                (lowerDefenders!!.toDouble() + upperDefenders!!) / (2.0 * attackerAmount)
            } else null
    }

    fun run(
        attackerAmount: Int,
        maxDefenders: Int,
        simulate: (attackerAmount: Int, defenderAmount: Int) -> BalanceCell
    ): Result {
        require(attackerAmount > 0 && maxDefenders > 0)
        val probes = linkedMapOf<Int, BalanceCell>()
        fun probe(defenders: Int): BalanceCell = probes.getOrPut(defenders) {
            simulate(attackerAmount, defenders).also {
                require(it.attackerAmount == attackerAmount && it.defenderAmount == defenders)
            }
        }

        fun result(status: Status, lower: Int? = null, upper: Int? = null) =
            Result(status, attackerAmount, lower, upper, probes.values.toList())

        fun nearHalf(cell: BalanceCell) = cell.decisiveRate?.let { it in 0.45..0.55 } == true

        val first = probe(1)
        val firstRate = first.decisiveRate ?: return result(Status.INCONCLUSIVE)
        if (nearHalf(first)) return result(Status.NEAR_HALF, 1, 1)
        // Even one defender is too strong at this army scale; do not extrapolate below one.
        if (firstRate < 0.45) return result(Status.OUT_OF_RANGE)

        var lower = 1
        var upper: Int
        while (true) {
            if (lower == maxDefenders) return result(Status.OUT_OF_RANGE)
            upper = minOf(lower.toLong() * 2, maxDefenders.toLong()).toInt()
            val cell = probe(upper)
            val rate = cell.decisiveRate ?: return result(Status.INCONCLUSIVE)
            if (nearHalf(cell)) return result(Status.NEAR_HALF, upper, upper)
            if (rate < 0.45) break
            lower = upper
        }

        // lower: attacker wins more often; upper: defender wins more often.
        while (upper - lower > 1) {
            val middle = lower + (upper - lower) / 2
            val cell = probe(middle)
            val rate = cell.decisiveRate ?: return result(Status.INCONCLUSIVE)
            if (nearHalf(cell)) return result(Status.NEAR_HALF, middle, middle)
            if (rate > 0.55) lower = middle else upper = middle
        }
        return result(Status.BRACKETED, lower, upper)
    }
}
