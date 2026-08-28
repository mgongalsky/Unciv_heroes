package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.ApplyMovementFormationPenaltyUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyMovementFormationPenaltyUseCaseTest {
    private fun apply(
        current: Int = 100,
        maximum: Int = 100,
        distance: Int,
        movement: Int = 5
    ) = ApplyMovementFormationPenaltyUseCase.execute(
        ApplyMovementFormationPenaltyUseCase.Input(
            currentFormation = current,
            maximumFormation = maximum,
            movementDistance = distance,
            maximumMovement = movement
        )
    )

    @Test
    fun `three cells with speed five removes quarter of maximum formation`() {
        val result = apply(distance = 3)

        assertTrue(result.penaltyApplied)
        assertEquals(75, result.remainingFormation)
    }

    @Test
    fun `two cells with speed five has no penalty`() {
        val result = apply(distance = 2)

        assertFalse(result.penaltyApplied)
        assertEquals(100, result.remainingFormation)
    }

    @Test
    fun `exactly half movement has no penalty`() {
        val result = apply(distance = 2, movement = 4)

        assertFalse(result.penaltyApplied)
        assertEquals(100, result.remainingFormation)
    }

    @Test
    fun `penalty cannot reduce formation below zero`() {
        assertEquals(0, apply(current = 10, distance = 3).remainingFormation)
    }

    @Test
    fun `invalid negative distance is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            apply(distance = -1)
        }
    }
}
