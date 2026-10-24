package com.unciv.logic.battle.session

import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.session.BattleSessionEngine
import com.unciv.pure.application.battle.session.BattleSessionExecution
import com.unciv.pure.domain.battle.BattleSide

/** Adapts an initialized battle. Its owning session must have exclusive access to the manager. */
class BattleManagerSessionEngine(private val manager: BattleManager) : BattleSessionEngine {
    override val isFinished: Boolean get() = !manager.isBattleOn()
    override val currentTroopId: Int?
        get() = if (isFinished) null else manager.getCurrentTroop()?.id
    override val currentSide: BattleSide?
        get() {
            if (isFinished) return null
            val troop = manager.getCurrentTroop() ?: return null
            return when {
                manager.getAttackerArmy().contains(troop) -> BattleSide.ATTACKER
                manager.getDefenderArmy().contains(troop) -> BattleSide.DEFENDER
                else -> null
            }
        }

    override fun execute(command: BattleCommand): BattleSessionExecution {
        val events = mutableListOf<BattleEvent>()
        val result = manager.execute(command) { events.add(it) }
        if (result.success) manager.completeAction(result)
        // Post-battle recovery is separate: consumers need the final combat state for the report.
        return BattleSessionExecution(result, events.toList())
    }
    override fun snapshot(): com.unciv.pure.application.battle.session.BattleStateSnapshot =
            BattleStateSnapshotReader.capture(manager)
}
