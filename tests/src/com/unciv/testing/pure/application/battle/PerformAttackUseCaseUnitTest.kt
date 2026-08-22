package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.application.battle.PerformAttackUseCase
import com.unciv.pure.domain.battle.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformAttackUseCaseUnitTest {
    private val current = Point(0, 0)
    private val attackFrom = Point(1, 0)

    private fun input(
        defenderId: Int? = 2,
        from: Point? = attackFrom,
        currentPoint: Point? = current,
        enemy: Boolean = true,
        achievable: Boolean = true,
        free: Boolean = true,
        luck: Boolean = false,
        morale: Boolean = false,
        remaining: Int = 7,
        died: Boolean = false
    ) = PerformAttackUseCase.Input(
        1,
        defenderId,
        from,
        currentPoint,
        enemy,
        achievable,
        free,
        luck,
        morale,
        remaining,
        died
    )

    @Test
    fun `successful attack returns value-only movement and outcome`() {
        val result = PerformAttackUseCase.execute(
            input(
                luck = true,
                morale = true,
                remaining = 3,
                died = true
            )
        )
        assertTrue(result.success)
        assertNull(result.rejection)
        assertEquals(current, result.movedFrom)
        assertEquals(attackFrom, result.movedTo)
        assertTrue(result.isLuck)
        assertTrue(result.isMorale)
        assertEquals(3, result.defenderRemainingAmount)
        assertTrue(result.defenderDied)
    }

    @Test
    fun `missing defender is invalid target`() {
        val result = PerformAttackUseCase.execute(input(defenderId = null))
        assertFalse(result.success)
        assertEquals(BattleRejection.INVALID_TARGET, result.rejection)
    }

    @Test
    fun `non-enemy target is invalid`() {
        assertEquals(
            BattleRejection.INVALID_TARGET,
            PerformAttackUseCase.execute(input(enemy = false)).rejection
        )
    }

    @Test
    fun `missing attack coordinate is invalid`() {
        assertEquals(
            BattleRejection.INVALID_TARGET,
            PerformAttackUseCase.execute(input(from = null)).rejection
        )
    }

    @Test
    fun `unreachable attack coordinate is invalid`() {
        assertEquals(
            BattleRejection.INVALID_TARGET,
            PerformAttackUseCase.execute(input(achievable = false)).rejection
        )
    }

    @Test
    fun `occupied current coordinate remains a valid attack origin`() {
        assertTrue(
            PerformAttackUseCase.execute(
                input(
                    currentPoint = attackFrom,
                    free = false
                )
            ).success
        )
    }

    @Test
    fun `occupied non-current attack coordinate is invalid`() {
        assertEquals(
            BattleRejection.INVALID_TARGET,
            PerformAttackUseCase.execute(input(free = false)).rejection
        )
    }
}
