package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.CalculateFormationMeleeExchangeUseCase
import com.unciv.pure.domain.battle.FlankingCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class FlankingRetaliationTest {
    private val flank = FlankingCalculator.Result(4, 2, 10, 20, 20)

    @Test
    fun `retaliation consumes raw budget and does not store the previous targets flanking bonus`() {
        val fragile = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(1, 0, 12, 12, 0)
        val defender = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
            1, 100, 1000, 1000, 0, outgoingFlanking = flank
        )
        val first = CalculateFormationMeleeExchangeUseCase.execute(fragile, defender, false, false)
        assertEquals(0, first.damageToAttacker.remainingAmount)
        assertEquals(90, first.remainingRetaliationDamage)
        assertEquals(flank, first.retaliationFlanking)
        val second = CalculateFormationMeleeExchangeUseCase.execute(
            fragile.copy(health = 1000, maxHealth = 1000),
            defender.copy(outgoingFlanking = FlankingCalculator.Result()),
            false, false, first.remainingRetaliationDamage
        )
        assertEquals(910, second.damageToAttacker.remainingHealth)
        assertEquals(0, second.remainingRetaliationDamage)
    }

    @Test
    fun `retaliation can break formation and apply the unformed rate in the same hit`() {
        val attacker = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
            1, 0, 1000, 1000, 60, formationDamageReductionPercent = 50
        )
        val defender = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
            1, 200, 1000, 1000, 0, outgoingFlanking = flank
        )
        val result = CalculateFormationMeleeExchangeUseCase.execute(attacker, defender, false, false)
        assertEquals(825, result.damageToAttacker.remainingHealth)
        assertEquals(0, result.attackerRemainingFormation)
        assertEquals(0, result.remainingRetaliationDamage)
    }

    @Test
    fun `exhausted retaliation does not report an applied bonus`() {
        val troop = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
            1, 100, 1000, 1000, 0, outgoingFlanking = flank
        )
        val result = CalculateFormationMeleeExchangeUseCase.execute(troop, troop, false, false, 0)
        assertEquals(1000, result.damageToAttacker.remainingHealth)
        assertEquals(FlankingCalculator.Result(), result.retaliationFlanking)
    }
}
