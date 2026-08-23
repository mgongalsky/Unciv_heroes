package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.DamageResult

object CalculateFormationMeleeExchangeUseCase {
    data class TroopSnapshot(
        val amount: Int,
        val damage: Int,
        val health: Int,
        val maxHealth: Int,
        val formation: Int
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
        val fullRetaliationDamage = defender.amount * defender.damage * if (defenderIsLuck) 2 else 1
        val availableRetaliationDamage =
                (defenderRetaliationDamage ?: fullRetaliationDamage).coerceAtLeast(0)
        val attackerTotalHealth =
                ((attacker.amount - 1).coerceAtLeast(0) * attacker.maxHealth + attacker.health)
                    .coerceAtLeast(0)
        val rawDamageNeeded = attacker.formation.coerceAtLeast(0) * 2 + attackerTotalHealth
        val appliedRetaliationDamage = minOf(availableRetaliationDamage, rawDamageNeeded)
        val retaliationFormation = ApplyFormationDamageUseCase.execute(
            ApplyFormationDamageUseCase.Input(appliedRetaliationDamage, attacker.formation)
        )
        val attackFormation = ApplyFormationDamageUseCase.execute(
            ApplyFormationDamageUseCase.Input(
                attacker.amount * attacker.damage * if (attackerIsLuck) 2 else 1,
                defender.formation
            )
        )

        return Output(
            damageToAttacker = calculateSoldierDamage(
                retaliationFormation.damageToSoldiers,
                attacker,
                defenderIsLuck && defenderRetaliationDamage == null
            ),
            damageToDefender = calculateSoldierDamage(
                attackFormation.damageToSoldiers,
                defender,
                attackerIsLuck
            ),
            attackerRemainingFormation = retaliationFormation.remainingFormation,
            defenderRemainingFormation = attackFormation.remainingFormation,
            remainingRetaliationDamage = availableRetaliationDamage - appliedRetaliationDamage
        )
    }

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
