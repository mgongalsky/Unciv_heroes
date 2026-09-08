package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.RangedCombatRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RangedCombatRulesTest {
    @Test
    fun `archers deal half total melee damage`() {
        assertEquals(50, RangedCombatRules.meleeDamage(100, true))
        assertEquals(100, RangedCombatRules.meleeDamage(100, false))
    }

    @Test
    fun `rounding happens on the total hit after luck`() {
        assertEquals(7, RangedCombatRules.meleeDamage(3 * 5, true))
        assertEquals(15, RangedCombatRules.meleeDamage(3 * 5 * 2, true))
        assertEquals(0, RangedCombatRules.meleeDamage(0, true))
        assertEquals(0, RangedCombatRules.meleeDamage(1, true))
        assertEquals(1, RangedCombatRules.meleeDamage(2, true))
    }

    @Test
    fun `penalty can be varied without changing melee troops`() {
        assertEquals(100, RangedCombatRules.meleeDamage(100, true, 0))
        assertEquals(75, RangedCombatRules.meleeDamage(100, true, 25))
        assertEquals(0, RangedCombatRules.meleeDamage(100, true, 100))
        assertEquals(100, RangedCombatRules.meleeDamage(100, false, 100))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative penalty is invalid`() {
        RangedCombatRules.meleeDamage(100, true, -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `penalty above one hundred is invalid`() {
        RangedCombatRules.meleeDamage(100, true, 101)
    }

    @Test
    fun `only a living placed unblocked ranged troop can shoot`() {
        assertTrue(RangedCombatRules.canShoot(true, true, true, false))
        assertFalse(RangedCombatRules.canShoot(true, true, true, true))
        assertFalse(RangedCombatRules.canShoot(false, true, true, false))
        assertFalse(RangedCombatRules.canShoot(true, false, true, false))
        assertFalse(RangedCombatRules.canShoot(true, true, false, false))
    }
}
