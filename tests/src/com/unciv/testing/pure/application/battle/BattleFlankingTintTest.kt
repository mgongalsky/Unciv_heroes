package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleFlankingAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class BattleFlankingTintTest {
    private val blue = BattleFlankingAppearance.Tint(0.25f, 0.7f, 1f, 0.6f)

    @Test
    fun `support transitions to red and back`() {
        assertEquals(blue, BattleFlankingAppearance.tint(0, 1, true, 0f))
        val peak = BattleFlankingAppearance.tint(0, 1, true, 0.5f)
        assertEquals(1f, peak.r, 0.0001f)
        assertEquals(0.12f, peak.g, 0.0001f)
        assertEquals(0.08f, peak.b, 0.0001f)
        assertEquals(BattleFlankingAppearance.intensity(1), peak.a, 0.0001f)
        assertEquals(blue, BattleFlankingAppearance.tint(0, 1, true, 1f))
    }

    @Test
    fun `current flanking overrides support and preview can restore blue`() {
        assertEquals(BattleFlankingAppearance.tint(2, null, false, 0f),
            BattleFlankingAppearance.tint(2, null, true, 0f))
        val restored = BattleFlankingAppearance.tint(2, 0, true, 0.5f)
        assertEquals(blue.r, restored.r, 0.0001f)
        assertEquals(blue.g, restored.g, 0.0001f)
        assertEquals(blue.b, restored.b, 0.0001f)
        assertEquals(blue.a, restored.a, 0.0001f)
    }

    @Test
    fun `unsupported preview retains transparent red endpoints`() {
        for (phase in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val tint = BattleFlankingAppearance.tint(0, 2, false, phase)
            assertEquals(1f, tint.r, 0f)
            assertEquals(0.12f, tint.g, 0f)
            assertEquals(0.08f, tint.b, 0f)
            assertEquals(BattleFlankingAppearance.alpha(0, 2, phase), tint.a, 0.0001f)
        }
    }
}
