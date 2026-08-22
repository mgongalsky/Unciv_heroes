package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleBatchSimulator
import com.unciv.pure.application.battle.BattleSimulationResult
import com.unciv.pure.application.battle.BattleTermination
import org.junit.Assert.assertEquals
import org.junit.Test

class BattleBatchSimulatorTest {
    @Test
    fun `batch aggregates outcomes turn statistics and seeds`() {
        val outcomes = mapOf(
            10L to result(BattleTermination.VICTORY, true, 2, 10L),
            11L to result(BattleTermination.VICTORY, false, 4, 11L),
            12L to result(BattleTermination.STALEMATE, null, 6, 12L),
            13L to result(BattleTermination.MAX_TURNS, null, 8, 13L)
        )

        val batch = BattleBatchSimulator.run(outcomes.keys) { outcomes.getValue(it) }

        assertEquals(4, batch.simulations)
        assertEquals(1, batch.attackerWins)
        assertEquals(1, batch.defenderWins)
        assertEquals(1, batch.stalemates)
        assertEquals(1, batch.maxTurnTerminations)
        assertEquals(0.25, batch.attackerWinRate, 0.0)
        assertEquals(0.25, batch.defenderWinRate, 0.0)
        assertEquals(5.0, batch.averageTurns, 0.0)
        assertEquals(5.0, batch.medianTurns, 0.0)
        assertEquals(listOf(10L, 11L, 12L, 13L), batch.results.map { it.seed })
    }

    @Test
    fun `empty batch has zero metrics`() {
        val batch = BattleBatchSimulator.run(emptyList()) { error("must not be called") }

        assertEquals(0, batch.simulations)
        assertEquals(0.0, batch.attackerWinRate, 0.0)
        assertEquals(0.0, batch.averageTurns, 0.0)
        assertEquals(0.0, batch.medianTurns, 0.0)
    }

    private fun result(
        termination: BattleTermination,
        winnerIsAttacker: Boolean?,
        turns: Int,
        seed: Long
    ) = BattleSimulationResult(
        termination = termination,
        winnerIsAttacker = winnerIsAttacker,
        turns = turns,
        commands = emptyList(),
        events = emptyList(),
        seed = seed
    )
}
