package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.application.battle.PerformShootUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformShootUseCaseUnitTest {
    private fun input(
        defenderId: Int? = 2,
        canShoot: Boolean = true,
        enemy: Boolean = true,
        luck: Boolean = false,
        morale: Boolean = false,
        remaining: Int = 7,
        died: Boolean = false
    ) = PerformShootUseCase.Input(1, defenderId, canShoot, enemy, luck, morale, remaining, died)

    @Test
    fun `successful shot returns outcome without movement`() {
        val result = PerformShootUseCase.execute(
            input(
                luck = true,
                morale = true,
                remaining = 3,
                died = true
            )
        )
        assertTrue(result.success)
        assertNull(result.rejection)
        assertTrue(result.isLuck)
        assertTrue(result.isMorale)
        assertEquals(3, result.defenderRemainingAmount)
        assertTrue(result.defenderDied)
    }

    @Test
    fun `missing defender is rejected first`() {
        val result =
            PerformShootUseCase.execute(input(defenderId = null, canShoot = false, enemy = false))
        assertFalse(result.success)
        assertEquals(BattleRejection.INVALID_TARGET, result.rejection)
    }

    @Test
    fun `incapable shooter returns legacy NOT_IMPLEMENTED rejection`() {
        val result = PerformShootUseCase.execute(input(canShoot = false))
        assertFalse(result.success)
        assertEquals(BattleRejection.NOT_IMPLEMENTED, result.rejection)
    }

    @Test
    fun `cannot shoot takes priority over non-enemy target`() {
        assertEquals(
            BattleRejection.NOT_IMPLEMENTED,
            PerformShootUseCase.execute(input(canShoot = false, enemy = false)).rejection
        )
    }

    @Test
    fun `non-enemy target is invalid`() {
        val result = PerformShootUseCase.execute(input(enemy = false))
        assertFalse(result.success)
        assertEquals(BattleRejection.INVALID_TARGET, result.rejection)
    }
}
