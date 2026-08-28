package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.UpdateFormationAfterMoveUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateFormationAfterMoveUseCaseTest {
    private fun update(
        current: Int = 50,
        maximum: Int = 100,
        distance: Int,
        movement: Int = 5,
        turnEndsWithoutAttack: Boolean = true
    ) = UpdateFormationAfterMoveUseCase.execute(
        UpdateFormationAfterMoveUseCase.Input(
            currentFormation = current,
            maximumFormation = maximum,
            movementDistance = distance,
            maximumMovement = movement,
            turnEndsWithoutAttack = turnEndsWithoutAttack
        )
    )

    @Test
    fun `one cell move ending the turn restores fifteen percent of maximum`() {
        val result = update(distance = 1)

        assertEquals(65, result.remainingFormation)
        assertTrue(result.recoveryApplied)
        assertFalse(result.penaltyApplied)
    }

    @Test
    fun `one cell morale move does not restore before the extra action`() {
        val result = update(distance = 1, turnEndsWithoutAttack = false)

        assertEquals(50, result.remainingFormation)
        assertFalse(result.recoveryApplied)
        assertFalse(result.penaltyApplied)
    }

    @Test
    fun `two cell move neither restores nor fatigues speed five troop`() {
        val result = update(distance = 2)

        assertEquals(50, result.remainingFormation)
        assertFalse(result.recoveryApplied)
        assertFalse(result.penaltyApplied)
    }

    @Test
    fun `three cell move keeps existing quarter fatigue`() {
        val result = update(distance = 3)

        assertEquals(25, result.remainingFormation)
        assertFalse(result.recoveryApplied)
        assertTrue(result.penaltyApplied)
    }

    @Test
    fun `ordinary one cell move takes recovery priority for speed one troop`() {
        val result = update(distance = 1, movement = 1)

        assertEquals(65, result.remainingFormation)
        assertTrue(result.recoveryApplied)
        assertFalse(result.penaltyApplied)
    }

    @Test
    fun `fully depleted formation restores after one cell move`() {
        val result = update(current = 0, distance = 1)

        assertEquals(15, result.remainingFormation)
        assertTrue(result.recoveryApplied)
        assertFalse(result.penaltyApplied)
    }
}
