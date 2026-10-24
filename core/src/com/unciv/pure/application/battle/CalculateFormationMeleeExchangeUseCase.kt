package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.DamageResult
import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.TroopSupport

object CalculateFormationMeleeExchangeUseCase {
    data class TroopSnapshot(
        val amount: Int,
        val damage: Int,
        val health: Int,
        val maxHealth: Int,
        val formation: Int,
        val formationDamageReductionPercent: Int = 50,
        val isRanged: Boolean = false,
        val meleePenaltyPercent: Int = 50,
        val supportBonusPercent: Int = 0,
        val outgoingFlanking: FlankingCalculator.Result = FlankingCalculator.Result()
    )

    data class Output(
        val damageToAttacker: DamageResult,
        val damageToDefender: DamageResult,
        val attackerRemainingFormation: Int,
        val defenderRemainingFormation: Int,
        val remainingRetaliationDamage: Int,
        val attackFlanking: FlankingCalculator.Result = FlankingCalculator.Result(),
        val retaliationFlanking: FlankingCalculator.Result = FlankingCalculator.Result(),
        val attackPreparation: PreparedBattleDamage? = null,
        val retaliationPreparation: PreparedBattleDamage? = null,
        val availableRetaliationDamage: Int = 0,
        val appliedRetaliationDamage: Int = 0
    )

    fun execute(
        attacker: TroopSnapshot,
        defender: TroopSnapshot,
        attackerIsLuck: Boolean,
        defenderIsLuck: Boolean,
        defenderRetaliationDamage: Int? = null
    ): Output {
        val attackFlanking = if (attacker.isRanged) FlankingCalculator.Result() else attacker.outgoingFlanking
        val retaliationFlanking = if (defender.isRanged) FlankingCalculator.Result() else defender.outgoingFlanking
        val retaliationPreparation = prepareDamage(defender, defenderIsLuck)
        // Stored retaliation includes support, luck and penalties, but no target-specific flanking.
        val availableRetaliationDamage =
            (defenderRetaliationDamage ?: retaliationPreparation.incomingDamage).coerceAtLeast(0)
        val attackerTotalHealth = if (attacker.amount <= 0) 0L else
            (attacker.amount - 1).toLong() * attacker.maxHealth + attacker.health
        // Consume only the raw retaliation budget needed to kill this target.
        var lower = 0
        var upper = availableRetaliationDamage
        while (lower < upper) {
            val middle = lower + (upper - lower) / 2
            if (applyFormation(middle, attacker, retaliationFlanking).damageToSoldiers.toLong() >= attackerTotalHealth)
                upper = middle
            else lower = middle + 1
        }
        val appliedRetaliationDamage = lower
        val retaliationFormation = applyFormation(appliedRetaliationDamage, attacker, retaliationFlanking)
        val attackPreparation = prepareDamage(attacker, attackerIsLuck)
        val attackFormation = applyFormation(attackPreparation.incomingDamage, defender, attackFlanking)
        return Output(
            damageToAttacker = calculateSoldierDamage(
                retaliationFormation.damageToSoldiers, attacker,
                defenderIsLuck && defenderRetaliationDamage == null
            ),
            damageToDefender = calculateSoldierDamage(
                attackFormation.damageToSoldiers, defender, attackerIsLuck
            ),
            attackerRemainingFormation = retaliationFormation.remainingFormation,
            defenderRemainingFormation = attackFormation.remainingFormation,
            remainingRetaliationDamage = availableRetaliationDamage - appliedRetaliationDamage,
            attackFlanking = attackFlanking,
            retaliationFlanking = if (appliedRetaliationDamage > 0) retaliationFlanking else FlankingCalculator.Result(),
            attackPreparation = attackPreparation,
            retaliationPreparation = if (defenderRetaliationDamage == null) retaliationPreparation else null,
            availableRetaliationDamage = availableRetaliationDamage,
            appliedRetaliationDamage = appliedRetaliationDamage
        )
    }

    private fun prepareDamage(troop: TroopSnapshot, isLuck: Boolean): PreparedBattleDamage {
        val luckMultiplier = if (isLuck) 2 else 1
        val afterLuck = (troop.amount.toLong() * troop.damage * luckMultiplier)
            .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        val afterSupport = TroopSupport.damage(afterLuck, troop.supportBonusPercent)
        val incomingDamage = RangedCombatRules.meleeDamage(
            afterSupport, troop.isRanged, troop.meleePenaltyPercent
        )
        return PreparedBattleDamage(
            troop.amount, troop.damage, luckMultiplier, afterLuck,
            troop.supportBonusPercent, afterSupport,
            if (troop.isRanged) troop.meleePenaltyPercent else 0, incomingDamage
        )
    }

    private fun applyFormation(damage: Int, troop: TroopSnapshot, flanking: FlankingCalculator.Result) =
        ApplyFlankingDamageUseCase.execute(
            damage, troop.formation.coerceAtLeast(0),
            troop.formationDamageReductionPercent.coerceIn(0, 100), flanking
        )

    private fun calculateSoldierDamage(
        damage: Int,
        defender: TroopSnapshot,
        isLuck: Boolean
    ): DamageResult = CalculateDamageUseCase.execute(
        attackerAmount = 1,
        attackerDamage = damage,
        defenderAmount = defender.amount,
        defenderHealth = defender.health,
        defenderMaxHealth = defender.maxHealth,
        isLuck = false
    ).copy(isLuck = isLuck)
}
