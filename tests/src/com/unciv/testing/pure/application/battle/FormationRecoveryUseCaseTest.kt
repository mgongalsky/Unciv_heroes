package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.FormationRecoveryUseCase as Recovery
import org.junit.Assert.*
import org.junit.Test

class FormationRecoveryUseCaseTest {
    private fun move(
        cost: Float = 1f,
        speed: Int = 5,
        damaged: Boolean = true,
        adjacent: Boolean = false
    ) =
        Recovery.afterMove(Recovery.State(), cost, speed, damaged, adjacent)

    @Test
    fun `short withdrawal prepares recovery only for next activation`() {
        val moved = move()
        assertTrue(Recovery.hasChance(moved, true, false))
        assertFalse(Recovery.canRestore(moved, true, false))
        val waiting = Recovery.endActivation(moved)
        assertTrue(Recovery.hasChance(waiting, true, false))
        assertFalse(Recovery.canRestore(waiting, true, false))
        val ready = Recovery.beginActivation(waiting)
        assertTrue(Recovery.canRestore(ready, true, false))
        assertFalse(Recovery.canRestore(Recovery.endActivation(ready), true, false))
    }

    @Test
    fun `half allowance includes the exact boundary`() {
        assertTrue(Recovery.hasChance(move(1.99f, 4), true, false))
        assertTrue(Recovery.hasChance(move(2f, 4), true, false))
        assertFalse(Recovery.hasChance(move(2.01f, 4), true, false))
        assertTrue(Recovery.hasChance(move(5f, 10), true, false))
        assertFalse(Recovery.hasChance(move(6f, 10), true, false))
    }

    @Test
    fun `morale grants another short withdrawal but recovery waits for next activation`() {
        val first = move(2f, 5)
        val second = Recovery.afterMove(first, 2f, 5, true, false)
        assertEquals(4f, second.movementSpent, 0f)
        assertTrue(Recovery.hasChance(second, true, false))
        assertFalse(Recovery.canRestore(first, true, false))
        assertFalse(Recovery.canRestore(second, true, false))
        val waiting = Recovery.endActivation(second)
        assertFalse(Recovery.canRestore(waiting, true, false))
        assertTrue(Recovery.canRestore(Recovery.beginActivation(waiting), true, false))
    }

    @Test
    fun `incoming attack cancels waiting even without a damage value`() {
        val waiting = Recovery.endActivation(move())
        val interrupted = Recovery.interrupt(waiting)
        assertFalse(Recovery.hasChance(interrupted, true, false))
        assertFalse(Recovery.canRestore(Recovery.beginActivation(interrupted), true, false))
    }

    @Test
    fun `attacking then withdrawing in same activation cannot prepare recovery`() {
        val attacked = Recovery.interrupt(Recovery.State())
        val moved = Recovery.afterMove(attacked, 1f, 5, true, false)
        assertFalse(Recovery.hasChance(moved, true, false))
        assertFalse(
            Recovery.canRestore(
                Recovery.beginActivation(Recovery.endActivation(moved)),
                true,
                false
            )
        )
    }

    @Test
    fun `adjacent enemies and intact or absent formation prevent preparation`() {
        assertFalse(Recovery.hasChance(move(adjacent = true), true, false))
        assertFalse(Recovery.hasChance(move(damaged = false), true, false))
        val ready = Recovery.beginActivation(Recovery.endActivation(move()))
        assertFalse(Recovery.canRestore(ready, true, true))
        assertFalse(Recovery.canRestore(ready, false, false))
        assertFalse(Recovery.canRestore(Recovery.State(), true, false))
    }

    @Test
    fun `movement in recovery activation requires another quiet interval`() {
        val ready = Recovery.beginActivation(Recovery.endActivation(move()))
        val movedAgain = Recovery.afterMove(ready, 1f, 5, true, false)
        assertFalse(Recovery.canRestore(movedAgain, true, false))
        assertTrue(
            Recovery.canRestore(
                Recovery.beginActivation(Recovery.endActivation(movedAgain)),
                true,
                false
            )
        )
    }

    @Test
    fun `invalid movement values are rejected without changing original state`() {
        val original = Recovery.State()
        for (cost in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) {
                Recovery.afterMove(original, cost, 5, true, false)
            }
        }
        assertEquals(Recovery.State(), original)
    }

    @Test
    fun `long movement cannot be repaired by a short morale movement in same activation`() {
        val longMove = move(3f, 5)
        val shortMove = Recovery.afterMove(longMove, 1f, 5, true, false)
        assertFalse(Recovery.hasChance(shortMove, true, false))
        assertFalse(
            Recovery.canRestore(
                Recovery.beginActivation(Recovery.endActivation(shortMove)),
                true,
                false
            )
        )
    }

    @Test
    fun `morale movement at exactly half allowance preserves preparation`() {
        val first = move(2f, 4)
        val second = Recovery.afterMove(first, 2f, 4, true, false)
        assertTrue(Recovery.hasChance(second, true, false))
        assertFalse(Recovery.canRestore(second, true, false))
        assertTrue(
            Recovery.canRestore(
                Recovery.beginActivation(Recovery.endActivation(second)),
                true,
                false
            )
        )
    }
}
