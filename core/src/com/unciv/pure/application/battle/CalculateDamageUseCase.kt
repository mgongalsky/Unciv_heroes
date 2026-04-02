package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.DamageResult

object CalculateDamageUseCase {
    fun execute(
        attackerAmount: Int,
        attackerDamage: Int,
        defenderAmount: Int,
        defenderHealth: Int,
        defenderMaxHealth: Int,
        isLuck: Boolean
    ): DamageResult {
        var damage = attackerAmount * attackerDamage
        if (isLuck) damage *= 2

        val healthDeficit = defenderMaxHealth - defenderHealth
        val totalDamage = damage + healthDeficit

        val perished = (totalDamage / defenderMaxHealth).toInt()
        val remainingAmount = (defenderAmount - perished).coerceAtLeast(0)
        val remainingHealth = defenderMaxHealth - (totalDamage % defenderMaxHealth)

        return DamageResult(
            damageDealt = totalDamage,
            perished = perished,
            remainingAmount = remainingAmount,
            remainingHealth = remainingHealth,
            isLuck = isLuck
        )
    }
}


