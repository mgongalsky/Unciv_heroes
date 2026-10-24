package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.CalculateDamageUseCase
import com.unciv.pure.application.battle.ApplyFlankingDamageUseCase
import com.unciv.pure.domain.battle.FlankingCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class DamageArithmeticRegressionTest {
    @Test
    fun `old wounds are not reported as new damage`() {
        val result = CalculateDamageUseCase.execute(1, 20, 3, 60, 100, false)
        assertEquals(20, result.damageDealt)
        assertEquals(0, result.perished)
        assertEquals(3, result.remainingAmount)
        assertEquals(40, result.remainingHealth)
        val zero = CalculateDamageUseCase.execute(1, 0, 3, 60, 100, false)
        assertEquals(0, zero.damageDealt)
        assertEquals(60, zero.remainingHealth)
    }

    @Test
    fun `overkill cannot create extra casualties`() {
        val result = CalculateDamageUseCase.execute(1, 10000, 5, 60, 100, false)
        assertEquals(10000, result.damageDealt)
        assertEquals(5, result.perished)
        assertEquals(0, result.remainingAmount)
    }

    @Test
    fun `large lucky hit does not wrap into negative damage`() {
        val result = CalculateDamageUseCase.execute(Int.MAX_VALUE, Int.MAX_VALUE, 5, 60, 100, true)
        assertEquals(Int.MAX_VALUE, result.damageDealt)
        assertEquals(5, result.perished)
        assertEquals(0, result.remainingAmount)
    }

    @Test
    fun `remaining stack health equals previous health minus this hit`() {
        for (amount in 1..5) for (health in listOf(1, 40, 100)) {
            for (damage in listOf(0, 1, 20, 99, 100, 101, 1000)) {
                val before = (amount - 1) * 100 + health
                val expected = (before - damage).coerceAtLeast(0)
                val result = CalculateDamageUseCase.execute(1, damage, amount, health, 100, false)
                val actual = if (result.remainingAmount == 0) 0 else
                    (result.remainingAmount - 1) * 100 + result.remainingHealth
                assertEquals(expected, actual)
                assertEquals(amount - result.remainingAmount, result.perished)
                assertEquals(damage, result.damageDealt)
            }
        }
    }

    @Test
    fun `level one fractional formation break rounds combined soldier damage once`() {
        // Formed raw = 20 / 0.55 = 400/11.
        // Soldiers = (400/11)*0.525 + (700/11)*1.1 = 980/11; ceil = 90.
        val result = ApplyFlankingDamageUseCase.execute(
            100, 20, 50, FlankingCalculator.Result(3, 1, 5, 10, 10)
        )
        assertEquals(20, result.absorbedByFormation)
        assertEquals(0, result.remainingFormation)
        assertEquals(90, result.damageToSoldiers)
    }

    @Test
    fun `melee preparation saturates before support instead of wrapping`() {
        val attacker =
                com.unciv.pure.application.battle.CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
                    50000, 50000, 100, 100, 0, supportBonusPercent = 25
                )
        val defender =
                com.unciv.pure.application.battle.CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
                    5, 0, 100, 100, 0
                )
        val result =
                com.unciv.pure.application.battle.CalculateFormationMeleeExchangeUseCase.execute(
                    attacker, defender, true, false
                )
        assertEquals(Int.MAX_VALUE, result.attackPreparation!!.afterLuck)
        assertEquals(Int.MAX_VALUE, result.attackPreparation!!.afterSupport)
        assertEquals(5, result.damageToDefender.perished)
        assertEquals(0, result.damageToDefender.remainingAmount)
    }
}
