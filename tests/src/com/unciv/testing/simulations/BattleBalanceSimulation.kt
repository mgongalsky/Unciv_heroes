package com.unciv.testing.simulations

import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.battle.StandaloneBattleSetup
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.battle.BattleBatchResult
import com.unciv.pure.application.battle.BattleBatchSimulator
import com.unciv.pure.domain.arena.ArenaArmyDistribution

/** Runs ordinary game battles. No test manager, movement overrides or application startup. */
object BattleBalanceSimulation {
    fun simulateBatch(
        attacker: String,
        attackerAmount: Int,
        defender: String,
        defenderAmount: Int,
        seeds: Iterable<Long>,
        luckProbability: Double,
        moraleProbability: Double,
        ruleset: Ruleset,
        troopSlots: Int = 4
    ): BattleBatchResult {
        require(attackerAmount > 0 && defenderAmount > 0)
        require(luckProbability in 0.0..1.0 && moraleProbability in 0.0..1.0)
        fun stacks(name: String, amount: Int) = ArenaArmyDistribution.split(amount, troopSlots)
            .map { StandaloneBattleSetup.Stack(name, it) }
        return BattleBatchSimulator.run(seeds) { seed ->
            val manager = StandaloneBattleSetup.create(
                stacks(attacker, attackerAmount), stacks(defender, defenderAmount), ruleset,
                SeededBattleRandom(seed), luckProbability, moraleProbability
            )
            val policy = AIBattlePolicy(manager)
            BattleSimulationRunner(
                manager, policy, policy,
                maxTurns = 1000, maxTurnsWithoutProgress = 100, seed = seed
            ).run()
        }
    }
}
