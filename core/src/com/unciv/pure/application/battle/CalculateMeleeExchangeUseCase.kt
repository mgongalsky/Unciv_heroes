package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.DamageResult

object CalculateMeleeExchangeUseCase {
    data class TroopSnapshot(
        val amount: Int,
        val damage: Int,
        val health: Int,
        val maxHealth: Int
    )

    data class Output(
        val damageToAttacker: DamageResult,
        val damageToDefender: DamageResult,
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
        val appliedRetaliationDamage = minOf(availableRetaliationDamage, attackerTotalHealth)

        val damageToAttacker = CalculateDamageUseCase.execute(
            attackerAmount = 1,
            attackerDamage = appliedRetaliationDamage,
            defenderAmount = attacker.amount,
            defenderHealth = attacker.health,
            defenderMaxHealth = attacker.maxHealth,
            isLuck = false
        ).copy(isLuck = defenderIsLuck && defenderRetaliationDamage == null)

        return Output(
            damageToAttacker = damageToAttacker,
            damageToDefender = CalculateDamageUseCase.execute(
                attackerAmount = attacker.amount,
                attackerDamage = attacker.damage,
                defenderAmount = defender.amount,
                defenderHealth = defender.health,
                defenderMaxHealth = defender.maxHealth,
                isLuck = attackerIsLuck
            ),
            remainingRetaliationDamage = availableRetaliationDamage - appliedRetaliationDamage
        )
    }
}
