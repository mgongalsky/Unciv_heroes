package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeArmy
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.TestableBattleManager
import org.junit.Assert.*
import org.junit.Test

class RangedCombatIntegrationTest {
    private class Scenario(val enemyIsRanged: Boolean = false) {
        val archer = Troop("Archer", 10, 5, 10, 100, rangedStrength = 10, id = 1)
        val enemy = Troop(
            "Enemy", 10, 4, 10, 100,
            rangedStrength = if (enemyIsRanged) 10 else 0, id = 2
        )
        val distant = Troop("Distant archer", 10, 3, 10, 100, rangedStrength = 10, id = 3)
        val start = FakeBattleTile(Vector2(0f, 0f))
        val near = FakeBattleTile(Vector2(1f, 0f))
        val far = FakeBattleTile(Vector2(2f, 0f))
        var randomCalls = 0
        val manager = TestableBattleManager(
            FakeArmy(archer), FakeArmy(enemy, distant),
            FakeBattleField(listOf(start, near, far)),
            object : IBattleRandom {
                override fun nextDouble(): Double {
                    randomCalls++
                    return 0.5
                }
            }, moraleProbability = 0.0, luckProbability = 0.0
        )

        init {
            start.addNeighbor(near)
            near.addNeighbor(start)
            near.addNeighbor(far)
            far.addNeighbor(near)
            manager.placeTroop(archer, start)
            manager.placeTroop(enemy, near)
            manager.placeTroop(distant, far)
            manager.initializeTurnQueue()
        }
    }

    @Test
    fun `adjacent enemy blocks shots at both near and distant targets without mutation`() {
        val s = Scenario()
        val events = mutableListOf<BattleEvent>()
        val queue = s.manager.getTurnQueue()
        for (target in listOf(Point(1, 0), Point(2, 0))) {
            val result = s.manager.execute(BattleCommand.Shoot(1, target)) { events.add(it) }
            assertFalse(result.success)
            assertEquals(BattleRejection.SHOOTING_BLOCKED_BY_ENEMY, result.rejection)
        }
        assertEquals(2, s.randomCalls)
        assertTrue(events.isEmpty())
        assertEquals(queue, s.manager.getTurnQueue())
        for (troop in listOf(s.archer, s.enemy, s.distant)) {
            assertEquals(10, troop.currentAmount)
            assertEquals(100, troop.currentHealth)
            assertEquals(0, troop.formation.current)
        }
        assertSame(s.start, s.manager.getTroopTile(s.archer))
        assertSame(s.enemy, s.near.getTroop())
        assertSame(s.distant, s.far.getTroop())
    }

    @Test
    fun `removing adjacent enemy restores full damage shooting at distance two`() {
        val s = Scenario()
        s.manager.removeTroop(s.enemy)
        assertTrue(s.manager.canShoot(s.archer))
        val events = mutableListOf<BattleEvent>()
        val result = s.manager.execute(BattleCommand.Shoot(1, Point(2, 0))) { events.add(it) }
        assertTrue(result.success)
        assertEquals(9, s.distant.currentAmount)
        assertEquals(100, s.distant.currentHealth)
        assertEquals(10, s.archer.currentAmount)
        assertEquals(2, s.randomCalls)
        assertTrue(events.single() is BattleEvent.TroopShot)
    }

    @Test
    fun `dead enemy and adjacent ally do not block shooting`() {
        val s = Scenario()
        s.enemy.currentAmount = 0
        assertTrue(s.manager.canShoot(s.archer))
        s.manager.removeTroop(s.enemy)
        val ally = Troop("Ally", 1, 4, 10, 100, id = 4)
        val manager = TestableBattleManager(
            FakeArmy(s.archer, ally), FakeArmy(s.distant),
            FakeBattleField(listOf(s.start, s.near, s.far)),
            object : IBattleRandom {
                override fun nextDouble() = 0.5
            }
        )
        manager.placeTroop(s.archer, s.start)
        manager.placeTroop(ally, s.near)
        manager.placeTroop(s.distant, s.far)
        assertTrue(manager.canShoot(s.archer))
    }

    @Test
    fun `archer melee attack deals half damage while melee enemy retaliates fully`() {
        val s = Scenario()
        val result = s.manager.execute(BattleCommand.Attack(1, Point(1, 0), Point(0, 0)))
        assertTrue(result.success)
        assertEquals(10, s.enemy.currentAmount)
        assertEquals(50, s.enemy.currentHealth)
        assertEquals(9, s.archer.currentAmount)
        assertEquals(100, s.archer.currentHealth)
        assertEquals(3, s.randomCalls)
    }

    @Test
    fun `archer defender retaliates with half damage`() {
        val s = Scenario(enemyIsRanged = true)
        assertTrue(s.manager.execute(BattleCommand.Attack(1, Point(1, 0), Point(0, 0))).success)
        assertEquals(10, s.archer.currentAmount)
        assertEquals(50, s.archer.currentHealth)
        assertEquals(50, s.enemy.currentHealth)
    }

    @Test
    fun `blocked AI attacks adjacent enemy instead of shooting preferred distant archer`() {
        val s = Scenario()
        val command = AIBattlePolicy(s.manager).chooseCommand(1)
        assertEquals(BattleCommand.Attack(1, Point(1, 0), Point(0, 0)), command)
        assertEquals(0, s.randomCalls)
        assertEquals(100, s.enemy.currentHealth)
        assertSame(s.start, s.manager.getTroopTile(s.archer))
    }

    @Test
    fun `blocked archers finish headless battle reproducibly using melee`() {
        fun simulate(): com.unciv.pure.application.battle.BattleSimulationResult {
            val s = Scenario()
            s.manager.removeTroop(s.distant)
            val policy = AIBattlePolicy(s.manager)
            return BattleSimulationRunner(
                s.manager, attackerPolicy = policy, defenderPolicy = policy,
                maxTurns = 100, maxTurnsWithoutProgress = 20, seed = 123L
            ).run()
        }

        val first = simulate()
        val second = simulate()
        assertEquals(false, first.winnerIsAttacker)
        assertTrue(first.turns in 2..100)
        assertTrue(first.events.any { it is BattleEvent.TroopAttacked })
        assertFalse(first.events.any { it is BattleEvent.TroopShot })
        assertTrue(first.events.last() is BattleEvent.BattleEnded)
        assertEquals(first.termination, second.termination)
        assertEquals(first.turns, second.turns)
        assertEquals(first.events, second.events)
    }
}
