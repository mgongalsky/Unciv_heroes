package com.unciv.testing.simulations

/** Compact UI/report value; no battle state or transcripts cross the process boundary. */
data class BalanceCell(
    val attacker: Int, val defender: Int,
    val attackerAmount: Int, val defenderAmount: Int,
    val simulations: Int, val attackerWins: Int, val defenderWins: Int,
    val stalemates: Int, val maxTurns: Int, val other: Int, val averageTurns: Double
) {
    val decisiveRate: Double?
        get() = if (attackerWins + defenderWins == 0) null
        else attackerWins.toDouble() / (attackerWins + defenderWins)
    val shade: Int
        get() = decisiveRate?.let { if (it < 0.45) -1 else if (it > 0.55) 1 else 0 } ?: 2
    val key: List<Int> get() = listOf(attacker, defender, attackerAmount, defenderAmount)
    override fun toString(): String =
        decisiveRate?.let { "%.0f%% · %d".format(it * 100, simulations) }
            ?: "— · $simulations"

    companion object {
        fun parse(line: String): BalanceCell {
            val fields = line.split('\t')
            require(fields.size == 13 && fields[0] == "BALANCE1" && fields[1] == "CELL")
            val n = fields.subList(2, 12).map { it.toInt() }
            require(n[0] >= 0 && n[1] >= 0 && n[2] > 0 && n[3] > 0 && n[4] > 0)
            require(n.drop(5).all { it >= 0 } && n.drop(5).sum() == n[4])
            val turns = fields[12].toDouble()
            require(turns.isFinite() && turns >= 0)
            return BalanceCell(n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[8], n[9], turns)
        }
    }
}
