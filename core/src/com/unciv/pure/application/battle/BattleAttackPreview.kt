package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.DamageResult
import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.RangedShotRules

/** Deterministic forecast without new luck rolls. A stored retaliation budget retains past luck. */
data class BattleAttackPreview(
    val attack: Hit,
    val retaliation: Hit? = null,
    val retaliationBudget: Int? = null,
    val remainingRetaliationBudget: Int? = null,
    val usesStoredRetaliation: Boolean = false,
    val isRanged: Boolean = false,
    val rangedShot: RangedShotRules.Result? = null
) {
    data class Hit(
        val preparation: PreparedBattleDamage?,
        val flanking: FlankingCalculator.Result,
        val formationBefore: Int,
        val formationReductionPercent: Int,
        val remainingFormation: Int,
        val result: DamageResult
    )

    /** Effective soldier-damage increase for the same raw hit, including formation breaking mid-hit. */
    fun flankingBonusPercent(isRetaliation: Boolean): Double {
        val hit = if (isRetaliation) retaliation ?: return 0.0 else attack
        val flank = hit.flanking
        if (flank.flankLevel <= 0 || (flank.formedDamageBonusPercent == 0 &&
                        flank.formationDamageBonusPercent == 0 && flank.unformedDamageBonusPercent == 0)
        ) return 0.0
        val rawDamage = if (isRetaliation)
            ((retaliationBudget ?: 0) - (remainingRetaliationBudget ?: 0)).coerceAtLeast(0)
        else hit.preparation?.incomingDamage ?: return 0.0
        if (rawDamage == 0) return 0.0
        val withoutFlank = ApplyFlankingDamageUseCase.execute(
            rawDamage,
            hit.formationBefore,
            hit.formationReductionPercent,
            FlankingCalculator.Result()
        ).damageToSoldiers
        val withFlank = ApplyFlankingDamageUseCase.execute(
            rawDamage, hit.formationBefore, hit.formationReductionPercent, flank
        ).damageToSoldiers
        if (withoutFlank == 0) return if (withFlank > 0) Double.POSITIVE_INFINITY else 0.0
        return (withFlank.toDouble() - withoutFlank) * 100.0 / withoutFlank
    }
}
