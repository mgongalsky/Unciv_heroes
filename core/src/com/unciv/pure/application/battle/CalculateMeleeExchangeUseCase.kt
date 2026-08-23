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
        val damageToDefender: DamageResult
    )

    fun execute(
        attacker: TroopSnapshot,
        defender: TroopSnapshot,
        attackerIsLuck: Boolean,
        defenderIsLuck: Boolean
    ): Output = Output(
        damageToAttacker = CalculateDamageUseCase.execute(
            attackerAmount = defender.amount,
            attackerDamage = defender.damage,
            defenderAmount = attacker.amount,
            defenderHealth = attacker.health,
            defenderMaxHealth = attacker.maxHealth,
            isLuck = defenderIsLuck
        ),
        damageToDefender = CalculateDamageUseCase.execute(
            attackerAmount = attacker.amount,
            attackerDamage = attacker.damage,
            defenderAmount = defender.amount,
            defenderHealth = defender.health,
            defenderMaxHealth = defender.maxHealth,
            isLuck = attackerIsLuck
        )
    )
}
