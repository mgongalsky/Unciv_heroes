package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattlePolicy
import com.unciv.pure.application.battle.BattleTermination
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BattleSimulationRunnerTest {
    private lateinit var attackerArmy: Army
    private lateinit var defenderArmy: Army
    private lateinit var manager: BattleManager

    @Before
    fun setUp() {
        val source = object : ITroopDefinitionSource {
            override fun getSpeed(unitName: String) = 5
            override fun getDamage(unitName: String) = 10
            override fun getMaxHealth(unitName: String) = 100
            override fun getRangedStrength(unitName: String) = 0
            override fun isSelfFeeding(unitName: String) = false
        }
        val attackerTile = FakeBattleTile(Vector2(0f, 0f))
        val defenderTile = FakeBattleTile(Vector2(1f, 0f))
        attackerTile.addNeighbor(defenderTile)
        defenderTile.addNeighbor(attackerTile)
        val attacker = TroopFactory.create("Spearman", 10, source)
        val defender = TroopFactory.create("Spearman", 2, source)
        attackerArmy = Army(1).apply { setTroopAt(0, attacker) }
        defenderArmy = Army(1).apply { setTroopAt(0, defender) }
        manager = BattleManager(
            attackerArmy, defenderArmy, FakeBattleField(listOf(attackerTile, defenderTile)),
            SeededBattleRandom(42L), moraleProbability = 0.0, luckProbability = 0.0
        )
        attackerTile.receiveTroop(attacker)
        defenderTile.receiveTroop(defender)
        manager.setTroopPosition(attacker, attackerTile)
        manager.setTroopPosition(defender, defenderTile)
        manager.initializeTurnQueue()
    }

    @Test
    fun `headless battle returns victory and ordered transcript`() {
        val attacker = attackerArmy.getAllTroops().filterNotNull().single()
        val attack = BattlePolicy { BattleCommand.Attack(it, Point(1, 0), Point(0, 0)) }
        val result = BattleSimulationRunner(manager, attack, BattlePolicy { null }).run()
        assertEquals(BattleTermination.VICTORY, result.termination)
        assertEquals(true, result.winnerIsAttacker)
        assertEquals(3, result.turns)
        assertEquals(2, result.commands.size)
        assertTrue(result.commands.all { it.troopId == attacker.id })
        assertEquals(
            listOf("TroopAttacked", "TroopAttacked", "BattleEnded"),
            result.events.map { it.javaClass.simpleName })
        val attacks = result.events.filterIsInstance<BattleEvent.TroopAttacked>()
        assertFalse(attacks.first().defenderDied)
        assertEquals(1, attacks.first().defenderRemainingAmount)
        assertTrue(attacks.last().defenderDied)
    }

    @Test
    fun `no commands terminate as stalemate`() {
        val noAction = BattlePolicy { null }
        val result = BattleSimulationRunner(
            manager, noAction, noAction,
            maxTurns = 10, maxTurnsWithoutProgress = 2
        ).run()
        assertEquals(BattleTermination.STALEMATE, result.termination)
        assertNull(result.winnerIsAttacker)
        assertEquals(2, result.turns)
        assertTrue(result.commands.isEmpty())
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `max turns protects simulation from endless skipping`() {
        val skip = BattlePolicy { BattleCommand.Skip(it) }
        val result = BattleSimulationRunner(
            manager, skip, skip,
            maxTurns = 3, maxTurnsWithoutProgress = 10, seed = 42L
        ).run()
        assertEquals(BattleTermination.MAX_TURNS, result.termination)
        assertEquals(3, result.turns)
        assertEquals(42L, result.seed)
        assertFalse(result.events.isEmpty())
    }
}
