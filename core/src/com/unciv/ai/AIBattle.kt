package com.unciv.ai

import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.domain.troop.Troop

/** Executes the shared AI policy through the same command entry point as player input. */
class AIBattle(
    private val battleManager: BattleManager,
    private val onApplicationEvent: ((BattleEvent) -> Unit)? = null
) {
    companion object {
        var AI_verbose = true
    }

    private val policy = AIBattlePolicy(battleManager)

    fun performTurn(troop: Troop): BattleCommandResult? {
        val command = policy.chooseCommand(troop.id) ?: return null
        if (AI_verbose) println("AI Turn: ${troop.unitName}, command=$command")
        return battleManager.execute(command, onApplicationEvent)
    }
}
