package com.unciv.testing.pure.domain.battle

import com.unciv.pure.domain.battle.ZoneOfControlTransition
import com.unciv.pure.domain.battle.ZoneOfControlTransition.Input
import com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneOfControlTransitionTest {
    @Test
    fun `two or more distinct enemies reinforce control`() {
        assertEquals(Strength.NONE, ZoneOfControlTransition.strength(0))
        assertEquals(Strength.NORMAL, ZoneOfControlTransition.strength(1))
        assertEquals(Strength.REINFORCED, ZoneOfControlTransition.strength(2))
        assertEquals(Strength.REINFORCED, ZoneOfControlTransition.strength(3))
    }

    @Test
    fun `entering ordinary control from free space has no surcharge`() {
        for (firstStep in listOf(false, true)) {
            val input = Input(Strength.NONE, Strength.NORMAL, 1f, firstStep)
            val result = ZoneOfControlTransition.execute(input)
            assertTrue(result.allowed)
            assertEquals(1f, result.movementCost, 0f)
            assertFalse(result.endsMovement)
            assertFalse(ZoneOfControlTransition.execute(input.copy(remainingMovement = 0.99f)).allowed)
            assertTrue(ZoneOfControlTransition.execute(input.copy(remainingMovement = 1.01f)).allowed)
        }
    }

    @Test
    fun `moving within ordinary control requires the full doubled cost`() {
        val input = Input(Strength.NORMAL, Strength.NORMAL, 2f, true)
        val result = ZoneOfControlTransition.execute(input)
        assertTrue(result.allowed)
        assertEquals(2f, result.movementCost, 0f)
        assertFalse(result.endsMovement)
        assertFalse(ZoneOfControlTransition.execute(input.copy(remainingMovement = 1.99f)).allowed)
        assertTrue(ZoneOfControlTransition.execute(input.copy(remainingMovement = 2.01f)).allowed)
    }

    @Test
    fun `entering reinforced control ends the route`() {
        for (from in listOf(Strength.NONE, Strength.NORMAL)) {
            val result = ZoneOfControlTransition.execute(
                Input(from, Strength.REINFORCED, 5f, false)
            )
            assertTrue(result.allowed)
            assertTrue(result.endsMovement)
            assertEquals(if (from == Strength.NORMAL) 2f else 1f, result.movementCost, 0f)
        }
    }

    @Test
    fun `leaving reinforced control for ordinary control costs two and permits continuation`() {
        val input = Input(Strength.REINFORCED, Strength.NORMAL, 2f, true)
        val result = ZoneOfControlTransition.execute(input)
        assertTrue(result.allowed)
        assertEquals(2f, result.movementCost, 0f)
        assertFalse(result.endsMovement)
        assertFalse(ZoneOfControlTransition.execute(input.copy(remainingMovement = 1.99f)).allowed)
        assertTrue(ZoneOfControlTransition.execute(input.copy(remainingMovement = 2.01f)).allowed)
    }

    @Test
    fun `reinforced control cannot be crossed later in the same route`() {
        for (to in Strength.values()) {
            assertFalse(
                ZoneOfControlTransition.execute(
                    Input(Strength.REINFORCED, to, 10f, false)
                ).allowed
            )
        }
    }

    @Test
    fun `leaving control for a free tile has ordinary cost and permits continuation`() {
        for (from in listOf(Strength.NORMAL, Strength.REINFORCED)) {
            val result = ZoneOfControlTransition.execute(
                Input(from, Strength.NONE, 3f, true)
            )
            assertTrue(result.allowed)
            assertEquals(1f, result.movementCost, 0f)
            assertFalse(result.endsMovement)
        }
    }

    @Test
    fun `no movement points means no step even at the start of an action`() {
        assertFalse(
            ZoneOfControlTransition.execute(
                Input(Strength.REINFORCED, Strength.NORMAL, 0f, true)
            ).allowed
        )
    }

    @Test
    fun `ordinary control multiplier is configurable`() {
        val result = ZoneOfControlTransition.execute(
            Input(Strength.NORMAL, Strength.NORMAL, 6f, false, baseCost = 2f, normalMultiplier = 3f)
        )
        assertTrue(result.allowed)
        assertEquals(6f, result.movementCost, 0f)
    }

    @Test
    fun `the same facts always produce the same result`() {
        val input = Input(Strength.NORMAL, Strength.REINFORCED, 4f, false)
        assertEquals(ZoneOfControlTransition.execute(input), ZoneOfControlTransition.execute(input))
    }

    @Test
    fun `moving from reinforced control to reinforced control still ends movement`() {
        val result = ZoneOfControlTransition.execute(
            Input(Strength.REINFORCED, Strength.REINFORCED, 10f, true)
        )
        assertTrue(result.allowed)
        assertTrue(result.endsMovement)
        assertEquals(1f, result.movementCost, 0f)
    }
}
