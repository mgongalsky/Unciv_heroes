package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.FlankingCalculator
import java.math.BigInteger

/** Applies independent soldier/cohesion bonuses and switches rates exactly when formation breaks. */
object ApplyFlankingDamageUseCase {
    fun execute(
        incomingDamage: Int,
        currentFormation: Int,
        reductionPercent: Int,
        flanking: FlankingCalculator.Result
    ): ApplyFormationDamageUseCase.Output {
        require(incomingDamage >= 0)
        require(currentFormation >= 0)
        require(reductionPercent in 0..100)
        require(flanking.formedDamageBonusPercent >= 0)
        require(flanking.formationDamageBonusPercent >= 0)
        require(flanking.unformedDamageBonusPercent >= 0)
        if (flanking.flankLevel <= 0 ||
            (flanking.formedDamageBonusPercent == 0 &&
                flanking.formationDamageBonusPercent == 0 &&
                flanking.unformedDamageBonusPercent == 0)
        ) return ApplyFormationDamageUseCase.execute(
            ApplyFormationDamageUseCase.Input(incomingDamage, currentFormation, reductionPercent, 100)
        )

        fun number(value: Int) = BigInteger.valueOf(value.toLong())
        val hundred = number(100)
        val scale = number(10_000)
        val raw = number(incomingDamage)
        val formation = number(currentFormation)
        val unformedRate = hundred + number(flanking.unformedDamageBonusPercent)
        // Exact rational arithmetic avoids losing the fractional remainder at the transition.
        val formationRate = number(reductionPercent) *
            (hundred + number(flanking.formationDamageBonusPercent))
        val soldierRate = number(100 - reductionPercent) *
            (hundred + number(flanking.formedDamageBonusPercent))
        if (currentFormation == 0) return ApplyFormationDamageUseCase.Output(
            0, 0, roundedDamage(raw * unformedRate, hundred)
        )
        val formationImpact = raw * formationRate
        if (formationRate == BigInteger.ZERO || formationImpact <= formation * scale) {
            val lostFormation = (formationImpact / scale).toInt()
            return ApplyFormationDamageUseCase.Output(
                lostFormation, currentFormation - lostFormation,
                roundedDamage(raw * soldierRate, scale)
            )
        }
        val soldierNumerator = formation * soldierRate * hundred +
            (formationImpact - formation * scale) * unformedRate
        return ApplyFormationDamageUseCase.Output(
            currentFormation, 0,
            roundedDamage(soldierNumerator, formationRate * hundred)
        )
    }

    /** Round soldier damage up once, matching the existing damage-minus-floored-absorption rule. */
    private fun roundedDamage(numerator: BigInteger, denominator: BigInteger): Int =
        ((numerator + denominator - BigInteger.ONE) / denominator)
            .min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt()
}
