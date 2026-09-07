package com.unciv.logic.battle

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattlePolicy
import com.unciv.pure.application.battle.BattleSimulationResult
import com.unciv.pure.application.battle.BattleTermination

class BattleSimulationRunner(
    private val manager: BattleManager,
    private val attackerPolicy: BattlePolicy,
    private val defenderPolicy: BattlePolicy,
    private val maxTurns: Int = 1_000,
    private val maxTurnsWithoutProgress: Int = 100,
    private val seed: Long? = null
) {
    init {
        require(maxTurns > 0) { "maxTurns must be positive" }
        require(maxTurnsWithoutProgress > 0) { "maxTurnsWithoutProgress must be positive" }
    }

    fun run(): BattleSimulationResult {
        val commands = mutableListOf<BattleCommand>()
        val events = mutableListOf<BattleEvent>()
        var turns = 0
        var turnsWithoutProgress = 0
        while (manager.isBattleOn() && turns < maxTurns) {
            val troop = manager.getCurrentTroop()
                ?: return result(BattleTermination.NO_CURRENT_TROOP, turns, commands, events)
            val policy = if (manager.getAttackerArmy()
                        .contains(troop)
            ) attackerPolicy else defenderPolicy
            val command = policy.chooseCommand(troop.id)
            turns++
            val actionResult = if (command == null || command.troopId != troop.id) {
                null
            } else {
                commands.add(command)
                manager.execute(command) { events.add(it) }
            }
            val madeProgress = actionResult?.success == true && command !is BattleCommand.Skip
            turnsWithoutProgress = if (madeProgress) 0 else turnsWithoutProgress + 1
            if (!manager.isBattleOn()) break
            if (turnsWithoutProgress >= maxTurnsWithoutProgress) {
                return result(BattleTermination.STALEMATE, turns, commands, events)
            }
            manager.completeAction(actionResult)
        }
        val battleResult = manager.getBattleResult()
        val termination = when {
            battleResult?.winningArmy == null && !manager.isBattleOn() -> BattleTermination.MUTUAL_DEFEAT
            !manager.isBattleOn() -> BattleTermination.VICTORY
            turns >= maxTurns -> BattleTermination.MAX_TURNS
            else -> BattleTermination.STALEMATE
        }
        return result(termination, turns, commands, events)
    }

    private fun result(
        termination: BattleTermination,
        turns: Int,
        commands: List<BattleCommand>,
        events: List<BattleEvent>
    ): BattleSimulationResult {
        val battleResult = manager.getBattleResult()
        return BattleSimulationResult(
            termination = termination,
            winnerIsAttacker = battleResult?.winningArmy?.let {
                it == manager.getAttackerArmy()
            },
            turns = turns,
            commands = commands.toList(),
            events = events.toList(),
            seed = seed
        )
    }
}
