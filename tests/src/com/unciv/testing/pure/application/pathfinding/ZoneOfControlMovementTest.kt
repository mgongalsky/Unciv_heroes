package com.unciv.testing.pure.application.pathfinding

import com.unciv.pure.application.pathfinding.BattleMovementContext
import com.unciv.pure.application.pathfinding.MovementRangeUseCase
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeBattleFieldBuilder
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneOfControlMovementTest {

    private val enemy = Troop(unitName = "Enemy", amount = 1, speed = 3, id = 2)
    private val ally = Troop(unitName = "Ally", amount = 1, speed = 3, id = 3)

    private val context = BattleMovementContext(isEnemy = { it.id == enemy.id })

    private fun tile(grid: Array<Array<FakeBattleTile>>, x: Int, y: Int) =
            FakeBattleFieldBuilder.tileAt(grid, x, y)

    @Test
    fun `enemy occupies the corridor but ordinary control does not block approach`() {
        val grid = FakeBattleFieldBuilder.buildGrid(10, 1)
        tile(grid, 5, 0).setTroop(enemy)
        val reachable = MovementRangeUseCase.execute(tile(grid, 0, 0), 8f, context)
        assertEquals(6, reachable.size)
        assertEquals(5, reachable.keys.count { it.getTroop() == null })
        assertTrue(reachable.containsKey(tile(grid, 4, 0)))
        // Occupied targets remain candidates for typed command rejection, never for traversal.
        assertTrue(reachable.containsKey(tile(grid, 5, 0)))
        assertFalse(reachable.containsKey(tile(grid, 6, 0)))
    }

    @Test
    fun `troop starting its turn in enemy zone of control can leave`() {
        val grid = FakeBattleFieldBuilder.buildGrid(14, 8)
        tile(grid, 8, 4).setTroop(enemy)

        val reachable = MovementRangeUseCase.execute(tile(grid, 7, 4), 3f, context)

        assertTrue(reachable.containsKey(tile(grid, 6, 4)))
        assertTrue(reachable.containsKey(tile(grid, 7, 1)))
    }

    @Test
    fun `allied troop does not exert a zone of control`() {
        val grid = FakeBattleFieldBuilder.buildGrid(14, 8)
        tile(grid, 8, 4).setTroop(ally)

        val reachable = MovementRangeUseCase.execute(tile(grid, 7, 4), 2f, context)

        assertTrue(reachable.containsKey(tile(grid, 6, 4)))
        assertTrue(reachable.containsKey(tile(grid, 7, 2)))
    }

    @Test
    fun `without an enemy predicate movement is unrestricted`() {
        val grid = FakeBattleFieldBuilder.buildGrid(14, 8)
        tile(grid, 8, 4).setTroop(enemy)

        val reachable = MovementRangeUseCase.execute(tile(grid, 7, 4), 3f, BattleMovementContext())

        assertTrue(reachable.containsKey(tile(grid, 7, 1)))
    }

    @Test
    fun `disabled zone of control allows leaving a pinned tile`() {
        val grid = FakeBattleFieldBuilder.buildGrid(14, 8)
        tile(grid, 8, 4).setTroop(enemy)

        val reachable = MovementRangeUseCase.execute(
            startTile = tile(grid, 7, 4),
            unitMovement = 3f,
            context = context,
            considerZoneOfControl = false
        )

        assertTrue(reachable.containsKey(tile(grid, 7, 1)))
    }
}
