package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class BattleAttackPreviewTest {
    private class Scenario {
        val attacker = Troop("Swordsman", 4, 5, 10, 100, id = 1, supportBonusPercent = 25)
        val ally = Troop("Spearman", 4, 4, 10, 100, id = 2, supportBonusPercent = 25)
        val defender = Troop("Enemy", 4, 3, 10, 100, id = 3)
        val field = TacticalBattleField((-3..3).flatMap { x ->
            (-3..3).map { y -> TacticalBattleField.Cell(Point(x, y), false) }
        })
        var randomCalls = 0
        val manager = BattleManager(
            Army(2).apply { setTroopAt(0, attacker); setTroopAt(1, ally) },
            Army(1).apply { setTroopAt(0, defender) }, field,
            object : IBattleRandom {
                override fun nextDouble(): Double { randomCalls++; return 1.0 }
            }, moraleProbability = 0.0, luckProbability = 0.0
        )

        init {
            place(attacker, 0, 0)
            place(ally, 0, 1)
            place(defender, 1, 0)
            manager.initializeTurnQueue()
        }

        fun tile(x: Int, y: Int) = field.getTileAt(Vector2(x.toFloat(), y.toFloat()))!!
        fun place(troop: Troop, x: Int, y: Int) {
            manager.getTroopTile(troop)?.clearTroop()
            tile(x, y).receiveTroop(troop)
            manager.setTroopPosition(troop, tile(x, y))
        }
    }

    @Test
    fun `preview matches both sides of the executed exchange without consuming randomness`() {
        val s = Scenario()
        val queue = s.manager.getTurnQueue().toList()
        val preview = requireNotNull(s.manager.getAttackPreview(s.attacker, s.defender, s.tile(0, 0)))
        assertEquals(25, preview.attack.preparation!!.supportBonusPercent)
        assertEquals(50, preview.attack.result.damageDealt)
        assertEquals(40, preview.retaliation!!.result.damageDealt)
        assertEquals(40, preview.retaliationBudget)
        assertEquals(0, s.randomCalls)
        assertEquals(100, s.attacker.currentHealth)
        assertEquals(100, s.defender.currentHealth)
        assertEquals(queue, s.manager.getTurnQueue())
        assertTrue(s.manager.hasRetaliationRemaining(s.defender))
        assertEquals(preview, s.manager.getAttackPreview(s.attacker, s.defender, s.tile(0, 0)))
        assertTrue(s.manager.execute(BattleCommand.Attack(1, Point(1, 0), Point(0, 0))).success)
        assertEquals(preview.attack.result.remainingAmount, s.defender.currentAmount)
        assertEquals(preview.attack.result.remainingHealth, s.defender.currentHealth)
        assertEquals(preview.retaliation!!.result.remainingAmount, s.attacker.currentAmount)
        assertEquals(preview.retaliation!!.result.remainingHealth, s.attacker.currentHealth)
        assertEquals(3, s.randomCalls)
    }

    @Test
    fun `preview gains support at the approach tile without moving the attacker`() {
        val s = Scenario()
        s.place(s.ally, 1, 1)
        s.place(s.defender, 2, 0)
        s.place(s.attacker, -1, 0)
        val preview = requireNotNull(s.manager.getAttackPreview(s.attacker, s.defender, s.tile(1, 0)))
        assertEquals(25, preview.attack.preparation!!.supportBonusPercent)
        assertEquals(0, s.manager.getSupportBonusPercent(s.attacker))
        assertSame(s.tile(-1, 0), s.manager.getTroopTile(s.attacker))
        assertSame(s.attacker, s.tile(-1, 0).getTroop())
        assertNull(s.tile(1, 0).getTroop())
        assertEquals(0, s.randomCalls)
        assertTrue(s.manager.execute(BattleCommand.Attack(1, Point(2, 0), Point(1, 0))).success)
        assertEquals(preview.attack.result.remainingHealth, s.defender.currentHealth)
    }

    @Test
    fun `exhausted retaliation is shown as zero without reapplying support`() {
        val s = Scenario()
        assertTrue(s.manager.execute(BattleCommand.Attack(1, Point(1, 0), Point(0, 0))).success)
        val calls = s.randomCalls
        val preview = requireNotNull(s.manager.getAttackPreview(s.attacker, s.defender, s.tile(0, 0)))
        assertTrue(preview.usesStoredRetaliation)
        assertEquals(0, preview.retaliationBudget)
        assertEquals(0, preview.retaliation!!.result.damageDealt)
        assertNull(preview.retaliation!!.preparation)
        assertEquals(calls, s.randomCalls)
    }

    @Test
    fun `lethal attack still forecasts simultaneous retaliation`() {
        val s = Scenario()
        s.defender.currentAmount = 1
        s.defender.currentHealth = 1
        val preview = requireNotNull(s.manager.getAttackPreview(s.attacker, s.defender, s.tile(0, 0)))
        assertEquals(0, preview.attack.result.remainingAmount)
        assertEquals(10, preview.retaliation!!.result.damageDealt)
        assertTrue(s.manager.execute(BattleCommand.Attack(1, Point(1, 0), Point(0, 0))).success)
        assertEquals(preview.retaliation!!.result.remainingHealth, s.attacker.currentHealth)
    }

    @Test
    fun `ally occupied approach and missing approach have no attack preview`() {
        val s = Scenario()
        assertNull(s.manager.getAttackPreview(s.attacker, s.ally, s.tile(0, 0)))
        assertNull(s.manager.getAttackPreview(s.attacker, s.defender, s.tile(0, 1)))
        assertNull(s.manager.getAttackPreview(s.attacker, s.defender))
        assertEquals(0, s.randomCalls)
        assertEquals(100, s.attacker.currentHealth)
        assertEquals(100, s.defender.currentHealth)
    }

    @Test
    fun `normal retaliation reserve remains separate from the spent reserve without random draws`() {
        val s = Scenario()
        assertEquals(50, s.manager.getNormalRetaliationBudget(s.attacker))
        assertEquals(40, s.manager.getNormalRetaliationBudget(s.defender))
        assertEquals(40, s.manager.getRetaliationBudget(s.defender))
        assertEquals(0, s.randomCalls)
        assertTrue(s.manager.execute(BattleCommand.Attack(1, Point(1, 0), Point(0, 0))).success)
        val calls = s.randomCalls
        assertEquals(0, s.manager.getRetaliationBudget(s.defender))
        assertEquals(40, s.manager.getNormalRetaliationBudget(s.defender))
        s.defender.currentAmount = 2
        assertEquals(20, s.manager.getNormalRetaliationBudget(s.defender))
        assertEquals(0, s.manager.getRetaliationBudget(s.defender))
        s.defender.currentAmount = 0
        assertEquals(0, s.manager.getNormalRetaliationBudget(s.defender))
        assertEquals(calls, s.randomCalls)
    }
}
