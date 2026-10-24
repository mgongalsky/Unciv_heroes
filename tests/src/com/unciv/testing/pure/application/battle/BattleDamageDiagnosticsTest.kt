package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.CalculateFormationMeleeExchangeUseCase
import org.junit.Assert.*
import org.junit.Test

class BattleDamageDiagnosticsTest {
    @Test
    fun `diagnostic values capture luck support and ranged melee penalty in execution order`() {
        val attacker = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
            4, 10, 100, 100, 0, isRanged = true, supportBonusPercent = 25
        )
        val defender = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(4, 0, 100, 100, 0)
        val result = CalculateFormationMeleeExchangeUseCase.execute(attacker, defender, true, false)
        val trace = result.attackPreparation!!
        assertEquals(2, trace.luckMultiplier)
        assertEquals(80, trace.afterLuck)
        assertEquals(100, trace.afterSupport)
        assertEquals(50, trace.penaltyPercent)
        assertEquals(50, trace.incomingDamage)
        assertEquals(50, result.damageToDefender.remainingHealth)
    }

    @Test
    fun `stored retaliation is identified without claiming a fresh luck or support calculation`() {
        val attacker = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(1, 0, 10, 10, 0)
        val defender = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(4, 10, 100, 100, 0)
        val result = CalculateFormationMeleeExchangeUseCase.execute(attacker, defender, false, true, 40)
        assertNull(result.retaliationPreparation)
        assertEquals(40, result.availableRetaliationDamage)
        assertEquals(10, result.appliedRetaliationDamage)
        assertEquals(30, result.remainingRetaliationDamage)
    }
}
