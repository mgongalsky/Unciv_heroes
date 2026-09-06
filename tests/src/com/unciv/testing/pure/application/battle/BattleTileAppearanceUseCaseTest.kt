package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleTileAppearanceUseCase
import com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength
import org.junit.Assert.assertEquals
import org.junit.Test

class BattleTileAppearanceUseCaseTest {
    @Test
    fun `safe and formation penalty destinations retain different shading`() {
        val safe = BattleTileAppearanceUseCase.execute(true, 2, 4, Strength.NONE, true)
        val costly = BattleTileAppearanceUseCase.execute(true, 3, 4, Strength.NONE, true)
        assertEquals(0.5f, safe.terrainAlpha, 0f)
        assertEquals(0.7f, costly.terrainAlpha, 0f)
    }

    @Test
    fun `unreachable control is hidden and terrain stays unchanged`() {
        for (strength in Strength.values()) {
            assertEquals(
                BattleTileAppearanceUseCase.Appearance(),
                BattleTileAppearanceUseCase.execute(false, 5, 3, strength, true)
            )
        }
    }

    @Test
    fun `both control levels can be shown without replacing formation shading`() {
        for (strength in listOf(Strength.NORMAL, Strength.REINFORCED)) {
            val safe = BattleTileAppearanceUseCase.execute(true, 1, 4, strength, true)
            val costly = BattleTileAppearanceUseCase.execute(true, 3, 4, strength, true)
            assertEquals(strength, safe.control)
            assertEquals(strength, costly.control)
            assertEquals(0.5f, safe.terrainAlpha, 0f)
            assertEquals(0.7f, costly.terrainAlpha, 0f)
        }
    }

    @Test
    fun `occupied destinations do not show control markers`() {
        assertEquals(
            Strength.NONE,
            BattleTileAppearanceUseCase.execute(true, 1, 4, Strength.REINFORCED, false).control
        )
    }

    @Test
    fun `active tile preserves its shading and can show the control trapping it`() {
        val appearance = BattleTileAppearanceUseCase.execute(true, 0, 4, Strength.REINFORCED, true)
        assertEquals(0.7f, appearance.terrainAlpha, 0f)
        assertEquals(Strength.REINFORCED, appearance.control)
    }
}
