package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.CalculateFormationMeleeExchangeUseCase
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class TroopSupportIntegrationTest {
    private class Scenario {
        val attacker = Troop("Swordsman", 4, 5, 10, 100, id = 1, supportBonusPercent = 25)
        val ally = Troop("Spearman", 4, 4, 10, 100, id = 2, supportBonusPercent = 25)
        val defender = Troop("Enemy", 4, 3, 10, 100, id = 3)
        val field = TacticalBattleField(
            (-3..3).flatMap { x ->
                (-3..3).map { y ->
                    TacticalBattleField.Cell(Point(x, y), false)
                }
            }
        )
        var randomCalls = 0
        val manager = BattleManager(
            Army(2).apply { setTroopAt(0, attacker); setTroopAt(1, ally) },
            Army(1).apply { setTroopAt(0, defender) }, field,
            object : IBattleRandom {
                override fun nextDouble(): Double {
                    randomCalls++; return 1.0
                }
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
    fun `adjacent allies support each other and attack deals twenty five percent more`() {
        val s = Scenario()
        assertEquals(25, s.manager.getSupportBonusPercent(s.attacker))
        assertEquals(25, s.manager.getSupportBonusPercent(s.ally))
        assertEquals(0, s.manager.getSupportBonusPercent(s.defender))
        val result = s.manager.execute(BattleCommand.Attack(1, Point(1, 0), Point(0, 0)))
        assertTrue(result.success)
        assertEquals(50, s.defender.currentHealth)
        assertEquals(60, s.attacker.currentHealth)
        assertEquals(3, s.randomCalls)
    }

    @Test
    fun `moving away removes support from both allies`() {
        val s = Scenario()
        s.place(s.defender, 3, 3)
        val result = s.manager.execute(BattleCommand.Move(1, Point(-2, 0)))
        assertTrue(result.success)
        assertEquals(0, s.manager.getSupportBonusPercent(s.attacker))
        assertEquals(0, s.manager.getSupportBonusPercent(s.ally))
    }

    @Test
    fun `dead or removed ally cannot provide support`() {
        val s = Scenario()
        s.ally.currentAmount = 0
        assertEquals(0, s.manager.getSupportBonusPercent(s.attacker))
        assertEquals(0, s.manager.getSupportBonusPercent(s.ally))
        s.manager.removeTroop(s.ally)
        assertEquals(0, s.manager.getSupportBonusPercent(s.attacker))
    }

    @Test
    fun `attack gains support from its destination without changing prospective query state`() {
        val s = Scenario()
        s.place(s.ally, 1, 1)
        s.place(s.defender, 2, 0)
        s.place(s.attacker, -1, 0)
        assertEquals(0, s.manager.getSupportBonusPercent(s.attacker))
        assertEquals(25, s.manager.getSupportBonusPercent(s.attacker, s.tile(1, 0)))
        assertSame(s.tile(-1, 0), s.manager.getTroopTile(s.attacker))
        val result = s.manager.execute(BattleCommand.Attack(1, Point(2, 0), Point(1, 0)))
        assertTrue(result.success)
        assertEquals(50, s.defender.currentHealth)
    }

    @Test
    fun `rejected attack preserves positions health and support`() {
        val s = Scenario()
        val result = s.manager.execute(BattleCommand.Attack(1, Point(0, 1), Point(0, 0)))
        assertFalse(result.success)
        assertSame(s.tile(0, 0), s.manager.getTroopTile(s.attacker))
        assertEquals(100, s.attacker.currentHealth)
        assertEquals(100, s.ally.currentHealth)
        assertEquals(25, s.manager.getSupportBonusPercent(s.attacker))
    }

    @Test
    fun `support and luck precede formation absorption`() {
        val attacker = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
            4, 10, 100, 100, 0, supportBonusPercent = 25
        )
        val defender = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
            4, 0, 100, 100, 100, formationDamageReductionPercent = 50
        )
        val result = CalculateFormationMeleeExchangeUseCase.execute(attacker, defender, true, false)
        assertEquals(50, result.defenderRemainingFormation)
        assertEquals(50, result.damageToDefender.remainingHealth)
    }

    @Test
    fun `stored retaliation already includes support and is not multiplied again`() {
        val fragile = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(1, 0, 10, 10, 0)
        val supported = CalculateFormationMeleeExchangeUseCase.TroopSnapshot(
            4, 10, 100, 100, 0, supportBonusPercent = 25
        )
        val first = CalculateFormationMeleeExchangeUseCase.execute(fragile, supported, false, false)
        assertEquals(40, first.remainingRetaliationDamage)
        val second = CalculateFormationMeleeExchangeUseCase.execute(
            fragile.copy(health = 100, maxHealth = 100), supported, false, true,
            defenderRetaliationDamage = first.remainingRetaliationDamage
        )
        assertEquals(60, second.damageToAttacker.remainingHealth)
        assertEquals(0, second.remainingRetaliationDamage)
    }

    @Test
    fun `AI chooses a supported attack position without moving or consuming randomness`() {
        val s = Scenario()
        s.place(s.attacker, -1, 0)
        s.place(s.ally, 1, 1)
        s.place(s.defender, 2, 0)
        val command = com.unciv.ai.AIBattlePolicy(s.manager).chooseCommand(s.attacker.id)
        assertTrue(command is BattleCommand.Attack)
        val attack = command as BattleCommand.Attack
        assertEquals(Point(2, 0), attack.target)
        val attackFrom = requireNotNull(attack.attackFrom) { "AI must choose an attack position" }
        val chosenTile = s.tile(attackFrom.x, attackFrom.y)
        assertEquals(25, s.manager.getSupportBonusPercent(s.attacker, chosenTile))
        assertSame(s.tile(-1, 0), s.manager.getTroopTile(s.attacker))
        assertEquals(100, s.defender.currentHealth)
        assertEquals(0, s.randomCalls)
    }

    @Test
    fun `supported AI armies finish headless battle with a repeatable transcript`() {
        fun run(): com.unciv.pure.application.battle.BattleSimulationResult {
            val s = Scenario()
            val policy = com.unciv.ai.AIBattlePolicy(s.manager)
            return com.unciv.logic.battle.BattleSimulationRunner(
                s.manager, policy, policy, maxTurns = 100, maxTurnsWithoutProgress = 20
            ).run()
        }

        val first = run()
        val second = run()
        assertEquals(com.unciv.pure.application.battle.BattleTermination.VICTORY, first.termination)
        assertEquals(true, first.winnerIsAttacker)
        assertTrue(first.turns in 2..100)
        assertEquals(first.turns, second.turns)
        assertEquals(first.commands, second.commands)
        assertEquals(first.events, second.events)
        assertTrue(first.events.last() is com.unciv.pure.application.battle.BattleEvent.BattleEnded)
        assertTrue(
            first.events[first.events.lastIndex - 1] is
                    com.unciv.pure.application.battle.BattleEvent.TroopAttacked
        )
    }
}
