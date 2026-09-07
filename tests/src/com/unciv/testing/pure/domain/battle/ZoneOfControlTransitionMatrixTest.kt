package com.unciv.testing.pure.domain.battle

import com.unciv.pure.domain.battle.ZoneOfControlTransition
import com.unciv.pure.domain.battle.ZoneOfControlTransition.Input
import com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength
import org.junit.Assert.*
import org.junit.Test

class ZoneOfControlTransitionMatrixTest {
    private data class Case(
        val from: Strength, val to: Strength, val cost: Float, val stops: Boolean
    )

    private val cases = listOf(
        Case(Strength.NONE, Strength.NONE, 1f, false),
        Case(Strength.NONE, Strength.NORMAL, 1f, false),
        Case(Strength.NONE, Strength.REINFORCED, 1f, true),
        Case(Strength.NORMAL, Strength.NONE, 1f, false),
        Case(Strength.NORMAL, Strength.NORMAL, 2f, false),
        Case(Strength.NORMAL, Strength.REINFORCED, 2f, true),
        Case(Strength.REINFORCED, Strength.NONE, 1f, false),
        Case(Strength.REINFORCED, Strength.NORMAL, 2f, false),
        Case(Strength.REINFORCED, Strength.REINFORCED, 1f, true)
    )

    @Test
    fun `every first step obeys its declared cost and stopping rule`() {
        for (case in cases) {
            val result = ZoneOfControlTransition.execute(Input(case.from, case.to, 10f, true))
            assertTrue(case.toString(), result.allowed)
            assertEquals(case.toString(), case.cost, result.movementCost, 0f)
            assertEquals(case.toString(), case.stops, result.endsMovement)
        }
    }

    @Test
    fun `every transition requires its entire cost with no free final step`() {
        for (case in cases) {
            for ((budget, allowed) in listOf(
                0f to false, case.cost - 0.01f to false,
                case.cost to true, case.cost + 0.01f to true
            )) {
                val result =
                    ZoneOfControlTransition.execute(Input(case.from, case.to, budget, true))
                assertEquals("$case budget=$budget", allowed, result.allowed)
            }
        }
    }

    @Test
    fun `later steps retain costs but cannot continue after entering red`() {
        for (case in cases) {
            val result = ZoneOfControlTransition.execute(Input(case.from, case.to, 10f, false))
            assertEquals(case.toString(), case.from != Strength.REINFORCED, result.allowed)
            assertEquals(case.toString(), case.cost, result.movementCost, 0f)
            assertEquals(case.toString(), case.stops, result.endsMovement)
        }
    }

    @Test
    fun `fractional terrain cost is multiplied only when already in control`() {
        for ((from, expectedCost) in listOf(
            Strength.NONE to 0.5f,
            Strength.NORMAL to 1.5f,
            Strength.REINFORCED to 1.5f
        )) {
            val input = Input(
                from, Strength.NORMAL, expectedCost, true,
                baseCost = 0.5f, normalMultiplier = 3f
            )
            val result = ZoneOfControlTransition.execute(input)
            assertTrue(from.toString(), result.allowed)
            assertEquals(expectedCost, result.movementCost, 0f)
            assertFalse(result.endsMovement)
            assertFalse(
                ZoneOfControlTransition.execute(
                    input.copy(remainingMovement = expectedCost - 0.01f)
                ).allowed
            )
        }
    }

    @Test
    fun `invalid movement facts are rejected`() {
        val valid = Input(Strength.NONE, Strength.NORMAL, 5f, true)
        val invalid = listOf(
            valid.copy(baseCost = 0f), valid.copy(baseCost = -1f),
            valid.copy(baseCost = Float.NaN), valid.copy(baseCost = Float.POSITIVE_INFINITY),
            valid.copy(remainingMovement = -1f), valid.copy(remainingMovement = Float.NaN),
            valid.copy(remainingMovement = Float.POSITIVE_INFINITY),
            valid.copy(normalMultiplier = 0.99f), valid.copy(normalMultiplier = Float.NaN),
            valid.copy(normalMultiplier = Float.POSITIVE_INFINITY)
        )
        for (input in invalid) assertThrows(
            input.toString(),
            IllegalArgumentException::class.java
        ) {
            ZoneOfControlTransition.execute(input)
        }
        assertThrows(IllegalArgumentException::class.java) { ZoneOfControlTransition.strength(-1) }
    }
}
