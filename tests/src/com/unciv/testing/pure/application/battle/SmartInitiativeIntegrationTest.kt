package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattlePolicy
import com.unciv.pure.application.battle.BattleTermination
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class SmartInitiativeIntegrationTest {
    private class Scenario {
        val attackers = List(4) { Troop("Attacker", 1, 7, 0, 10, id = it + 1) }
        val defenders = List(4) { Troop("Defender", 1, 5, 0, 10, id = it + 5) }
        val expectedIds = listOf(1, 2, 3, 5, 4, 6, 7, 8)
        private val field = TacticalBattleField((0..7).map {
            TacticalBattleField.Cell(Point(it, 0), false)
        })
        val manager = BattleManager(
            Army(4).apply { attackers.forEachIndexed { index, troop -> setTroopAt(index, troop) } },
            Army(4).apply { defenders.forEachIndexed { index, troop -> setTroopAt(index, troop) } },
            field, object : IBattleRandom { override fun nextDouble() = 1.0 },
            moraleProbability = 0.0, luckProbability = 0.0
        )

        init {
            (attackers + defenders).forEachIndexed { index, troop ->
                val tile = field.getTileAt(Vector2(index.toFloat(), 0f))!!
                tile.receiveTroop(troop)
                manager.setTroopPosition(troop, tile)
            }
            manager.initializeTurnQueue()
        }
    }

    @Test
    fun `command execution follows displayed forecast across round boundaries`() {
        val s = Scenario()
        val events = mutableListOf<BattleEvent>()
        repeat(24) { turn ->
            val expected = List(8) { s.expectedIds[(turn + it) % 8] }
            assertEquals(expected, s.manager.getTurnQueue().map { it.id })
            assertEquals(expected.first(), s.manager.getCurrentTroop()!!.id)
            val result = s.manager.execute(BattleCommand.Skip(expected.first())) { events.add(it) }
            assertTrue(result.success)
            s.manager.completeAction(result)
        }
        assertEquals(List(24) { BattleEvent.TurnSkipped }, events)
        assertTrue(s.attackers.all { it.speed == 7 })
        assertTrue(s.defenders.all { it.speed == 5 })
    }

    @Test
    fun `rejected command leaves current troop and future initiative unchanged`() {
        val s = Scenario()
        val before = s.manager.getTurnQueue()
        val events = mutableListOf<BattleEvent>()
        val result = s.manager.execute(BattleCommand.Skip(-1)) { events.add(it) }
        assertFalse(result.success)
        assertTrue(events.isEmpty())
        assertEquals(before, s.manager.getTurnQueue())
        assertSame(s.attackers.first(), s.manager.getCurrentTroop())
        val skip = s.manager.execute(BattleCommand.Skip(1))
        assertTrue(skip.success)
        s.manager.completeAction(skip)
        assertEquals(listOf(2, 3, 5, 4, 6, 7, 8, 1), s.manager.getTurnQueue().map { it.id })
    }

    @Test
    fun `headless runner reproduces smart initiative for three rounds and stops at limit`() {
        fun run() = Scenario().let { s ->
            val policy = BattlePolicy { BattleCommand.Skip(it) }
            BattleSimulationRunner(
                s.manager, policy, policy, maxTurns = 24,
                maxTurnsWithoutProgress = 100, seed = 42L
            ).run()
        }
        val first = run()
        val second = run()
        assertEquals(BattleTermination.MAX_TURNS, first.termination)
        assertEquals(24, first.turns)
        assertEquals(List(3) { listOf(1, 2, 3, 5, 4, 6, 7, 8) }.flatten(),
            first.commands.map { it.troopId })
        assertEquals(List(24) { BattleEvent.TurnSkipped }, first.events)
        assertEquals(first.commands, second.commands)
        assertEquals(first.events, second.events)
        assertEquals(first.termination, second.termination)
    }

    @Test
    fun `headless runner detects stalemate after crossing round boundary`() {
        val s = Scenario()
        val policy = BattlePolicy { BattleCommand.Skip(it) }
        val result = BattleSimulationRunner(
            s.manager, policy, policy, maxTurns = 100,
            maxTurnsWithoutProgress = 9, seed = 42L
        ).run()
        assertEquals(BattleTermination.STALEMATE, result.termination)
        assertEquals(9, result.turns)
        assertEquals(s.expectedIds + 1, result.commands.map { it.troopId })
    }
}
