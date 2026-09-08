package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleMovementPreviewUseCase
import com.unciv.pure.application.battle.MoveAndShootRules
import org.junit.Assert.*
import org.junit.Test

class MoveAndShootRulesTest {
    @Test
    fun `movement below or exactly at half grants a shot`() {
        assertTrue(MoveAndShootRules.grantsShot(true, 1, 4, false))
        assertTrue(MoveAndShootRules.grantsShot(true, 2, 4, false))
        assertFalse(MoveAndShootRules.grantsShot(true, 3, 4, false))
    }

    @Test
    fun `odd speed uses the exact half boundary`() {
        assertTrue(MoveAndShootRules.grantsShot(true, 2, 5, false))
        assertFalse(MoveAndShootRules.grantsShot(true, 3, 5, false))
        assertFalse(MoveAndShootRules.grantsShot(true, 1, 1, false))
    }

    @Test
    fun `standing still melee troops and repeated grants do not earn a shot`() {
        assertFalse(MoveAndShootRules.grantsShot(true, 0, 4, false))
        assertFalse(MoveAndShootRules.grantsShot(true, 0, 0, false))
        assertFalse(MoveAndShootRules.grantsShot(false, 1, 4, false))
        assertFalse(MoveAndShootRules.grantsShot(true, 1, 4, true))
    }

    @Test
    fun `shot eligibility matches the highlighted half movement zone`() {
        for (speed in 1..10) for (distance in 1..speed) {
            val style = BattleMovementPreviewUseCase.execute(true, distance, speed)
            assertEquals(
                style == BattleMovementPreviewUseCase.TileStyle.SAFE,
                MoveAndShootRules.grantsShot(true, distance, speed, false)
            )
        }
    }

    @Test
    fun `moving halves total shot damage while stationary shots remain unchanged`() {
        assertEquals(50, MoveAndShootRules.shotDamage(100, true))
        assertEquals(100, MoveAndShootRules.shotDamage(100, false))
        assertEquals(7, MoveAndShootRules.shotDamage(15, true))
        assertEquals(15, MoveAndShootRules.shotDamage(15 * 2, true))
        assertEquals(0, MoveAndShootRules.shotDamage(0, true))
        assertEquals(0, MoveAndShootRules.shotDamage(1, true))
    }

    @Test
    fun `shot penalty is configurable independently of melee penalty`() {
        assertEquals(75, MoveAndShootRules.shotDamage(100, true, 25))
        assertEquals(100, MoveAndShootRules.shotDamage(100, true, 0))
        assertEquals(0, MoveAndShootRules.shotDamage(100, true, 100))
    }
}
