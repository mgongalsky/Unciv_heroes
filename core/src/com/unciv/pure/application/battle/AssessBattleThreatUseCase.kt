package com.unciv.pure.application.battle

import kotlin.math.roundToInt

/** A deterministic, UI-independent comparison of two armies before battle. */
object AssessBattleThreatUseCase {
    const val SKILL_BONUS_PER_POINT = 0.05
    const val SPEED_BONUS_PER_POINT = 0.03
    const val RANGE_BONUS_PER_POINT = 0.04

    data class TroopSnapshot(
        val amount: Int,
        val damage: Int,
        val maxHealth: Int,
        val speed: Int,
        val range: Int
    )

    enum class ThreatLevel {
        EASY,
        FAVORABLE,
        EVEN,
        DANGEROUS,
        DEADLY
    }

    data class Result(
        val attackerPower: Int,
        val defenderPower: Int,
        val defenderToAttackerRatio: Double,
        val level: ThreatLevel
    )

    fun execute(
        attacker: List<TroopSnapshot>,
        defender: List<TroopSnapshot>,
        attackerAttackSkill: Int = 0,
        attackerDefenseSkill: Int = 0,
        defenderAttackSkill: Int = 0,
        defenderDefenseSkill: Int = 0
    ): Result {
        val attackerPower = armyPower(attacker, attackerAttackSkill, attackerDefenseSkill)
        val defenderPower = armyPower(defender, defenderAttackSkill, defenderDefenseSkill)
        val ratio = when {
            attackerPower <= 0 && defenderPower <= 0 -> 1.0
            attackerPower <= 0 -> Double.POSITIVE_INFINITY
            else -> defenderPower.toDouble() / attackerPower
        }
        val level = when {
            ratio < 0.6 -> ThreatLevel.EASY
            ratio < 0.85 -> ThreatLevel.FAVORABLE
            ratio < 1.2 -> ThreatLevel.EVEN
            ratio < 1.65 -> ThreatLevel.DANGEROUS
            else -> ThreatLevel.DEADLY
        }
        return Result(attackerPower, defenderPower, ratio, level)
    }

    private fun armyPower(troops: List<TroopSnapshot>, attackSkill: Int, defenseSkill: Int): Int {
        val attackMultiplier = 1.0 + attackSkill.coerceAtLeast(0) * SKILL_BONUS_PER_POINT
        val defenseMultiplier = 1.0 + defenseSkill.coerceAtLeast(0) * SKILL_BONUS_PER_POINT
        return troops.sumOf { troop ->
            val amount = troop.amount.coerceAtLeast(0)
            val combatValue = troop.damage.coerceAtLeast(0) * attackMultiplier +
                    troop.maxHealth.coerceAtLeast(0) * defenseMultiplier
            val utilityMultiplier = 1.0 +
                    troop.speed.coerceAtLeast(0) * SPEED_BONUS_PER_POINT +
                    troop.range.coerceAtLeast(0) * RANGE_BONUS_PER_POINT
            (amount * combatValue * utilityMultiplier).roundToInt()
        }
    }
}
