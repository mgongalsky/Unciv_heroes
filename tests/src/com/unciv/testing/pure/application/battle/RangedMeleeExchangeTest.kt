package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.CalculateFormationMeleeExchangeUseCase
import org.junit.Assert.*
import org.junit.Test

class RangedMeleeExchangeTest {
    private fun troop(ranged: Boolean = false, formation: Int = 0) =
        CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
            10, 10, 100, 100, formation, isRanged = ranged
        )

    @Test
    fun `archer attack and retaliation both deal half damage`() {
        val result = CalculateFormationMeleeExchangeUseCase.execute(
            troop(true), troop(true), false, false
        )
        assertEquals(50, result.damageToAttacker.damageDealt)
        assertEquals(50, result.damageToDefender.damageDealt)
        assertEquals(0, result.remainingRetaliationDamage)
    }

    @Test
    fun `luck doubles penalized damage before formation absorbs it`() {
        val result = CalculateFormationMeleeExchangeUseCase.execute(
            troop(true), troop(formation = 100), true, false
        )
        assertEquals(50, result.defenderRemainingFormation)
        assertEquals(50, result.damageToDefender.damageDealt)
        assertTrue(result.damageToDefender.isLuck)
        assertEquals(100, result.damageToAttacker.damageDealt)
    }

    @Test
    fun `remaining archer retaliation is spent without a second penalty`() {
        val first = CalculateFormationMeleeExchangeUseCase.execute(
            troop().copy(amount = 1, health = 10), troop(true), false, false
        )
        assertEquals(40, first.remainingRetaliationDamage)
        val second = CalculateFormationMeleeExchangeUseCase.execute(
            troop(), troop(true), false, true, first.remainingRetaliationDamage
        )
        assertEquals(40, second.damageToAttacker.damageDealt)
        assertEquals(0, second.remainingRetaliationDamage)
        assertFalse(second.damageToAttacker.isLuck)
    }

    @Test
    fun `spent retaliation stays zero and configured penalty affects only ranged troops`() {
        val result = CalculateFormationMeleeExchangeUseCase.execute(
            troop(true).copy(meleePenaltyPercent = 25), troop(), false, false, 0
        )
        assertEquals(75, result.damageToDefender.damageDealt)
        assertEquals(0, result.damageToAttacker.damageDealt)
    }
}
