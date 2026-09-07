package com.unciv.testing.pure.application.pathfinding

import com.badlogic.gdx.math.Vector2
import com.unciv.pure.application.pathfinding.BattleMovementContext
import com.unciv.pure.application.pathfinding.MovementRangeUseCase
import com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.*
import org.junit.Test

class ZoneOfControlRouteRegressionTest {
    private var nextId = 100
    private val context = BattleMovementContext { it.unitName == "Enemy" }
    private fun tile(x: Int, y: Int = 0) = FakeBattleTile(Vector2(x.toFloat(), y.toFloat()))
    private fun connect(a: FakeBattleTile, b: FakeBattleTile) {
        a.addNeighbor(b)
        b.addNeighbor(a)
    }

    private fun guard(target: FakeBattleTile, enemy: Boolean = true): FakeBattleTile {
        val id = nextId++
        return tile(id, 10).apply {
            setTroop(Troop(unitName = if (enemy) "Enemy" else "Ally", amount = 1, id = id))
            connect(target, this)
        }
    }

    private fun line(size: Int): List<FakeBattleTile> = (0 until size).map { tile(it) }.also {
        it.zipWithNext().forEach { (a, b) -> connect(a, b) }
    }

    @Test
    fun `ten movement pays for five yellow cells even when starting in red`() {
        val path = line(7)
        path.forEach { guard(it) }
        guard(path.first())
        for (budget in 1..12) {
            val reachable = MovementRangeUseCase.execute(path.first(), budget.toFloat(), context)
            for (step in 1..6) {
                assertEquals(
                    "budget=$budget step=$step",
                    step * 2 <= budget,
                    reachable.containsKey(path[step])
                )
                if (step * 2 <= budget) {
                    assertEquals(
                        (step * 2).toFloat(),
                        reachable.getValue(path[step]).totalDistance,
                        0f
                    )
                    assertEquals(path.subList(1, step + 1), reachable.getPathToTile(path[step]))
                }
            }
        }
    }

    @Test
    fun `entering separate yellow zones from free cells has no surcharge`() {
        val path = line(5)
        guard(path[1])
        guard(path[3])
        val reachable = MovementRangeUseCase.execute(path.first(), 4f, context)
        val expected = listOf(0f, 1f, 2f, 3f, 4f)
        path.forEachIndexed { i, tile ->
            assertEquals(expected[i], reachable.getValue(tile).totalDistance, 0f)
        }
        assertEquals(path.drop(1), reachable.getPathToTile(path.last()))
        assertFalse(
            MovementRangeUseCase.execute(path.first(), 3.99f, context).containsKey(path.last())
        )
    }

    @Test
    fun `red destination stops a route even with abundant movement`() {
        val path = line(4)
        guard(path[1]); guard(path[1])
        val reachable = MovementRangeUseCase.execute(path.first(), 100f, context)
        assertTrue(reachable.containsKey(path[1]))
        assertFalse(reachable.containsKey(path[2]))
        assertFalse(reachable.containsKey(path[3]))
    }

    @Test
    fun `free detour reaches beyond red without traversing the red tile`() {
        val path = line(3)
        guard(path[1]); guard(path[1])
        val detour = tile(0, 2)
        connect(path[0], detour)
        connect(detour, path[2])
        val reachable = MovementRangeUseCase.execute(path[0], 2f, context)
        assertEquals(2f, reachable.getValue(path[2]).totalDistance, 0f)
        assertEquals(listOf(detour, path[2]), reachable.getPathToTile(path[2]))
    }

    @Test
    fun `allies block occupancy but never add control strength`() {
        val path = line(3)
        guard(path[0], enemy = false)
        guard(path[0], enemy = false)
        assertEquals(Strength.NONE, context.controlStrength(path[0]))
        val ally = Troop(unitName = "Ally", amount = 1, id = nextId++)
        path[1].setTroop(ally)
        val reachable = MovementRangeUseCase.execute(path[0], 10f, context)
        assertTrue(reachable.containsKey(path[1]))
        assertFalse(reachable.containsKey(path[2]))
        assertSame(ally, path[1].getTroop())
        assertEquals(1, ally.currentAmount)
        assertNull(path[2].getTroop())
    }

    @Test
    fun `one enemy reported twice does not create reinforced control`() {
        val start = tile(0)
        val enemyTile = guard(start)
        start.addNeighbor(enemyTile)
        guard(start, enemy = false)
        assertEquals(Strength.NORMAL, context.controlStrength(start))
    }

    @Test
    fun `dead enemies exert no control and removing guards updates routes immediately`() {
        val path = line(3)
        val first = guard(path[1])
        val second = guard(path[1])
        assertFalse(MovementRangeUseCase.execute(path[0], 3f, context).containsKey(path[2]))
        second.getTroop()!!.currentAmount = 0
        assertEquals(Strength.NORMAL, context.controlStrength(path[1]))
        assertEquals(
            2f,
            MovementRangeUseCase.execute(path[0], 2f, context).getValue(path[2]).totalDistance,
            0f
        )
        first.clearTroop()
        assertEquals(Strength.NONE, context.controlStrength(path[1]))
        assertEquals(
            2f,
            MovementRangeUseCase.execute(path[0], 2f, context).getValue(path[2]).totalDistance,
            0f
        )
    }

    @Test
    fun `ignored tiles cannot be used as a shortcut through a yellow corridor`() {
        val path = line(3)
        guard(path[1])
        val reachable =
            MovementRangeUseCase.execute(path[0], 10f, context, tilesToIgnore = hashSetOf(path[1]))
        assertFalse(reachable.containsKey(path[1]))
        assertFalse(reachable.containsKey(path[2]))
    }

    @Test
    fun `ignoring control allows crossing red but does not allow crossing occupied cells`() {
        val path = line(4)
        guard(path[1]); guard(path[1])
        val reachable =
            MovementRangeUseCase.execute(path[0], 3f, context, considerZoneOfControl = false)
        assertEquals(3f, reachable.getValue(path[3]).totalDistance, 0f)
        path[2].setTroop(Troop(unitName = "Enemy", amount = 1, id = nextId++))
        assertFalse(
            MovementRangeUseCase.execute(
                path[0], 10f, context,
                considerZoneOfControl = false
            ).containsKey(path[3])
        )
    }

    @Test
    fun `zero budget has no reachable destinations`() {
        val path = line(2)
        guard(path[0]); guard(path[0])
        assertTrue(MovementRangeUseCase.execute(path[0], 0f, context).isEmpty())
    }
}
