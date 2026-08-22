package com.unciv.testing.pure.application.battle

import com.unciv.infrastructure.battle.SeededBattleRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SeededBattleRandomTest {
    @Test
    fun `same seed produces same sequence`() {
        val first = SeededBattleRandom(42L)
        val second = SeededBattleRandom(42L)

        assertEquals(
            List(20) { first.nextDouble() },
            List(20) { second.nextDouble() }
        )
    }

    @Test
    fun `different seeds produce different sequences`() {
        val first = SeededBattleRandom(1L)
        val second = SeededBattleRandom(2L)

        assertNotEquals(
            List(5) { first.nextDouble() },
            List(5) { second.nextDouble() }
        )
    }
}
