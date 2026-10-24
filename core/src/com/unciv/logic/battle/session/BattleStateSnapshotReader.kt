package com.unciv.logic.battle.session

import com.unciv.logic.battle.BattleManager
import com.unciv.pure.application.battle.session.BattleStateSnapshot
import com.unciv.pure.application.battle.session.BattleTroopSnapshot
import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.Point

/** Read under the same session serialization boundary as command execution. */
object BattleStateSnapshotReader {
    fun capture(manager: BattleManager): BattleStateSnapshot {
        fun troops(army: IArmy, side: BattleSide) = army.getAllTroops().mapIndexedNotNull { slot, troop ->
            troop ?: return@mapIndexedNotNull null
            val position = manager.getTroopTile(troop)?.position
            BattleTroopSnapshot(
                id = troop.id, side = side, slot = slot, unitName = troop.unitName,
                initialAmount = troop.amount, currentAmount = troop.currentAmount,
                currentHealth = troop.currentHealth, maxHealth = troop.maxHealth,
                speed = troop.speed, damage = troop.damage, rangedStrength = troop.rangedStrength,
                formation = troop.formation.current, maximumFormation = troop.formation.maximum,
                position = position?.let { Point(it.x.toInt(), it.y.toInt()) },
                retaliationBudget = manager.getRetaliationBudget(troop),
                normalRetaliationBudget = manager.getNormalRetaliationBudget(troop),
                hasPendingFollowUpShot = manager.hasPendingFollowUpShot(troop),
                hasFormationRecoveryChance = manager.hasFormationRecoveryChance(troop),
                supportBonusPercent = manager.getSupportBonusPercent(troop),
                flankingLevel = manager.getCurrentFlankingLevel(troop),
                formationHealthPercent = troop.formationHealthPercent,
                formationDamageReductionPercent = troop.formationDamageReductionPercent
            )
        }
        val attacker = manager.getAttackerArmy()
        val defender = manager.getDefenderArmy()
        val finished = !manager.isBattleOn()
        val current = if (finished) null else manager.getCurrentTroop()
        val winner = if (finished) manager.getBattleResult()?.winningArmy else null
        val commands = BattleAvailableCommandsReader.capture(manager)
        return BattleStateSnapshot(
            troops = troops(attacker, BattleSide.ATTACKER) + troops(defender, BattleSide.DEFENDER),
            turnQueue = if (finished) emptyList() else manager.getTurnQueue().map { it.id },
            remainingTurnsInRound = if (finished) 0 else manager.getRemainingTurnsInRound(),
            currentTroopId = current?.id,
            currentSide = current?.let {
                if (attacker.contains(it)) BattleSide.ATTACKER else BattleSide.DEFENDER
            },
            finished = finished,
            winner = when {
                winner === attacker -> BattleSide.ATTACKER
                winner === defender -> BattleSide.DEFENDER
                else -> null
            },
            availableCommands = commands,
            presentation = BattlePresentationReader.capture(manager, commands)
        )
    }
}
