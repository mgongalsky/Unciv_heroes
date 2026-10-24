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
        val damage = attackerAmount.toLong() * attackerDamage * if (isLuck) 2L else 1L
        // Previous wounds affect casualty allocation, but are not damage from this hit.
        val healthDeficit = defenderMaxHealth.toLong() - defenderHealth
        val totalDamage = damage + healthDeficit
        val perished = (totalDamage / defenderMaxHealth)
            .coerceIn(0L, defenderAmount.coerceAtLeast(0).toLong()).toInt()
        val remainingAmount = (defenderAmount - perished).coerceAtLeast(0)
        val remainingHealth = (defenderMaxHealth - totalDamage % defenderMaxHealth).toInt()
        return DamageResult(
            damageDealt = damage.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
            perished = perished,
            remainingAmount = remainingAmount,
            remainingHealth = remainingHealth,
            isLuck = isLuck
        )
    }
}


