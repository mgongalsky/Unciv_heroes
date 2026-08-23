package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.CalculateMeleeExchangeUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateMeleeExchangeUseCaseTest {
    private fun troop(
        amount: Int,
        damage: Int = 10,
        health: Int = 100,
        maxHealth: Int = 100
    ) = CalculateMeleeExchangeUseCase.TroopSnapshot(amount, damage, health, maxHealth)

    @Test
    fun `both sides deal damage from the same pre-attack snapshot`() {
        val result = CalculateMeleeExchangeUseCase.execute(
            attacker = troop(amount = 10),
            defender = troop(amount = 8),
            attackerIsLuck = false,
            defenderIsLuck = false
        )

        assertEquals(10, result.damageToAttacker.remainingAmount)
        assertEquals(20, result.damageToAttacker.remainingHealth)
        assertEquals(7, result.damageToDefender.remainingAmount)
        assertEquals(100, result.damageToDefender.remainingHealth)
    }

    @Test
    fun `lethally hit defender still deals damage with its original amount`() {
        val result = CalculateMeleeExchangeUseCase.execute(
            attacker = troop(amount = 10),
            defender = troop(amount = 1, damage = 100),
            attackerIsLuck = false,
            defenderIsLuck = false
        )

        assertEquals(0, result.damageToDefender.remainingAmount)
        assertEquals(9, result.damageToAttacker.remainingAmount)
    }

    @Test
    fun `both last troops can perish in one exchange`() {
        val result = CalculateMeleeExchangeUseCase.execute(
            attacker = troop(amount = 1, damage = 100),
            defender = troop(amount = 1, damage = 100),
            attackerIsLuck = false,
            defenderIsLuck = false
        )

        assertEquals(0, result.damageToAttacker.remainingAmount)
        assertEquals(0, result.damageToDefender.remainingAmount)
    }

    @Test
    fun `attacker and defender luck are applied independently`() {
        val result = CalculateMeleeExchangeUseCase.execute(
            attacker = troop(amount = 5),
            defender = troop(amount = 5),
            attackerIsLuck = true,
            defenderIsLuck = false
        )

        assertFalse(result.damageToAttacker.isLuck)
        assertTrue(result.damageToDefender.isLuck)
        assertEquals(5, result.damageToAttacker.remainingAmount)
        assertEquals(4, result.damageToDefender.remainingAmount)
    }

    @Test
    fun `wounded state of each side is included independently`() {
        val result = CalculateMeleeExchangeUseCase.execute(
            attacker = troop(amount = 3, damage = 10, health = 50),
            defender = troop(amount = 4, damage = 10, health = 25),
            attackerIsLuck = false,
            defenderIsLuck = false
        )

        assertEquals(3, result.damageToAttacker.remainingAmount)
        assertEquals(3, result.damageToDefender.remainingAmount)
        assertEquals(10, result.damageToAttacker.remainingHealth)
        assertEquals(95, result.damageToDefender.remainingHealth)
    }

    @Test
    fun `fully used retaliation leaves no damage for the next attacker`() {
        val first = CalculateMeleeExchangeUseCase.execute(
            attacker = troop(amount = 1),
            defender = troop(amount = 3),
            attackerIsLuck = false,
            defenderIsLuck = false
        )

        assertEquals(1, first.damageToAttacker.remainingAmount)
        assertEquals(70, first.damageToAttacker.remainingHealth)
        assertEquals(0, first.remainingRetaliationDamage)

        val second = CalculateMeleeExchangeUseCase.execute(
            attacker = troop(amount = 5),
            defender = troop(amount = 3),
            attackerIsLuck = false,
            defenderIsLuck = false,
            defenderRetaliationDamage = first.remainingRetaliationDamage
        )

        assertEquals(5, second.damageToAttacker.remainingAmount)
        assertEquals(100, second.damageToAttacker.remainingHealth)
    }

    @Test
    fun `unused lethal overkill remains available for the next attacker`() {
        val first = CalculateMeleeExchangeUseCase.execute(
            attacker = troop(amount = 1, health = 20),
            defender = troop(amount = 10),
            attackerIsLuck = false,
            defenderIsLuck = false
        )

        assertEquals(0, first.damageToAttacker.remainingAmount)
        assertEquals(80, first.remainingRetaliationDamage)

        val second = CalculateMeleeExchangeUseCase.execute(
            attacker = troop(amount = 2),
            defender = troop(amount = 10),
            attackerIsLuck = false,
            defenderIsLuck = false,
            defenderRetaliationDamage = first.remainingRetaliationDamage
        )

        assertEquals(2, second.damageToAttacker.remainingAmount)
        assertEquals(20, second.damageToAttacker.remainingHealth)
        assertEquals(0, second.remainingRetaliationDamage)
    }

    @Test
    fun `wounded attacker consumes only its actual remaining total health`() {
        val result = CalculateMeleeExchangeUseCase.execute(
            attacker = troop(amount = 2, health = 25),
            defender = troop(amount = 20),
            attackerIsLuck = false,
            defenderIsLuck = false
        )

        assertEquals(0, result.damageToAttacker.remainingAmount)
        assertEquals(75, result.remainingRetaliationDamage)
    }
}
