package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.ApplyFormationDamageUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApplyFormationDamageUseCaseTest {
    private fun apply(damage: Int, formation: Int) = ApplyFormationDamageUseCase.execute(
        ApplyFormationDamageUseCase.Input(
            incomingDamage = damage,
            currentFormation = formation
        )
    )

    @Test
    fun `intact formation splits damage equally with soldiers`() {
        val result = apply(damage = 10, formation = 100)

        assertEquals(5, result.absorbedByFormation)
        assertEquals(95, result.remainingFormation)
        assertEquals(5, result.damageToSoldiers)
    }

    @Test
    fun `odd damage is conserved and remainder hurts soldiers`() {
        val result = apply(damage = 11, formation = 100)

        assertEquals(5, result.absorbedByFormation)
        assertEquals(95, result.remainingFormation)
        assertEquals(6, result.damageToSoldiers)
    }

    @Test
    fun `formation breaking mid-hit sends unabsorbed damage to soldiers`() {
        val result = apply(damage = 10, formation = 2)

        assertEquals(2, result.absorbedByFormation)
        assertEquals(0, result.remainingFormation)
        assertEquals(8, result.damageToSoldiers)
    }

    @Test
    fun `broken formation sends the whole hit to soldiers`() {
        val result = apply(damage = 10, formation = 0)

        assertEquals(0, result.absorbedByFormation)
        assertEquals(0, result.remainingFormation)
        assertEquals(10, result.damageToSoldiers)
    }

    @Test
    fun `zero damage changes nothing`() {
        val result = apply(damage = 0, formation = 75)

        assertEquals(0, result.absorbedByFormation)
        assertEquals(75, result.remainingFormation)
        assertEquals(0, result.damageToSoldiers)
    }

    @Test
    fun `invalid negative damage is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            apply(damage = -1, formation = 10)
        }
    }
}
