package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.DamageResult
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
        val supportBonusPercent: Int = 0
    )

    data class Output(
        val damageToAttacker: DamageResult,
        val damageToDefender: DamageResult,
        val attackerRemainingFormation: Int,
        val defenderRemainingFormation: Int,
        val remainingRetaliationDamage: Int
    )

    fun execute(
        attacker: TroopSnapshot,
        defender: TroopSnapshot,
        attackerIsLuck: Boolean,
        defenderIsLuck: Boolean,
        defenderRetaliationDamage: Int? = null
    ): Output {
        val fullRetaliationDamage = meleeDamage(defender, defenderIsLuck)
        // Stored retaliation already includes support, luck and penalties; do not apply them twice.
        val availableRetaliationDamage =
                (defenderRetaliationDamage ?: fullRetaliationDamage).coerceAtLeast(0)
        val attackerTotalHealth = if (attacker.amount <= 0) 0L else
            (attacker.amount - 1).toLong() * attacker.maxHealth + attacker.health
        // Find the smallest raw hit that kills the attacker with its configured absorption.
        var lower = 0
        var upper = availableRetaliationDamage
        while (lower < upper) {
            val middle = lower + (upper - lower) / 2
            if (applyFormation(middle, attacker).damageToSoldiers.toLong() >= attackerTotalHealth)
                upper = middle
            else lower = middle + 1
        }
        val appliedRetaliationDamage = lower
        val retaliationFormation = applyFormation(appliedRetaliationDamage, attacker)
        val attackFormation = applyFormation(meleeDamage(attacker, attackerIsLuck), defender)
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
            remainingRetaliationDamage = availableRetaliationDamage - appliedRetaliationDamage
        )
    }

    private fun meleeDamage(troop: TroopSnapshot, isLuck: Boolean): Int =
            RangedCombatRules.meleeDamage(
                TroopSupport.damage(
                    troop.amount * troop.damage * if (isLuck) 2 else 1,
                    troop.supportBonusPercent
                ),
                troop.isRanged, troop.meleePenaltyPercent
            )

    private fun applyFormation(damage: Int, troop: TroopSnapshot) =
            ApplyFormationDamageUseCase.execute(
                ApplyFormationDamageUseCase.Input(
                    damage, troop.formation.coerceAtLeast(0),
                    troop.formationDamageReductionPercent.coerceIn(0, 100), 100
                )
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
