package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.*
import com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength
import org.junit.Assert.*
import org.junit.Test

class MovementFormationImmunityTest {
    @Test
    fun `immune troops retain their current formation at every movement distance`() {
        for (speed in listOf(4, 7)) for (distance in 0..speed) {
            val result = ApplyMovementFormationPenaltyUseCase.execute(
                ApplyMovementFormationPenaltyUseCase.Input(
                    currentFormation = 60, maximumFormation = 100,
                    movementDistance = distance, maximumMovement = speed,
                    ignoresMovementFormationPenalty = true
                )
            )
            assertEquals(60, result.remainingFormation)
            assertFalse(result.penaltyApplied)
        }
    }

    @Test
    fun `ordinary infantry still loses formation beyond half movement`() {
        for (distance in 1..4) {
            val result = ApplyMovementFormationPenaltyUseCase.execute(
                ApplyMovementFormationPenaltyUseCase.Input(100, 100, distance, 4)
            )
            assertEquals(if (distance <= 2) 100 else 75, result.remainingFormation)
            assertEquals(distance > 2, result.penaltyApplied)
        }
    }

    @Test
    fun `immunity preserves short move recovery but does not restore formation on a long move`() {
        for (distance in listOf(1, 4)) {
            val result = UpdateFormationAfterMoveUseCase.execute(
                UpdateFormationAfterMoveUseCase.Input(
                    currentFormation = 60, maximumFormation = 100,
                    movementDistance = distance, maximumMovement = 4,
                    turnEndsWithoutAttack = true, ignoresMovementFormationPenalty = true
                )
            )
            assertEquals(if (distance == 1) 75 else 60, result.remainingFormation)
            assertEquals(distance == 1, result.recoveryApplied)
            assertFalse(result.penaltyApplied)
        }
    }

    @Test
    fun `immune troops and troops without formation show only safe reachable tiles`() {
        for ((hasFormation, immune) in listOf(true to true, false to false, false to true)) {
            for (distance in 1..7) {
                assertEquals(BattleMovementPreviewUseCase.TileStyle.SAFE,
                    BattleMovementPreviewUseCase.execute(true, distance, 7,
                        hasFormation = hasFormation, ignoresMovementFormationPenalty = immune))
                val appearance = BattleTileAppearanceUseCase.execute(
                    true, distance, 7, Strength.REINFORCED, true,
                    hasFormation = hasFormation, ignoresMovementFormationPenalty = immune
                )
                assertEquals(0.5f, appearance.terrainAlpha, 0f)
                assertEquals(Strength.REINFORCED, appearance.control)
            }
            assertEquals(BattleMovementPreviewUseCase.TileStyle.HIDDEN,
                BattleMovementPreviewUseCase.execute(false, 8, 7,
                    hasFormation = hasFormation, ignoresMovementFormationPenalty = immune))
            val hidden = BattleTileAppearanceUseCase.execute(
                false, 8, 7, Strength.REINFORCED, true,
                hasFormation = hasFormation, ignoresMovementFormationPenalty = immune
            )
            assertEquals(1f, hidden.terrainAlpha, 0f)
            assertEquals(Strength.NONE, hidden.control)
        }
    }

    @Test
    fun `safe highlight from immunity does not extend follow up shooting range`() {
        assertEquals(BattleMovementPreviewUseCase.TileStyle.SAFE,
            BattleMovementPreviewUseCase.execute(true, 4, 4,
                hasFormation = true, ignoresMovementFormationPenalty = true))
        assertFalse(MoveAndShootRules.grantsShot(true, 4, 4, false))
        assertTrue(MoveAndShootRules.grantsShot(true, 2, 4, false))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `immunity does not accept negative movement distance`() {
        ApplyMovementFormationPenaltyUseCase.execute(
            ApplyMovementFormationPenaltyUseCase.Input(100, 100, -1, 4,
                ignoresMovementFormationPenalty = true)
        )
    }
}
