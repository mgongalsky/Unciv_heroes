package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleFlankingAppearance as Appearance
import org.junit.Assert.*
import org.junit.Test

class BattleFlankingAppearanceTest {
    @Test
    fun `current encirclement stays visible without a preview`() {
        assertEquals(0f, Appearance.alpha(0, null, 0.5f), 0.0001f)
        for (phase in listOf(0f, 0.25f, 0.5f, 1f)) {
            assertEquals(Appearance.intensity(2), Appearance.alpha(2, null, phase), 0.0001f)
            assertEquals(Appearance.intensity(2), Appearance.alpha(2, 2, phase), 0.0001f)
        }
    }

    @Test
    fun `new encirclement pulses from transparent to red and back`() {
        assertEquals(0f, Appearance.alpha(0, 1, 0f), 0.0001f)
        assertEquals(Appearance.intensity(1) / 2f, Appearance.alpha(0, 1, 0.25f), 0.0001f)
        assertEquals(Appearance.intensity(1), Appearance.alpha(0, 1, 0.5f), 0.0001f)
        assertEquals(0f, Appearance.alpha(0, 1, 1f), 0.0001f)
    }

    @Test
    fun `stronger preview preserves the existing red minimum`() {
        val minimum = Appearance.intensity(1)
        val maximum = Appearance.intensity(3)
        assertTrue(maximum > minimum)
        for (step in 0..100) {
            assertTrue(Appearance.alpha(1, 3, step / 100f) in minimum..maximum)
        }
        assertEquals(minimum, Appearance.alpha(1, 3, 0f), 0.0001f)
        assertEquals(maximum, Appearance.alpha(1, 3, 0.5f), 0.0001f)
    }

    @Test
    fun `intensity remains bounded for extreme levels`() {
        assertEquals(0f, Appearance.intensity(Int.MIN_VALUE), 0f)
        assertTrue(Appearance.intensity(Int.MAX_VALUE) < 1f)
        assertTrue(Appearance.intensity(2) > Appearance.intensity(1))
        assertTrue(Appearance.intensity(3) > Appearance.intensity(2))
    }
}
