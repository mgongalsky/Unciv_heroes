package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.*
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeArmy
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.*
import org.junit.Test

class FormationRecoveryCommandTest {
    private class Fixture(speed: Int = 5, morale: Boolean = false) {
        val troop = Troop(
            unitName = "Swordsman", amount = 10, speed = speed, damage = 1, maxHealth = 100,
            id = 1, formationHealthPercent = 10, formationDamageReductionPercent = 50
        ).apply { formation.current = 0 }
        val enemy = Troop(
            unitName = "Archer", amount = 10, speed = 1, damage = 0, maxHealth = 100,
            rangedStrength = 1, id = 2
        )
        val tiles = (0..7).map { FakeBattleTile(Vector2(it.toFloat(), 0f)) }
        val manager: BattleManager

        init {
            tiles.zipWithNext().forEach { (a, b) -> a.addNeighbor(b); b.addNeighbor(a) }
            val attackers = object : com.unciv.pure.domain.army.IArmy by FakeArmy(troop) {
                override fun getBattleMorale(): Int = if (morale) 3 else 0
            }
            manager = BattleManager(
                attackers, FakeArmy(enemy), FakeBattleField(tiles),
                SeededBattleRandom(42), moraleProbability = if (morale) 1.0 else 0.0,
                luckProbability = 0.0
            )
            place(troop, 0)
            place(enemy, 7)
            manager.initializeTurnQueue()
        }

        fun place(unit: Troop, index: Int) {
            manager.getTroopTile(unit)?.clearTroop()
            tiles[index].receiveTroop(unit)
            manager.setTroopPosition(unit, tiles[index])
        }

        fun move(index: Int): BattleCommandResult =
            manager.execute(BattleCommand.Move(troop.id, Point(index, 0)))

        fun nextTroopActivation() {
            manager.advanceTurn()
            assertSame(enemy, manager.getCurrentTroop())
            manager.advanceTurn()
            assertSame(troop, manager.getCurrentTroop())
        }
    }

    @Test
    fun `short withdrawal keeps old fifteen percent and next skip fully restores before event`() {
        val f = Fixture()
        val moved = f.move(1)
        assertTrue(moved.success)
        assertEquals(15, f.troop.formation.current)
        assertTrue(f.manager.hasFormationRecoveryChance(f.troop))
        assertFalse(f.manager.canFullyRestoreFormation(f.troop))
        f.nextTroopActivation()
        assertTrue(f.manager.canFullyRestoreFormation(f.troop))
        var observed = -1
        val result = f.manager.execute(BattleCommand.Skip(f.troop.id)) {
            observed = f.troop.formation.current
        }
        assertTrue(result.success)
        assertEquals(100, observed)
        assertFalse(f.manager.hasFormationRecoveryChance(f.troop))
    }

    @Test
    fun `ordinary unprepared skip preserves quarter recovery`() {
        val f = Fixture()
        assertTrue(f.manager.execute(BattleCommand.Skip(f.troop.id)).success)
        assertEquals(25, f.troop.formation.current)
    }

    @Test
    fun `exact half movement preserves formation and prepares full recovery`() {
        val f = Fixture(speed = 4)
        assertTrue(f.move(2).success)
        assertEquals(0, f.troop.formation.current)
        assertTrue(f.manager.hasFormationRecoveryChance(f.troop))
        f.nextTroopActivation()
        f.manager.execute(BattleCommand.Skip(f.troop.id))
        assertEquals(100, f.troop.formation.current)
    }

    @Test
    fun `zero damage shot cancels recovery before observers see shot`() {
        val f = Fixture()
        f.move(1)
        f.manager.advanceTurn()
        var observedChance = true
        val shot = f.manager.execute(BattleCommand.Shoot(f.enemy.id, Point(1, 0))) {
            observedChance = f.manager.hasFormationRecoveryChance(f.troop)
        }
        assertTrue(shot.success)
        assertFalse(observedChance)
        assertEquals(15, f.troop.formation.current)
        f.manager.advanceTurn()
        f.manager.execute(BattleCommand.Skip(f.troop.id))
        assertEquals(40, f.troop.formation.current)
    }

    @Test
    fun `rejected command preserves prepared recovery`() {
        val f = Fixture()
        f.move(1)
        f.nextTroopActivation()
        val result = f.manager.execute(BattleCommand.Move(f.troop.id, Point(99, 0)))
        assertFalse(result.success)
        assertTrue(f.manager.canFullyRestoreFormation(f.troop))
        assertEquals(15, f.troop.formation.current)
    }

    @Test
    fun `neighboring enemy prevents full recovery while ordinary skip still works`() {
        val f = Fixture()
        f.move(1)
        f.place(f.enemy, 2)
        f.nextTroopActivation()
        assertFalse(f.manager.hasFormationRecoveryChance(f.troop))
        f.manager.execute(BattleCommand.Skip(f.troop.id))
        assertEquals(40, f.troop.formation.current)
    }

    @Test
    fun `AI chooses prepared skip without executing recovery`() {
        val f = Fixture()
        f.move(1)
        f.nextTroopActivation()
        assertEquals(
            BattleCommand.Skip(f.troop.id),
            AIBattlePolicy(f.manager).chooseCommand(f.troop.id)
        )
        assertEquals(15, f.troop.formation.current)
        assertTrue(f.manager.canFullyRestoreFormation(f.troop))
    }

    @Test
    fun `headless runner restores on next activation and respects turn limit deterministically`() {
        fun run(): Pair<BattleSimulationResult, Int> {
            val f = Fixture()
            var actions = 0
            val policy = BattlePolicy { id ->
                if (actions++ == 0) BattleCommand.Move(id, Point(1, 0)) else BattleCommand.Skip(id)
            }
            val result = BattleSimulationRunner(
                f.manager, policy, BattlePolicy { BattleCommand.Skip(it) },
                maxTurns = 3, maxTurnsWithoutProgress = 10, seed = 42L
            ).run()
            return result to f.troop.formation.current
        }
        val (first, formation) = run()
        val (second, secondFormation) = run()
        assertEquals(100, formation)
        assertEquals(formation, secondFormation)
        assertEquals(BattleTermination.MAX_TURNS, first.termination)
        assertEquals(first.commands, second.commands)
        assertEquals(first.events, second.events)
        assertEquals(
            listOf("TroopMoved", "TurnSkipped", "TurnSkipped"),
            first.events.map { it.javaClass.simpleName })
    }

    @Test
    fun `morale allows second half step but skip heals only next ordinary activation`() {
        val f = Fixture(morale = true)
        val first = f.move(2)
        assertTrue(first.success)
        assertTrue(first.isMorale)
        f.manager.completeAction(first)
        assertSame(f.troop, f.manager.getCurrentTroop())
        val second = f.move(4)
        assertTrue(second.success)
        assertTrue(second.isMorale)
        f.manager.completeAction(second)
        assertTrue(f.manager.hasFormationRecoveryChance(f.troop))
        val skip = f.manager.execute(BattleCommand.Skip(f.troop.id))
        assertEquals(0, f.troop.formation.current)
        assertFalse(skip.isMorale)
        f.manager.completeAction(skip)
        assertSame(f.enemy, f.manager.getCurrentTroop())
        f.manager.completeAction(f.manager.execute(BattleCommand.Skip(f.enemy.id)))
        assertSame(f.troop, f.manager.getCurrentTroop())
        assertTrue(f.manager.canFullyRestoreFormation(f.troop))
        f.manager.execute(BattleCommand.Skip(f.troop.id))
        assertEquals(100, f.troop.formation.current)
    }

    @Test
    fun `zero damage melee cancels preparation even after enemy leaves`() {
        val f = Fixture()
        f.move(1)
        f.manager.advanceTurn()
        f.place(f.enemy, 2)
        val result = f.manager.execute(BattleCommand.Attack(f.enemy.id, Point(1, 0), Point(2, 0)))
        assertTrue(result.success)
        f.place(f.enemy, 7)
        f.manager.completeAction(result)
        assertFalse(f.manager.hasFormationRecoveryChance(f.troop))
        assertFalse(f.manager.canFullyRestoreFormation(f.troop))
        f.manager.execute(BattleCommand.Skip(f.troop.id))
        assertEquals(40, f.troop.formation.current)
    }

    @Test
    fun `path cost rather than geometric distance controls preparation`() {
        val f = Fixture(speed = 5)
        // The target is geometrically adjacent but the only available route takes three steps.
        f.tiles[3].position.set(0f, 1f)
        assertTrue(f.manager.execute(BattleCommand.Move(f.troop.id, Point(0, 1))).success)
        assertEquals(15, f.troop.formation.current)
        assertFalse(f.manager.hasFormationRecoveryChance(f.troop))
    }

    @Test
    fun `queue reinitialization clears readiness and morale state`() {
        val f = Fixture(morale = true)
        f.move(1)
        assertTrue(f.manager.hasFormationRecoveryChance(f.troop))
        f.manager.initializeTurnQueue()
        assertFalse(f.manager.hasFormationRecoveryChance(f.troop))
        f.manager.execute(BattleCommand.Skip(f.troop.id))
        assertEquals(25, f.troop.formation.current)
    }

    @Test
    fun `withdrawal from adjacent enemy prepares recovery after an earlier incoming attack`() {
        val f = Fixture(speed = 3)
        f.place(f.enemy, 0)
        f.place(f.troop, 1)
        // Restore occupancy after moving the enemy into the troop's former tile.
        f.tiles[0].receiveTroop(f.enemy)
        f.manager.advanceTurn()
        val attack = f.manager.execute(BattleCommand.Attack(f.enemy.id, Point(1, 0), Point(0, 0)))
        assertTrue(attack.success)
        f.manager.completeAction(attack)
        assertSame(f.troop, f.manager.getCurrentTroop())
        val move = f.move(2)
        assertTrue(move.success)
        assertTrue(f.manager.hasFormationRecoveryChance(f.troop))
        f.manager.completeAction(move)
        f.manager.completeAction(f.manager.execute(BattleCommand.Skip(f.enemy.id)))
        assertTrue(f.manager.canFullyRestoreFormation(f.troop))
        f.manager.execute(BattleCommand.Skip(f.troop.id))
        assertEquals(100, f.troop.formation.current)
    }

    @Test
    fun `horseman withdrawal spending five of ten prepares green border and restores six to forty eight`() {
        val f = Fixture(speed = 10)
        f.troop.formation = com.unciv.pure.domain.troop.Formation(current = 6, maximum = 48)
        f.place(f.troop, 1)
        f.place(f.enemy, 0)
        var chanceAtMoveEvent = false
        val moved = f.manager.execute(BattleCommand.Move(f.troop.id, Point(6, 0))) {
            chanceAtMoveEvent = f.manager.hasFormationRecoveryChance(f.troop)
        }
        assertTrue(moved.success)
        assertTrue(chanceAtMoveEvent)
        assertEquals(6, f.troop.formation.current)
        f.manager.completeAction(moved)
        assertTrue(f.manager.hasFormationRecoveryChance(f.troop))
        f.manager.completeAction(f.manager.execute(BattleCommand.Skip(f.enemy.id)))
        assertTrue(f.manager.canFullyRestoreFormation(f.troop))
        f.manager.execute(BattleCommand.Skip(f.troop.id))
        assertEquals(48, f.troop.formation.current)
        assertFalse(f.manager.hasFormationRecoveryChance(f.troop))
    }
}
