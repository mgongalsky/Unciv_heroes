package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.RestoreFormationUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RestoreFormationUseCaseTest {
    private fun restore(
        current: Int,
        maximum: Int,
        numerator: Int = 1,
        denominator: Int = 4
    ): Int = RestoreFormationUseCase.execute(
        RestoreFormationUseCase.Input(
            currentFormation = current,
            maximumFormation = maximum,
            recoveryNumerator = numerator,
            recoveryDenominator = denominator
        )
    )

    @Test
    fun `partially damaged formation restores quarter of maximum`() {
        assertEquals(65, restore(current = 40, maximum = 100))
    }

    @Test
    fun `restoration is capped at maximum formation`() {
        assertEquals(100, restore(current = 90, maximum = 100))
    }

    @Test
    fun `fractional restoration rounds down`() {
        assertEquals(9, restore(current = 7, maximum = 10))
    }

    @Test
    fun `intact formation does not change`() {
        assertEquals(100, restore(current = 100, maximum = 100))
    }

    @Test
    fun `broken formation does not recover`() {
        assertEquals(0, restore(current = 0, maximum = 100))
    }

    @Test
    fun `invalid recovery denominator is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            restore(current = 50, maximum = 100, denominator = 0)
        }
    }
}
