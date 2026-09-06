package com.unciv.testing.simulations

import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.pure.application.battle.BattleBatchResult
import com.unciv.pure.application.battle.BattleBatchSimulator
import com.unciv.testing.pure.fakes.FakeBattleFieldBuilder
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.FakeGridBattleField
import com.unciv.testing.pure.fakes.TestableBattleManager

/** Headless scenario shared by the balance tests and the standalone experiment runner.
 * The caller supplies the troop definitions through the existing Koin setup.
 */
object BattleBalanceSimulation {
    fun simulateBatch(
        attacker: String,
        attackerAmount: Int,
        defender: String,
        defenderAmount: Int,
        seeds: Iterable<Long>,
        luckProbability: Double,
        moraleProbability: Double
    ): BattleBatchResult {
        require(attackerAmount > 0 && defenderAmount > 0)
        require(luckProbability in 0.0..1.0 && moraleProbability in 0.0..1.0)
        return BattleBatchSimulator.run(seeds) { seed ->
            val grid = FakeBattleFieldBuilder.buildGrid(14, 8)
            val manager = TestableBattleManager(
                attackerArmy = ArmyInfo(FakeCivilizationInfo(), maxSlots = 1).apply {
                    addUnits(attacker, attackerAmount)
                },
                defenderArmy = ArmyInfo(FakeCivilizationInfo(), maxSlots = 1).apply {
                    addUnits(defender, defenderAmount)
                },
                battleField = FakeGridBattleField(grid),
                random = SeededBattleRandom(seed),
                allTilesReachable = true,
                useRealMovement = false,
                moraleProbability = moraleProbability,
                luckProbability = luckProbability
            )
            val attackerTroop = manager.getAttackerArmy().getAllTroops().filterNotNull().single()
            val defenderTroop = manager.getDefenderArmy().getAllTroops().filterNotNull().single()
            manager.placeTroop(attackerTroop, grid[0][0])
            manager.placeTroop(defenderTroop, grid[0][13])
            manager.initializeTurnQueue()
            val policy = AIBattlePolicy(manager)
            BattleSimulationRunner(
                manager = manager,
                attackerPolicy = policy,
                defenderPolicy = policy,
                maxTurns = 300,
                maxTurnsWithoutProgress = 30,
                seed = seed
            ).run()
        }
    }
}
