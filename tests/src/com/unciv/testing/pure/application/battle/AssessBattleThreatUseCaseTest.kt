package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.AssessBattleThreatUseCase
import com.unciv.pure.application.battle.AssessBattleThreatUseCase.ThreatLevel
import com.unciv.pure.application.battle.AssessBattleThreatUseCase.TroopSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessBattleThreatUseCaseTest {
    private val standardTroop = TroopSnapshot(
        amount = 10,
        damage = 10,
        maxHealth = 100,
        speed = 3,
        range = 0
    )

    @Test
    fun `equal armies are an even fight`() {
        val result = AssessBattleThreatUseCase.execute(listOf(standardTroop), listOf(standardTroop))

        assertEquals(ThreatLevel.EVEN, result.level)
        assertEquals(1.0, result.defenderToAttackerRatio, 0.001)
    }

    @Test
    fun `much smaller defender is easy`() {
        val defender = standardTroop.copy(amount = 5)

        val result = AssessBattleThreatUseCase.execute(listOf(standardTroop), listOf(defender))

        assertEquals(ThreatLevel.EASY, result.level)
    }

    @Test
    fun `much larger defender is deadly`() {
        val defender = standardTroop.copy(amount = 20)

        val result = AssessBattleThreatUseCase.execute(listOf(standardTroop), listOf(defender))

        assertEquals(ThreatLevel.DEADLY, result.level)
    }

    @Test
    fun `speed and range increase tactical power`() {
        val mobileRanged = standardTroop.copy(speed = 6, range = 4)

        val result = AssessBattleThreatUseCase.execute(listOf(standardTroop), listOf(mobileRanged))

        assertTrue(result.defenderPower > result.attackerPower)
    }

    @Test
    fun `hero skills improve their army assessment`() {
        val result = AssessBattleThreatUseCase.execute(
            attacker = listOf(standardTroop),
            defender = listOf(standardTroop),
            defenderAttackSkill = 5,
            defenderDefenseSkill = 5
        )

        assertEquals(ThreatLevel.DANGEROUS, result.level)
    }

    @Test
    fun `empty attacker facing troops is deadly`() {
        val result = AssessBattleThreatUseCase.execute(emptyList(), listOf(standardTroop))

        assertEquals(ThreatLevel.DEADLY, result.level)
        assertEquals(Double.POSITIVE_INFINITY, result.defenderToAttackerRatio, 0.0)
    }

    @Test
    fun `two empty armies compare as even`() {
        val result = AssessBattleThreatUseCase.execute(emptyList(), emptyList())

        assertEquals(ThreatLevel.EVEN, result.level)
        assertEquals(1.0, result.defenderToAttackerRatio, 0.001)
    }
}
