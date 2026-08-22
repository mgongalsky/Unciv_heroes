package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.application.battle.PerformMoveUseCase
import com.unciv.pure.domain.battle.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformMoveUseCaseUnitTest {
    private val current = Point(0, 0)
    private val target = Point(1, 0)

    private fun input(
        achievable: Boolean = true,
        occupiedByAlly: Boolean = false,
        free: Boolean = true,
        currentPoint: Point? = current
    ) = PerformMoveUseCase.Input(
        target = target,
        current = currentPoint,
        isTargetAchievable = achievable,
        isTargetOccupiedByAlly = occupiedByAlly,
        isTargetFree = free
    )

    @Test
    fun `successful move returns value-only coordinates`() {
        val result = PerformMoveUseCase.execute(input())
        assertTrue(result.success)
        assertNull(result.rejection)
        assertEquals(current, result.movedFrom)
        assertEquals(target, result.movedTo)
    }

    @Test
    fun `successful move preserves null current coordinate`() {
        assertNull(PerformMoveUseCase.execute(input(currentPoint = null)).movedFrom)
    }

    @Test
    fun `unreachable target is rejected first`() {
        val result = PerformMoveUseCase.execute(
            input(
                achievable = false,
                occupiedByAlly = true,
                free = false
            )
        )
        assertFalse(result.success)
        assertEquals(BattleRejection.TOO_FAR, result.rejection)
        assertNull(result.movedFrom)
        assertNull(result.movedTo)
    }

    @Test
    fun `ally occupation takes priority over generic occupation`() {
        val result = PerformMoveUseCase.execute(input(occupiedByAlly = true, free = false))
        assertFalse(result.success)
        assertEquals(BattleRejection.OCCUPIED_BY_ALLY, result.rejection)
    }

    @Test
    fun `occupied target returns HEX_OCCUPIED`() {
        val result = PerformMoveUseCase.execute(input(free = false))
        assertFalse(result.success)
        assertEquals(BattleRejection.HEX_OCCUPIED, result.rejection)
        assertNull(result.movedTo)
    }
}
