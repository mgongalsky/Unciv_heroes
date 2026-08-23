package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.IsLuckTriggeredUseCase
import com.unciv.testing.pure.fakes.FakeBattleRandom
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsLuckTriggeredUseCaseTest {

    @Test
    fun `returns true when random is below effective probability`() {
        val random = FakeBattleRandom(listOf(0.0))
        assertTrue(
            IsLuckTriggeredUseCase.execute(
                luckValue = 3,
                random = random,
                luckProbability = 0.3
            )
        )
    }

    @Test
    fun `returns false when random is above effective probability`() {
        val random = FakeBattleRandom(listOf(1.0))
        assertFalse(
            IsLuckTriggeredUseCase.execute(
                luckValue = 3,
                random = random,
                luckProbability = 0.3
            )
        )
    }

    @Test
    fun `returns false when luckValue is zero`() {
        val random = FakeBattleRandom(listOf(0.0))
        assertFalse(
            IsLuckTriggeredUseCase.execute(
                luckValue = 0,
                random = random,
                luckProbability = 0.3
            )
        )
    }

    @Test
    fun `maximum luck scenario guarantees luck`() {
        val random = FakeBattleRandom(listOf(0.999999))
        assertTrue(
            IsLuckTriggeredUseCase.execute(
                luckValue = 3,
                random = random,
                luckProbability = 1.0
            )
        )
    }

    @Test
    fun `disabled luck never triggers`() {
        val random = FakeBattleRandom(listOf(0.0))
        assertFalse(
            IsLuckTriggeredUseCase.execute(
                luckValue = 3,
                random = random,
                luckProbability = 0.0
            )
        )
    }
}
