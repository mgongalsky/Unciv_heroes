package com.unciv.pure.application.battle.session

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.Point

/** Presentation state, not an engine checkpoint. A finished battle with no winner is a draw. */
data class BattleStateSnapshot(
    val troops: List<BattleTroopSnapshot>,
    val turnQueue: List<Int>,
    val remainingTurnsInRound: Int,
    val currentTroopId: Int?,
    val currentSide: BattleSide?,
    val finished: Boolean,
    val winner: BattleSide?,
    val field: BattleFieldDescription? = null,
    val availableCommands: List<BattleCommand> = emptyList(),
    val presentation: BattlePresentationSnapshot = BattlePresentationSnapshot()
)

/** Copies scalar values; neither Troop nor its mutable Formation escapes the engine. */
data class BattleTroopSnapshot(
    val id: Int,
    val side: BattleSide,
    val slot: Int,
    val unitName: String,
    val initialAmount: Int,
    val currentAmount: Int,
    val currentHealth: Int,
    val maxHealth: Int,
    val speed: Int,
    val damage: Int,
    val rangedStrength: Int,
    val formation: Int,
    val maximumFormation: Int,
    val position: Point?,
    val retaliationBudget: Int,
    val normalRetaliationBudget: Int,
    val hasPendingFollowUpShot: Boolean,
    val hasFormationRecoveryChance: Boolean,
    val supportBonusPercent: Int,
    val flankingLevel: Int,
    val formationHealthPercent: Int = 0,
    val formationDamageReductionPercent: Int = 0
)
