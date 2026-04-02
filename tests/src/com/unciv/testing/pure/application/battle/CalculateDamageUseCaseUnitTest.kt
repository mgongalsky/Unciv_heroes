package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.CalculateDamageUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateDamageUseCaseUnitTest {

    private fun execute(
        attackerAmount: Int = 10,
        attackerDamage: Int = 10,
        defenderAmount: Int = 10,
        defenderHealth: Int = 100,
        defenderMaxHealth: Int = 100,
        isLuck: Boolean = false
    ) = CalculateDamageUseCase.execute(
        attackerAmount, attackerDamage,
        defenderAmount, defenderHealth, defenderMaxHealth,
        isLuck
    )

    // --- Базовая математика ---

    @Test
    fun `damage equals attacker amount times attacker damage`() {
        val result = execute(attackerAmount = 5, attackerDamage = 20)
        assertEquals(100, result.damageDealt)
    }

    @Test
    fun `luck doubles damage dealt`() {
        val normal = execute(attackerAmount = 5, attackerDamage = 20, isLuck = false)
        val lucky = execute(attackerAmount = 5, attackerDamage = 20, isLuck = true)
        assertEquals(normal.damageDealt * 2, lucky.damageDealt)
    }

    @Test
    fun `isLuck flag preserved in result`() {
        assertFalse(execute(isLuck = false).isLuck)
        assertTrue(execute(isLuck = true).isLuck)
    }

    // --- Потери ---

    @Test
    fun `no units perish when damage less than max health`() {
        val result = execute(attackerAmount = 1, attackerDamage = 10, defenderMaxHealth = 100)
        assertEquals(0, result.perished)
        assertEquals(10, result.remainingAmount)
    }

    @Test
    fun `one unit perishes when damage equals max health`() {
        val result = execute(attackerAmount = 1, attackerDamage = 100, defenderMaxHealth = 100)
        assertEquals(1, result.perished)
        assertEquals(9, result.remainingAmount)
    }

    @Test
    fun `multiple units perish with high damage`() {
        val result = execute(attackerAmount = 10, attackerDamage = 50, defenderMaxHealth = 100)
        assertEquals(5, result.perished)
        assertEquals(5, result.remainingAmount)
    }

    @Test
    fun `remaining amount never goes below zero`() {
        val result = execute(attackerAmount = 100, attackerDamage = 100, defenderAmount = 3, defenderMaxHealth = 100)
        assertEquals(0, result.remainingAmount)
    }

    // --- Здоровье последнего юнита ---

    @Test
    fun `remaining health calculated correctly after partial damage`() {
        // 1 атакующий наносит 50 урона, у защитника 100 хп
        // perished = 50/100 = 0, remainingHealth = 100 - 50 = 50
        val result = execute(attackerAmount = 1, attackerDamage = 50, defenderMaxHealth = 100)
        assertEquals(0, result.perished)
        assertEquals(50, result.remainingHealth)
    }

    @Test
    fun `wounded defender loses more units`() {
        // защитник уже ранен — первый юнит добивается быстрее
        val healthy = execute(defenderHealth = 100, defenderMaxHealth = 100)
        val wounded = execute(defenderHealth = 10, defenderMaxHealth = 100)
        // у раненого healthDeficit=90, поэтому больше урона тратится на добивание
        assertTrue(wounded.perished >= healthy.perished)
    }

    @Test
    fun `health deficit from wounded unit adds to total damage`() {
        // attackerDamage=50, healthDeficit=50 → totalDamage=100 → perished=1
        val result = execute(
            attackerAmount = 1,
            attackerDamage = 50,
            defenderHealth = 50,
            defenderMaxHealth = 100
        )
        assertEquals(1, result.perished)
    }

    // --- Граничные случаи ---

    @Test
    fun `zero attacker amount deals no damage`() {
        val result = execute(attackerAmount = 0)
        assertEquals(0, result.perished)
        assertEquals(10, result.remainingAmount)
    }

    @Test
    fun `single attacker vs single defender no kill`() {
        val result = execute(
            attackerAmount = 1,
            attackerDamage = 10,
            defenderAmount = 1,
            defenderMaxHealth = 100
        )
        assertEquals(0, result.perished)
        assertEquals(1, result.remainingAmount)
        assertEquals(90, result.remainingHealth)
    }

    @Test
    fun `exact damage to kill all defenders`() {
        // 5 атакующих по 20 урона = 100 = 1 защитник с 100 хп
        val result = execute(
            attackerAmount = 5,
            attackerDamage = 20,
            defenderAmount = 1,
            defenderMaxHealth = 100
        )
        assertEquals(1, result.perished)
        assertEquals(0, result.remainingAmount)
    }
}
