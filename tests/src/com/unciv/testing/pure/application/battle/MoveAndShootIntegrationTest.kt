package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.*
import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeArmy
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.*
import org.junit.Test

class MoveAndShootIntegrationTest {
    private class Fixture(
        ranged: Boolean = true,
        val morale: Boolean = false,
        random: IBattleRandom = SeededBattleRandom(42)
    ) {
        val archer = Troop(
            "Archer", 10, 4, 10, 100,
            rangedStrength = if (ranged) 10 else 0, id = 1,
            formationHealthPercent = 10, formationDamageReductionPercent = 50
        )
            .apply { formation.current = 0 }
        val enemy = Troop("Enemy", 10, 1, 10, 100, id = 2)
        val tiles = (0..8).map { FakeBattleTile(Vector2(it.toFloat(), 0f)) }
        val manager: BattleManager

        init {
            tiles.zipWithNext().forEach { (a, b) -> a.addNeighbor(b); b.addNeighbor(a) }
            val army = object : IArmy by FakeArmy(archer) {
                override fun getBattleMorale() = if (morale) 3 else 0
            }
            manager = BattleManager(
                army, FakeArmy(enemy), FakeBattleField(tiles), random,
                moraleProbability = if (morale) 1.0 else 0.0, luckProbability = 0.0
            )
            place(archer, 0)
            place(enemy, 8)
            manager.initializeTurnQueue()
        }

        fun place(troop: Troop, index: Int) {
            manager.getTroopTile(troop)?.clearTroop()
            manager.setTroopPosition(troop, tiles[index])
            tiles[index].receiveTroop(troop)
        }

        fun move(index: Int) = manager.execute(BattleCommand.Move(1, Point(index, 0)))
        fun shoot() =
            manager.execute(BattleCommand.Shoot(1, manager.getTroopTile(enemy)!!.toPoint()))

        fun health(troop: Troop) = (troop.currentAmount - 1) * troop.maxHealth + troop.currentHealth
    }

    @Test
    fun `half move keeps archer without morale and one half damage shot ends activation`() {
        val f = Fixture()
        var readyAtEvent = false
        val moved = f.manager.execute(BattleCommand.Move(1, Point(2, 0))) {
            readyAtEvent = f.manager.hasPendingFollowUpShot(f.archer)
        }
        assertTrue(moved.success)
        assertTrue(moved.hasFollowUpShot)
        assertFalse(moved.isMorale)
        assertTrue(readyAtEvent)
        f.manager.completeAction(moved)
        assertSame(f.archer, f.manager.getCurrentTroop())
        assertEquals(0, f.archer.formation.current)
        val healthBefore = f.health(f.enemy)
        val shot = f.shoot()
        assertTrue(shot.success)
        assertFalse(shot.hasFollowUpShot)
        assertEquals(50, healthBefore - f.health(f.enemy))
        assertFalse(f.manager.hasPendingFollowUpShot(f.archer))
        f.manager.completeAction(shot)
        assertSame(f.enemy, f.manager.getCurrentTroop())
    }

    @Test
    fun `one step move does not restore formation before shooting`() {
        val f = Fixture()
        assertTrue(f.move(1).hasFollowUpShot)
        assertEquals(0, f.archer.formation.current)
        assertTrue(f.shoot().success)
    }

    @Test
    fun `long move and melee move end ordinary activation`() {
        val long = Fixture()
        val moved = long.move(3)
        assertTrue(moved.success)
        assertFalse(moved.hasFollowUpShot)
        long.manager.completeAction(moved)
        assertSame(long.enemy, long.manager.getCurrentTroop())
        val melee = Fixture(ranged = false)
        val meleeMove = melee.move(1)
        assertFalse(meleeMove.hasFollowUpShot)
        melee.manager.completeAction(meleeMove)
        assertSame(melee.enemy, melee.manager.getCurrentTroop())
    }

    @Test
    fun `pending shot rejects another move and melee without consuming the shot`() {
        val f = Fixture()
        f.manager.completeAction(f.move(2))
        val events = mutableListOf<BattleEvent>()
        val before = f.health(f.enemy)
        assertFalse(
            f.manager.execute(
                BattleCommand.Move(
                    1,
                    Point(3, 0)
                )
            ) { events.add(it) }.success
        )
        assertFalse(f.manager.execute(BattleCommand.Attack(1, Point(8, 0), Point(7, 0))) {
            events.add(it)
        }.success)
        assertTrue(events.isEmpty())
        assertTrue(f.manager.getReachableTiles(f.archer).isEmpty())
        assertEquals(Point(2, 0), f.manager.getTroopTile(f.archer)!!.toPoint())
        assertEquals(before, f.health(f.enemy))
        assertTrue(f.manager.hasPendingFollowUpShot(f.archer))
    }

    @Test
    fun `invalid shot preserves follow up and skip yields to enemy`() {
        val f = Fixture()
        f.manager.completeAction(f.move(2))
        assertFalse(f.manager.execute(BattleCommand.Shoot(1, Point(4, 0))).success)
        assertTrue(f.manager.hasPendingFollowUpShot(f.archer))
        val skipped = f.manager.execute(BattleCommand.Skip(1))
        assertTrue(skipped.success)
        assertFalse(f.manager.hasPendingFollowUpShot(f.archer))
        f.manager.completeAction(skipped)
        assertSame(f.enemy, f.manager.getCurrentTroop())
    }

    @Test
    fun `adjacent enemy blocks follow up and AI skips instead of attacking`() {
        val f = Fixture()
        f.place(f.enemy, 3)
        assertTrue(f.move(2).hasFollowUpShot)
        assertFalse(f.manager.canShoot(f.archer))
        val healthBefore = f.health(f.enemy)
        assertEquals(BattleRejection.SHOOTING_BLOCKED_BY_ENEMY, f.shoot().rejection)
        assertEquals(healthBefore, f.health(f.enemy))
        assertTrue(f.manager.hasPendingFollowUpShot(f.archer))
        assertEquals(BattleCommand.Skip(1), AIBattlePolicy(f.manager).chooseCommand(1))
    }

    @Test
    fun `AI chooses follow up shot without mutating battle`() {
        val f = Fixture()
        f.manager.completeAction(f.move(2))
        val before = f.health(f.enemy)
        assertEquals(
            BattleCommand.Shoot(1, Point(8, 0)),
            AIBattlePolicy(f.manager).chooseCommand(1)
        )
        assertEquals(before, f.health(f.enemy))
        assertTrue(f.manager.hasPendingFollowUpShot(f.archer))
    }

    @Test
    fun `next ordinary activation restores full shooting damage and can grant another follow up`() {
        val f = Fixture()
        f.manager.completeAction(f.move(2))
        f.manager.completeAction(f.shoot())
        f.manager.completeAction(f.manager.execute(BattleCommand.Skip(2)))
        assertSame(f.archer, f.manager.getCurrentTroop())
        val before = f.health(f.enemy)
        f.manager.completeAction(f.shoot())
        assertEquals(100, before - f.health(f.enemy))
        f.manager.completeAction(f.manager.execute(BattleCommand.Skip(2)))
        assertTrue(f.move(3).hasFollowUpShot)
    }

    @Test
    fun `queue initialization clears pending shot and damage penalty`() {
        val f = Fixture()
        f.move(2)
        f.manager.initializeTurnQueue()
        assertFalse(f.manager.hasPendingFollowUpShot(f.archer))
        val before = f.health(f.enemy)
        assertTrue(f.shoot().success)
        assertEquals(100, before - f.health(f.enemy))
    }

    @Test
    fun `movement morale survives follow up but does not grant another free shot`() {
        var calls = 0
        val f = Fixture(morale = true, random = object : IBattleRandom {
            override fun nextDouble(): Double = if (calls++ == 0) 0.0 else 1.0
        })
        val moved = f.move(2)
        assertTrue(moved.isMorale)
        f.manager.completeAction(moved)
        val shot = f.shoot()
        assertTrue(shot.isMorale)
        assertFalse(shot.hasFollowUpShot)
        f.manager.completeAction(shot)
        assertSame(f.archer, f.manager.getCurrentTroop())
        val secondMove = f.move(3)
        assertTrue(secondMove.success)
        assertFalse(secondMove.hasFollowUpShot)
        assertFalse(secondMove.isMorale)
        f.manager.completeAction(secondMove)
        assertSame(f.enemy, f.manager.getCurrentTroop())
        assertEquals(4, calls)
    }

    @Test
    fun `seeded simulation executes move and shot before enemy and reaches victory`() {
        fun run(): BattleSimulationResult {
            val f = Fixture()
            var first = true
            val ai = AIBattlePolicy(f.manager)
            val policy = BattlePolicy { id ->
                if (first) {
                    first = false
                    BattleCommand.Move(id, Point(2, 0))
                } else ai.chooseCommand(id)
            }
            return BattleSimulationRunner(
                f.manager, policy, ai,
                maxTurns = 100, maxTurnsWithoutProgress = 20, seed = 42L
            ).run()
        }

        val first = run()
        val second = run()
        assertEquals(BattleCommand.Move(1, Point(2, 0)), first.commands[0])
        assertEquals(BattleCommand.Shoot(1, Point(8, 0)), first.commands[1])
        assertEquals(BattleTermination.VICTORY, first.termination)
        assertTrue(first.turns in 3..100)
        assertTrue(first.events.last() is BattleEvent.BattleEnded)
        assertEquals(first.commands, second.commands)
        assertEquals(first.events, second.events)
        assertEquals(first.winnerIsAttacker, second.winnerIsAttacker)
    }
}
