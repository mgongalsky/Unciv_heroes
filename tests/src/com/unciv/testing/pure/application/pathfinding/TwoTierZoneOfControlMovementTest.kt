package com.unciv.testing.pure.application.pathfinding

import com.badlogic.gdx.math.Vector2
import com.unciv.pure.application.pathfinding.BattleMovementContext
import com.unciv.pure.application.pathfinding.MovementRangeUseCase
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwoTierZoneOfControlMovementTest {
    private val enemy = Troop(unitName = "Guard", amount = 1, speed = 3, id = 20)
    private val secondEnemy = Troop(unitName = "Second guard", amount = 1, speed = 3, id = 21)
    private val context = BattleMovementContext { it.id == 20 || it.id == 21 }

    private fun tile(x: Int, y: Int = 0) = FakeBattleTile(Vector2(x.toFloat(), y.toFloat()))

    private fun connect(a: FakeBattleTile, b: FakeBattleTile) {
        a.addNeighbor(b)
        b.addNeighbor(a)
    }

    /** Six adjacent hexes around a guard, including the edges between ring neighbors. */
    private fun ring(): List<FakeBattleTile> {
        val center = tile(0, 0).apply { setTroop(enemy) }
        val ring =
            listOf(tile(1, 0), tile(1, 1), tile(0, 1), tile(-1, 0), tile(-1, -1), tile(0, -1))
        ring.forEachIndexed { index, current ->
            connect(center, current)
            connect(current, ring[(index + 1) % ring.size])
        }
        return ring
    }

    @Test
    fun `passing two hexes around one guard costs four movement points`() {
        val ring = ring()
        val reachable = MovementRangeUseCase.execute(ring[0], 4f, context)
        assertTrue(reachable.containsKey(ring[2]))
        assertEquals(4f, reachable.getValue(ring[2]).totalDistance, 0f)
        assertFalse(reachable.containsKey(ring[3]))
        assertEquals(listOf(ring[1], ring[2]), reachable.getPathToTile(ring[2]))
    }

    @Test
    fun `one remaining point cannot pay for a second controlled step`() {
        val ring = ring()
        val reachable = MovementRangeUseCase.execute(ring[0], 3f, context)
        assertTrue(reachable.containsKey(ring[1]))
        assertFalse(reachable.containsKey(ring[2]))
    }

    @Test
    fun `entry and continuation in ordinary control both cost two`() {
        val ring = ring()
        val approach = tile(2, 0)
        connect(approach, ring[0])
        val reachable = MovementRangeUseCase.execute(approach, 4f, context)
        assertEquals(2f, reachable.getValue(ring[0]).totalDistance, 0f)
        assertTrue(reachable.containsKey(ring[1]))
        assertEquals(4f, reachable.getValue(ring[1]).totalDistance, 0f)
        assertFalse(reachable.containsKey(ring[2]))
        assertFalse(MovementRangeUseCase.execute(approach, 3f, context).containsKey(ring[1]))
    }

    @Test
    fun `entering overlapping control prevents crossing it in the same action`() {
        val start = tile(0)
        val overlap = tile(1)
        val beyond = tile(2)
        connect(start, overlap)
        connect(overlap, beyond)
        connect(overlap, tile(1, 1).apply { setTroop(enemy) })
        connect(overlap, tile(1, -1).apply { setTroop(secondEnemy) })
        val reachable = MovementRangeUseCase.execute(start, 5f, context)
        assertTrue(reachable.containsKey(overlap))
        assertFalse(reachable.containsKey(beyond))
    }

    @Test
    fun `starting in reinforced control allows multiple yellow steps within budget`() {
        val start = tile(0)
        val ordinary = tile(1)
        val beyond = tile(2)
        val firstGuard = tile(0, 1).apply { setTroop(enemy) }
        connect(start, ordinary)
        connect(ordinary, beyond)
        connect(start, firstGuard)
        connect(ordinary, firstGuard)
        connect(beyond, firstGuard)
        connect(start, tile(0, -1).apply { setTroop(secondEnemy) })
        val reachable = MovementRangeUseCase.execute(start, 4f, context)
        assertEquals(2f, reachable.getValue(ordinary).totalDistance, 0f)
        assertEquals(4f, reachable.getValue(beyond).totalDistance, 0f)
        assertEquals(listOf(ordinary, beyond), reachable.getPathToTile(beyond))
        assertFalse(MovementRangeUseCase.execute(start, 3.99f, context).containsKey(beyond))
        assertTrue(MovementRangeUseCase.execute(start, 4.01f, context).containsKey(beyond))
    }

    @Test
    fun `leaving reinforced control into free space allows continued movement`() {
        val start = tile(0)
        val outside = tile(1)
        val beyond = tile(2)
        connect(start, outside)
        connect(outside, beyond)
        connect(start, tile(0, 1).apply { setTroop(enemy) })
        connect(start, tile(0, -1).apply { setTroop(secondEnemy) })
        val reachable = MovementRangeUseCase.execute(start, 2f, context)
        assertEquals(1f, reachable.getValue(outside).totalDistance, 0f)
        assertEquals(2f, reachable.getValue(beyond).totalDistance, 0f)
    }

    @Test
    fun `ignoring control removes both surcharge and stopping`() {
        val ring = ring()
        val reachable = MovementRangeUseCase.execute(
            ring[0], 3f, context, considerZoneOfControl = false
        )
        assertEquals(3f, reachable.getValue(ring[3]).totalDistance, 0f)
    }

    @Test
    fun `direct exit through yellow control is cheaper than a detour through free space`() {
        val start = tile(0)
        val controlled = tile(1)
        val outside = tile(0, 2)
        val beyond = tile(2)
        val guard = tile(0, 1).apply { setTroop(enemy) }
        connect(start, controlled)
        connect(start, outside)
        connect(outside, controlled)
        connect(controlled, beyond)
        connect(start, guard)
        connect(controlled, guard)
        connect(start, tile(0, -1).apply { setTroop(secondEnemy) })
        val reachable = MovementRangeUseCase.execute(start, 3f, context)
        assertTrue(reachable.containsKey(beyond))
        assertEquals(3f, reachable.getValue(beyond).totalDistance, 0f)
        assertEquals(listOf(controlled, beyond), reachable.getPathToTile(beyond))
    }
}
