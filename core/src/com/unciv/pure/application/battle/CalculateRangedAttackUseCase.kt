package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.RangedShotRules
import com.unciv.pure.domain.battle.TroopSupport

/** Shared shot arithmetic for execution and read-only forecasts. */
object CalculateRangedAttackUseCase {
    fun execute(
        attacker: CalculateFormationMeleeExchangeUseCase.TroopSnapshot,
        defender: CalculateFormationMeleeExchangeUseCase.TroopSnapshot,
        isLuck: Boolean,
        afterMovement: Boolean,
        shot: RangedShotRules.Result?
    ): BattleAttackPreview.Hit {
        val luckMultiplier = if (isLuck) 2 else 1
        val afterLuck = (attacker.amount.toLong() * attacker.damage * luckMultiplier)
            .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        val afterSupport = TroopSupport.damage(afterLuck, attacker.supportBonusPercent)
        val movementDamage = MoveAndShootRules.shotDamage(afterSupport, afterMovement)
        val incomingDamage = RangedShotRules.damage(movementDamage, shot)
        val reduction = defender.formationDamageReductionPercent.coerceIn(0, 100)
        val formation = ApplyFormationDamageUseCase.execute(
            ApplyFormationDamageUseCase.Input(incomingDamage, defender.formation, reduction, 100)
        )
        val result = CalculateDamageUseCase.execute(
            attackerAmount = 1, attackerDamage = formation.damageToSoldiers,
            defenderAmount = defender.amount, defenderHealth = defender.health,
            defenderMaxHealth = defender.maxHealth, isLuck = false
        ).copy(isLuck = isLuck)
        return BattleAttackPreview.Hit(
            PreparedBattleDamage(
                attacker.amount, attacker.damage, luckMultiplier, afterLuck,
                attacker.supportBonusPercent, afterSupport,
                if (afterMovement) MoveAndShootRules.DEFAULT_PENALTY_PERCENT else 0,
                movementDamage
            ),
            FlankingCalculator.Result(), defender.formation, reduction,
            formation.remainingFormation, result
        )
    }
}
