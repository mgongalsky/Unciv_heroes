package com.unciv.testing.pure.domain.battle

import com.unciv.pure.domain.battle.AntiCavalryDamageRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AntiCavalryDamageRulesTest {
    @Test fun bonusAppliesOnlyToCavalry() {
        assertEquals(150, AntiCavalryDamageRules.damage(100, 50, true))
        assertEquals(100, AntiCavalryDamageRules.damage(100, 50, false))
        assertEquals(100, AntiCavalryDamageRules.damage(100, 0, true))
    }

    @Test fun bonusUsesConfiguredPercentageAndRoundsOncePerHit() {
        assertEquals(7, AntiCavalryDamageRules.damage(5, 50, true))
        assertEquals(125, AntiCavalryDamageRules.damage(100, 25, true))
        assertEquals(0, AntiCavalryDamageRules.damage(0, 50, true))
    }

    @Test fun largeValuesSaturateWithoutOverflow() {
        assertEquals(Int.MAX_VALUE, AntiCavalryDamageRules.damage(Int.MAX_VALUE, Int.MAX_VALUE, true))
    }

    @Test fun invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AntiCavalryDamageRules.damage(-1, 50, true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AntiCavalryDamageRules.damage(100, -1, true)
        }
    }
}
