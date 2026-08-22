package com.unciv.pure.application.battle

data class BattleBatchResult(
    val simulations: Int,
    val attackerWins: Int,
    val defenderWins: Int,
    val stalemates: Int,
    val maxTurnTerminations: Int,
    val otherTerminations: Int,
    val averageTurns: Double,
    val medianTurns: Double,
    val results: List<BattleSimulationResult>
) {
    val attackerWinRate: Double
        get() = if (simulations == 0) 0.0 else attackerWins.toDouble() / simulations

    val defenderWinRate: Double
        get() = if (simulations == 0) 0.0 else defenderWins.toDouble() / simulations
}

object BattleBatchSimulator {
    fun run(
        seeds: Iterable<Long>,
        simulate: (Long) -> BattleSimulationResult
    ): BattleBatchResult {
        val results = seeds.map(simulate)
        val turns = results.map { it.turns }.sorted()
        val median = when {
            turns.isEmpty() -> 0.0
            turns.size % 2 == 1 -> turns[turns.size / 2].toDouble()
            else -> (turns[turns.size / 2 - 1] + turns[turns.size / 2]) / 2.0
        }

        return BattleBatchResult(
            simulations = results.size,
            attackerWins = results.count {
                it.termination == BattleTermination.VICTORY && it.winnerIsAttacker == true
            },
            defenderWins = results.count {
                it.termination == BattleTermination.VICTORY && it.winnerIsAttacker == false
            },
            stalemates = results.count { it.termination == BattleTermination.STALEMATE },
            maxTurnTerminations = results.count { it.termination == BattleTermination.MAX_TURNS },
            otherTerminations = results.count {
                it.termination == BattleTermination.NO_CURRENT_TROOP
            },
            averageTurns = if (turns.isEmpty()) 0.0 else turns.average(),
            medianTurns = median,
            results = results
        )
    }
}
