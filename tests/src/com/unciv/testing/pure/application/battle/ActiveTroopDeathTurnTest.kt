package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class ActiveTroopDeathTurnTest {
    private class Scenario {
        val first = Troop("Fragile", 1, 10, 0, 10, id = 1)
        val next = Troop("Next", 1, 9, 0, 100, id = 2)
        val enemy = Troop("Enemy", 1, 5, 10, 100, id = 3)
        val field = TacticalBattleField((-3..3).flatMap { x ->
            (-3..3).map { y -> TacticalBattleField.Cell(Point(x, y), false) }
        })
        val target = field.getTileAt(Vector2(0f, 0f))!!
        val origin = target.neighbors.filterIsInstance<com.unciv.logic.battle.IBattleTile>().first()
        val manager = BattleManager(
            Army(2).apply { setTroopAt(0, first); setTroopAt(1, next) },
            Army(1).apply { setTroopAt(0, enemy) }, field,
            object : IBattleRandom { override fun nextDouble() = 1.0 },
            moraleProbability = 0.0, luckProbability = 0.0
        )
        init {
            target.receiveTroop(enemy)
            manager.setTroopPosition(enemy, target)
            origin.receiveTroop(first)
            manager.setTroopPosition(first, origin)
            val nextTile = field.getTileAt(Vector2(3f, 3f))!!
            nextTile.receiveTroop(next)
            manager.setTroopPosition(next, nextTile)
            manager.initializeTurnQueue()
        }
        fun fatalAttack() = manager.execute(BattleCommand.Attack(
            first.id, target.toPoint(), origin.toPoint()
        )).also {
            assertTrue(it.success)
            assertNull(manager.getTroopById(first.id))
            assertTrue(manager.isBattleOn())
        }
    }

    @Test
    fun `death to retaliation does not skip the next ally`() {
        val s = Scenario()
        s.manager.completeAction(s.fatalAttack())
        assertSame(s.next, s.manager.getCurrentTroop())
        val skip = s.manager.execute(BattleCommand.Skip(s.next.id))
        s.manager.completeAction(skip)
        assertSame(s.enemy, s.manager.getCurrentTroop())
    }

    @Test
    fun `morale of dead actor cannot suppress successor activation`() {
        val s = Scenario()
        s.manager.completeAction(s.fatalAttack().copy(isMorale = true))
        assertSame(s.next, s.manager.getCurrentTroop())
        s.manager.completeAction(BattleCommandResult(success = true))
        assertSame(s.enemy, s.manager.getCurrentTroop())
    }

    @Test
    fun `removing last active troop wraps without skipping first and resets retaliation`() {
        val s = Scenario()
        // Spend enemy retaliation, then bring it to its activation via removal of Next.
        s.manager.completeAction(s.fatalAttack())
        assertFalse(s.manager.hasRetaliationRemaining(s.enemy))
        s.manager.removeTroop(s.next)
        s.manager.advanceTurn()
        assertSame(s.enemy, s.manager.getCurrentTroop())
        assertTrue(s.manager.hasRetaliationRemaining(s.enemy))

        val wrap = Scenario()
        wrap.manager.advanceTurn()
        wrap.manager.advanceTurn()
        assertSame(wrap.enemy, wrap.manager.getCurrentTroop())
        wrap.manager.removeTroop(wrap.enemy)
        wrap.manager.advanceTurn()
        assertSame(wrap.first, wrap.manager.getCurrentTroop())
    }

    @Test
    fun `removing nonactive troop preserves normal advancement`() {
        val s = Scenario()
        s.manager.removeTroop(s.enemy)
        assertSame(s.first, s.manager.getCurrentTroop())
        s.manager.advanceTurn()
        assertSame(s.next, s.manager.getCurrentTroop())
    }

    @Test
    fun `survivors keep their order over multiple rounds after active death`() {
        val s = Scenario()
        s.manager.completeAction(s.fatalAttack())
        repeat(3) {
            for (expected in listOf(s.next, s.enemy)) {
                assertSame(expected, s.manager.getCurrentTroop())
                assertEquals(
                    listOf(expected, if (expected === s.next) s.enemy else s.next),
                    s.manager.getTurnQueue()
                )
                val result = s.manager.execute(BattleCommand.Skip(expected.id))
                assertTrue(result.success)
                s.manager.completeAction(result)
            }
        }
        assertSame(s.next, s.manager.getCurrentTroop())
        assertNull(s.manager.getTroopById(s.first.id))
    }

    @Test
    fun `last ally dying to retaliation ends battle without starting another activation`() {
        val s = Scenario()
        s.manager.removeTroop(s.next)
        val events = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()
        val result = s.manager.execute(
            BattleCommand.Attack(
                s.first.id, s.target.toPoint(), s.origin.toPoint()
            )
        ) { events.add(it) }
        assertTrue(result.success)
        assertTrue(result.battleEnded)
        assertFalse(s.manager.isBattleOn())
        assertNull(s.manager.getTroopById(s.first.id))
        assertEquals(listOf(s.enemy), s.manager.getTurnQueue())
        assertEquals(
            1,
            events.filterIsInstance<com.unciv.pure.application.battle.BattleEvent.BattleEnded>().size
        )
        assertTrue(events.last() is com.unciv.pure.application.battle.BattleEvent.BattleEnded)
        assertFalse(s.manager.hasRetaliationRemaining(s.enemy))

        s.manager.completeAction(result)

        assertSame(s.enemy, s.manager.getCurrentTroop())
        // Beginning another activation would incorrectly restore this spent budget.
        assertFalse(s.manager.hasRetaliationRemaining(s.enemy))
        assertFalse(s.manager.isBattleOn())
    }
}
