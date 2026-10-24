package com.unciv.testing.pure.domain.battle

import com.unciv.pure.domain.battle.RangedShotProfile
import com.unciv.pure.domain.battle.RangedShotRules
import com.unciv.pure.domain.battle.RangedShotRules.Blocker
import com.unciv.pure.domain.battle.RangedShotRules.IndirectReason
import com.unciv.pure.domain.battle.RangedShotRules.ShotType
import org.junit.Assert.*
import org.junit.Test

class RangedShotRulesTest {
    private val profile = RangedShotProfile(3, 20)

    @Test
    fun `clear shots inside and exactly at direct range retain full damage`() {
        for (distance in 1..3) {
            val shot = RangedShotRules.evaluate(true, profile, distance)!!
            assertEquals(ShotType.DIRECT, shot.shotType)
            assertNull(shot.reason)
            assertEquals(false, shot.lineBlocked)
            assertEquals(0, shot.damageModifierPercent)
            assertEquals(101, RangedShotRules.damage(101, shot))
        }
    }

    @Test
    fun `beyond direct range the line is not inspected`() {
        val unreadable = object : Iterable<Blocker> {
            override fun iterator(): Iterator<Blocker> = error("Line must not be inspected")
        }
        for (distance in listOf(4, 5)) {
            val shot = RangedShotRules.evaluate(true, profile, distance, unreadable)!!
            assertEquals(ShotType.INDIRECT, shot.shotType)
            assertEquals(IndirectReason.DISTANCE, shot.reason)
            assertNull(shot.lineBlocked)
            assertEquals(-20, shot.damageModifierPercent)
            assertEquals(80, RangedShotRules.damage(100, shot))
        }
    }

    @Test
    fun `living melee and ranged troops on the line cause indirect fire`() {
        for (melee in listOf(true, false)) {
            val shot = RangedShotRules.evaluate(true, profile, 3, listOf(Blocker(isMelee = melee)))!!
            assertEquals(ShotType.INDIRECT, shot.shotType)
            assertEquals(IndirectReason.BLOCKED_LINE, shot.reason)
            assertEquals(true, shot.lineBlocked)
            assertEquals(80, RangedShotRules.damage(100, shot))
        }
    }

    @Test
    fun `dead troops and noncombat objects do not obstruct direct fire`() {
        val shot = RangedShotRules.evaluate(true, profile, 2,
            listOf(Blocker(isAlive = false), Blocker(isCombatUnit = false)))!!
        assertEquals(ShotType.DIRECT, shot.shotType)
    }

    @Test
    fun `missing property and melee attackers retain legacy damage`() {
        assertNull(RangedShotRules.evaluate(true, null, 4, listOf(Blocker())))
        assertNull(RangedShotRules.evaluate(false, profile, 4, listOf(Blocker())))
        assertEquals(101, RangedShotRules.damage(101, null))
    }

    @Test
    fun `known lower melee blocker can be cleared by elevation`() {
        val lowMelee = listOf(Blocker(elevation = 0))
        assertEquals(ShotType.DIRECT,
            RangedShotRules.evaluate(true, profile, 3, lowMelee, 1)!!.shotType)
        assertEquals(ShotType.INDIRECT,
            RangedShotRules.evaluate(true, profile, 3, lowMelee, 0)!!.shotType)
        assertEquals(ShotType.INDIRECT,
            RangedShotRules.evaluate(true, profile, 3, lowMelee)!!.shotType)
        assertEquals(ShotType.INDIRECT,
            RangedShotRules.evaluate(true, profile, 3, listOf(Blocker()), 1)!!.shotType)
        assertEquals(ShotType.INDIRECT,
            RangedShotRules.evaluate(true, profile, 3,
                listOf(Blocker(isMelee = false, elevation = 0)), 1)!!.shotType)
    }

    @Test
    fun `penalty is configurable and damage arithmetic does not overflow`() {
        for (penalty in listOf(0, 20, 50, 100)) {
            val shot = RangedShotRules.evaluate(true, RangedShotProfile(3, penalty), 4)!!
            assertEquals(101L * (100 - penalty) / 100,
                RangedShotRules.damage(101, shot).toLong())
            assertEquals(Int.MAX_VALUE.toLong() * (100 - penalty) / 100,
                RangedShotRules.damage(Int.MAX_VALUE, shot).toLong())
        }
        assertEquals(ShotType.INDIRECT,
            RangedShotRules.evaluate(true, RangedShotProfile(0, 20), 1)!!.shotType)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `same cell cannot be a shot`() {
        RangedShotRules.evaluate(true, profile, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative range is rejected`() {
        RangedShotProfile(-1, 20)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `penalty above one hundred is rejected`() {
        RangedShotProfile(3, 101)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative penalty is rejected`() {
        RangedShotProfile(3, -1)
    }
}
