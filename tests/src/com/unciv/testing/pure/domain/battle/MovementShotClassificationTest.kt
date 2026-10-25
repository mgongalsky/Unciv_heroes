package com.unciv.testing.pure.domain.battle

import com.unciv.pure.domain.battle.RangedShotProfile
import com.unciv.pure.domain.battle.RangedShotRules
import org.junit.Assert.*
import org.junit.Test

class MovementShotClassificationTest {
    @Test
    fun `movement marks a clear shot without applying movement damage twice`() {
        val direct = RangedShotRules.evaluate(true, RangedShotProfile(), 2)!!
        val moved = RangedShotRules.afterMovement(direct, true)!!
        assertEquals(RangedShotRules.ShotType.INDIRECT, moved.shotType)
        assertEquals(RangedShotRules.IndirectReason.MOVEMENT, moved.reason)
        assertEquals(false, moved.lineBlocked)
        assertEquals(0, moved.damageModifierPercent)
        assertEquals(50, RangedShotRules.damage(50, moved))
        assertEquals(moved, RangedShotRules.afterMovement(moved, true))
        assertEquals(RangedShotRules.ShotType.DIRECT, direct.shotType)
    }

    @Test
    fun `stationary and unconfigured shots retain their classification`() {
        val direct = RangedShotRules.evaluate(true, RangedShotProfile(), 2)!!
        assertEquals(direct, RangedShotRules.afterMovement(direct, false))
        assertNull(RangedShotRules.afterMovement(null, true))
        assertNull(RangedShotRules.afterMovement(null, false))
    }

    @Test
    fun `existing distance and obstruction penalties remain unchanged after movement`() {
        val profile = RangedShotProfile(3, 20)
        val shots = listOf(
            RangedShotRules.evaluate(true, profile, 4)!!,
            RangedShotRules.evaluate(true, profile, 3, listOf(RangedShotRules.Blocker()))!!
        )
        for (shot in shots) {
            assertEquals(shot, RangedShotRules.afterMovement(shot, true))
            assertEquals(40, RangedShotRules.damage(50, RangedShotRules.afterMovement(shot, true)))
        }
    }
}
