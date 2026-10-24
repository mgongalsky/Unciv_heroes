package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.ApplyFlankingDamageUseCase
import com.unciv.pure.application.battle.ApplyFormationDamageUseCase
import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.FlankingProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyFlankingDamageUseCaseTest {
    private fun flank(profile: FlankingProfile = FlankingProfile()) = FlankingCalculator.calculate(
        List(4) { FlankingCalculator.Neighbor(isEnemy = true, hasFlanking = true) }, profile
    )

    @Test
    fun `level two increases soldier damage by ten and formation damage by twenty percent`() {
        val result = ApplyFlankingDamageUseCase.execute(200, 1000, 50, flank())
        assertEquals(110, result.damageToSoldiers)
        assertEquals(120, result.absorbedByFormation)
        assertEquals(880, result.remainingFormation)
    }

    @Test
    fun `unformed target receives twenty percent more damage at level two`() {
        val result = ApplyFlankingDamageUseCase.execute(200, 0, 50, flank())
        assertEquals(240, result.damageToSoldiers)
        assertEquals(0, result.absorbedByFormation)
    }

    @Test
    fun `remaining half of a hit uses the unformed rate after breaking formation`() {
        // First 100 raw damage destroys 60 formation and deals 55 soldier damage.
        // Remaining 100 raw damage deals 120 soldier damage.
        val result = ApplyFlankingDamageUseCase.execute(200, 60, 50, flank())
        assertEquals(175, result.damageToSoldiers)
        assertEquals(60, result.absorbedByFormation)
        assertEquals(0, result.remainingFormation)
    }

    @Test
    fun `exact formation break has no unformed remainder`() {
        val result = ApplyFlankingDamageUseCase.execute(100, 60, 50, flank())
        assertEquals(55, result.damageToSoldiers)
        assertEquals(0, result.remainingFormation)
    }

    @Test
    fun `fractional break point keeps the remainder and rounds soldier damage only once`() {
        // 10/0.6 raw damage while formed, then the remainder at 1.2 damage per raw point.
        val result = ApplyFlankingDamageUseCase.execute(100, 10, 50, flank())
        assertEquals(110, result.damageToSoldiers)
        assertEquals(0, result.remainingFormation)
    }

    @Test
    fun `stronger profile applies to the remainder after formation breaks`() {
        val result = ApplyFlankingDamageUseCase.execute(
            200, 60, 50, flank(FlankingProfile(unformedDamagePercent = 15))
        )
        assertEquals(185, result.damageToSoldiers)
    }

    @Test
    fun `zero level preserves the old damage calculation including rounding and overflow boundaries`() {
        for (damage in listOf(0, 1, 7, 100, Int.MAX_VALUE)) {
            for (formation in listOf(0, 1, 60, Int.MAX_VALUE)) {
                for (reduction in listOf(0, 25, 50, 60, 100)) {
                    val expected = ApplyFormationDamageUseCase.execute(
                        ApplyFormationDamageUseCase.Input(damage, formation, reduction, 100)
                    )
                    assertEquals(expected, ApplyFlankingDamageUseCase.execute(
                        damage, formation, reduction, FlankingCalculator.Result()
                    ))
                }
            }
        }
    }

    @Test
    fun `zero damage leaves formation and soldiers untouched`() {
        assertEquals(
            ApplyFormationDamageUseCase.Output(0, 60, 0),
            ApplyFlankingDamageUseCase.execute(0, 60, 50, flank())
        )
    }

    @Test
    fun `full absorption protects soldiers until formation breaks`() {
        val result = ApplyFlankingDamageUseCase.execute(200, 120, 100, flank())
        assertEquals(120, result.damageToSoldiers)
        assertEquals(0, result.remainingFormation)
    }
}
