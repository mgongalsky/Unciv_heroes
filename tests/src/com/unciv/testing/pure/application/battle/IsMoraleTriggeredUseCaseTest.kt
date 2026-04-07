package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.IsMoraleTriggeredUseCase
import com.unciv.testing.pure.fakes.FakeBattleRandom
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsMoraleTriggeredUseCaseTest {

    @Test
    fun `returns true when random is below effective probability`() {
        val random = FakeBattleRandom(listOf(0.0))
        assertTrue(IsMoraleTriggeredUseCase.execute(moraleValue = 3, random = random, moraleProbability = 0.3))
    }

    @Test
    fun `returns false when random is above effective probability`() {
        val random = FakeBattleRandom(listOf(1.0))
        assertFalse(IsMoraleTriggeredUseCase.execute(moraleValue = 3, random = random, moraleProbability = 0.3))
    }

    @Test
    fun `returns false when moraleValue is zero`() {
        val random = FakeBattleRandom(listOf(0.0))
        assertFalse(IsMoraleTriggeredUseCase.execute(moraleValue = 0, random = random, moraleProbability = 0.3))
    }
}
