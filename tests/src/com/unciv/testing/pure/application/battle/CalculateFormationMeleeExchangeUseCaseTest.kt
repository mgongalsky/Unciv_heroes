package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.CalculateFormationMeleeExchangeUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateFormationMeleeExchangeUseCaseTest {
    private fun troop(
        amount: Int = 10,
        damage: Int = 10,
        health: Int = 100,
        maxHealth: Int = 100,
        formation: Int = amount * 10
    ) = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
        amount, damage, health, maxHealth, formation
    )

    @Test
    fun `both sides split first melee exchange between formation and soldiers`() {
        val result = CalculateFormationMeleeExchangeUseCase.execute(
            attacker = troop(amount = 10),
            defender = troop(amount = 8),
            attackerIsLuck = false,
            defenderIsLuck = false
        )

        assertEquals(60, result.attackerRemainingFormation)
        assertEquals(30, result.defenderRemainingFormation)
        assertEquals(10, result.damageToAttacker.remainingAmount)
        assertEquals(60, result.damageToAttacker.remainingHealth)
        assertEquals(8, result.damageToDefender.remainingAmount)
        assertEquals(50, result.damageToDefender.remainingHealth)
    }

    @Test
    fun `broken defender takes the whole melee damage`() {
        val result = CalculateFormationMeleeExchangeUseCase.execute(
            attacker = troop(amount = 10),
            defender = troop(amount = 8, formation = 0),
            attackerIsLuck = false,
            defenderIsLuck = false
        )

        assertEquals(7, result.damageToDefender.remainingAmount)
        assertEquals(100, result.damageToDefender.remainingHealth)
    }

    @Test
    fun `formation breaking during retaliation sends overflow to soldiers`() {
        val result = CalculateFormationMeleeExchangeUseCase.execute(
            attacker = troop(amount = 1, formation = 2),
            defender = troop(amount = 3),
            attackerIsLuck = false,
            defenderIsLuck = false
        )

        assertEquals(0, result.attackerRemainingFormation)
        assertEquals(1, result.damageToAttacker.remainingAmount)
        assertEquals(72, result.damageToAttacker.remainingHealth)
        assertEquals(0, result.remainingRetaliationDamage)
    }

    @Test
    fun `each side uses its own protection percentage`() {
        val result = CalculateFormationMeleeExchangeUseCase.execute(
            attacker = troop().copy(formationDamageReductionPercent = 60),
            defender = troop().copy(formationDamageReductionPercent = 25),
            attackerIsLuck = false, defenderIsLuck = false
        )
        assertEquals(40, result.attackerRemainingFormation)
        assertEquals(75, result.defenderRemainingFormation)
        assertEquals(60, result.damageToAttacker.remainingHealth)
        assertEquals(25, result.damageToDefender.remainingHealth)
    }

    @Test
    fun `retaliation spends only the lethal damage at full protection`() {
        val result = CalculateFormationMeleeExchangeUseCase.execute(
            attacker = troop(amount = 1, health = 10, formation = 20)
                .copy(formationDamageReductionPercent = 100),
            defender = troop(), attackerIsLuck = false, defenderIsLuck = false
        )
        assertEquals(0, result.damageToAttacker.remainingAmount)
        assertEquals(0, result.attackerRemainingFormation)
        assertEquals(70, result.remainingRetaliationDamage)
    }
}
