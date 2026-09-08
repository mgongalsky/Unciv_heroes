package com.unciv.testing.pure.domain.battle

import com.unciv.pure.domain.battle.TroopSupport
import org.junit.Assert.assertEquals
import org.junit.Test

class TroopSupportTest {
    private val ally = TroopSupport.Neighbor(true, true, true, true, true)

    @Test
    fun `one or several supporting neighbors grant the same bonus`() {
        for (count in 1..6) {
            assertEquals(25, TroopSupport.bonusPercent(25, true, true, List(count) { ally }))
        }
    }

    @Test
    fun `self enemies dead unsupported and distant troops cannot support`() {
        val invalid = listOf(
            ally.copy(isOtherTroop = false), ally.copy(isAlly = false),
            ally.copy(isAlive = false), ally.copy(hasSupport = false),
            ally.copy(isAdjacent = false)
        )
        for (neighbor in invalid) {
            assertEquals(0, TroopSupport.bonusPercent(25, true, true, listOf(neighbor)))
        }
        assertEquals(0, TroopSupport.bonusPercent(25, true, true, invalid))
        assertEquals(25, TroopSupport.bonusPercent(25, true, true, invalid + ally))
    }

    @Test
    fun `absent capability position life or neighbors disables support`() {
        for (percent in listOf(0, -25)) {
            assertEquals(0, TroopSupport.bonusPercent(percent, true, true, listOf(ally)))
        }
        assertEquals(0, TroopSupport.bonusPercent(25, false, true, listOf(ally)))
        assertEquals(0, TroopSupport.bonusPercent(25, true, false, listOf(ally)))
        assertEquals(0, TroopSupport.bonusPercent(25, true, true, emptyList()))
    }

    @Test
    fun `configured bonus is applied to total damage with one downward rounding`() {
        assertEquals(37, TroopSupport.damage(3 * 10, 25))
        assertEquals(75, TroopSupport.damage(3 * 10 * 2, 25))
        assertEquals(30, TroopSupport.damage(30, 0))
        assertEquals(45, TroopSupport.damage(30, 50))
        assertEquals(0, TroopSupport.damage(0, 25))
        assertEquals(0, TroopSupport.damage(-1, 25))
        assertEquals(30, TroopSupport.damage(30, -25))
        assertEquals(Int.MAX_VALUE, TroopSupport.damage(Int.MAX_VALUE, 25))
    }
}
