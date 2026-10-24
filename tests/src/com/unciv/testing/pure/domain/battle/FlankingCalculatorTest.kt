package com.unciv.testing.pure.domain.battle

import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.FlankingProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class FlankingCalculatorTest {
    private val enemy = FlankingCalculator.Neighbor(isEnemy = true, hasFlanking = true)
    private val supporter = FlankingCalculator.Neighbor(
        isEnemy = false, isAlly = true, hasSupport = true
    )

    @Test
    fun `flanking begins at three enemies and increases for each additional enemy`() {
        for ((count, level) in listOf(0 to 0, 1 to 0, 2 to 0, 3 to 1, 4 to 2)) {
            val result = FlankingCalculator.calculate(List(count) { enemy }, FlankingProfile())
            assertEquals(count, result.flankScore)
            assertEquals(level, result.flankLevel)
            assertEquals(level * 5, result.formedDamageBonusPercent)
            assertEquals(level * 10, result.formationDamageBonusPercent)
            assertEquals(level * 10, result.unformedDamageBonusPercent)
        }
    }

    @Test
    fun `supporting ally reduces four enemies to level one`() {
        val result = FlankingCalculator.calculate(
            List(4) { enemy } + List(2) { supporter },
            FlankingProfile()
        )
        assertEquals(2, result.flankScore)
        assertEquals(1, result.flankLevel)
    }

    @Test
    fun `ranged unequipped dead inactive and distant enemies cannot create flanking`() {
        val excluded = listOf(
            enemy.copy(isMelee = false),
            enemy.copy(hasFlanking = false),
            enemy.copy(isAlive = false),
            enemy.copy(isActive = false),
            enemy.copy(isAdjacent = false)
        )
        for (neighbor in excluded) {
            val result = FlankingCalculator.calculate(listOf(enemy, enemy, neighbor), FlankingProfile())
            assertEquals(2, result.flankScore)
            assertEquals(0, result.flankLevel)
        }
    }

    @Test
    fun `only active adjacent living allies with support reduce the score`() {
        val excluded = listOf(
            supporter.copy(hasSupport = false),
            supporter.copy(isAlive = false),
            supporter.copy(isActive = false),
            supporter.copy(isAdjacent = false),
            supporter.copy(isAlly = false)
        )
        for (neighbor in excluded) {
            val result = FlankingCalculator.calculate(List(3) { enemy } + neighbor, FlankingProfile())
            assertEquals(3, result.flankScore)
            assertEquals(1, result.flankLevel)
        }
    }

    @Test
    fun `friendly support can produce a negative score but never a negative level`() {
        val result = FlankingCalculator.calculate(listOf(supporter), FlankingProfile())
        assertEquals(-1, result.flankScore)
        assertEquals(0, result.flankLevel)
        assertEquals(0, result.formedDamageBonusPercent)
    }

    @Test
    fun `stronger profile changes the unformed bonus without changing geometry`() {
        val result = FlankingCalculator.calculate(
            List(4) { enemy }, FlankingProfile(unformedDamagePercent = 15)
        )
        assertEquals(2, result.flankLevel)
        assertEquals(10, result.formedDamageBonusPercent)
        assertEquals(20, result.formationDamageBonusPercent)
        assertEquals(30, result.unformedDamageBonusPercent)
    }

    @Test
    fun `missing profile and ranged attacks receive no bonuses`() {
        val neighbors = List(3) { enemy }
        for (result in listOf(
            FlankingCalculator.calculate(neighbors, null),
            FlankingCalculator.calculate(neighbors, FlankingProfile(), isMeleeAttack = false)
        )) {
            assertEquals(3, result.flankScore)
            assertEquals(1, result.flankLevel)
            assertEquals(0, result.formedDamageBonusPercent)
            assertEquals(0, result.formationDamageBonusPercent)
            assertEquals(0, result.unformedDamageBonusPercent)
        }
    }

    @Test
    fun `peasants without flanking do not create a bonus despite surrounding the target`() {
        val result = FlankingCalculator.calculate(
            List(6) { enemy.copy(hasFlanking = false) }, null
        )
        assertEquals(FlankingCalculator.Result(), result)
    }

    @Test
    fun `minimum attackers and superiority over target plus support are separate requirements`() {
        val cases = listOf(
            Triple(2, 0, 0),
            Triple(3, 0, 1),
            Triple(3, 1, 1),
            Triple(3, 2, 0),
            Triple(3, 3, 0),
            Triple(4, 0, 2),
            Triple(4, 1, 2),
            Triple(4, 2, 1),
            Triple(5, 0, 3),
            Triple(5, 1, 3),
            Triple(6, 0, 4)
        )
        for ((attackers, supporters, expectedLevel) in cases) {
            val result = FlankingCalculator.calculate(
                List(attackers) { enemy } + List(supporters) { supporter }, FlankingProfile()
            )
            val label = "attackers=$attackers supporters=$supporters"
            assertEquals(label, attackers - supporters, result.flankScore)
            assertEquals(label, expectedLevel, result.flankLevel)
            assertEquals(label, expectedLevel * 5, result.formedDamageBonusPercent)
            assertEquals(label, expectedLevel * 10, result.formationDamageBonusPercent)
            assertEquals(label, expectedLevel * 10, result.unformedDamageBonusPercent)
        }
    }
}
