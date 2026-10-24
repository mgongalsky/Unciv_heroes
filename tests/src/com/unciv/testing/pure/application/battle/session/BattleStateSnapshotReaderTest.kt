package com.unciv.testing.pure.application.battle.session

import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.session.BattleManagerSessionEngine
import com.unciv.logic.battle.session.BattleStateSnapshotReader
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class BattleStateSnapshotReaderTest {
    private class Fixture {
        var randomCalls = 0
        val attacker = Troop("Attacker", 10, 5, 10, 100, id = 1,
            formationHealthPercent = 20, formationDamageReductionPercent = 50)
        val defender = Troop("Defender", 10, 4, 10, 100, id = 2)
        val manager = BattleManager(
            Army(1).apply { addTroop(attacker) },
            Army(1).apply { addTroop(defender) },
            TacticalBattleField.rectangular(14, 8) { false },
            object : IBattleRandom {
                override fun nextDouble(): Double {
                    randomCalls++
                    return 0.5
                }
            },
            moraleProbability = 0.0, luckProbability = 0.0
        ).also { it.initializeBattle() }
    }

    @Test
    fun repeatedReadsPreserveTheStateAndDoNotDrawRandomness() {
        val f = Fixture()
        val before = BattleStateSnapshotReader.capture(f.manager)
        assertEquals(before, BattleStateSnapshotReader.capture(f.manager))
        assertEquals(0, f.randomCalls)
        assertEquals(BattleSide.ATTACKER, before.currentSide)
        assertEquals(listOf(1, 2), before.turnQueue)
        assertTrue(before.troops.all { it.position != null })
        assertEquals(listOf(BattleSide.ATTACKER, BattleSide.DEFENDER), before.troops.map { it.side })
        assertFalse(before.finished)
        assertNull(before.winner)
    }

    @Test
    fun oldSnapshotRetainsFormationAndQueueAfterMutationAndTurnAdvance() {
        val f = Fixture()
        val before = BattleStateSnapshotReader.capture(f.manager)
        val formationBefore = before.troops.first().formation
        f.attacker.formation.current = 0
        f.attacker.currentHealth = 50
        BattleManagerSessionEngine(f.manager).execute(BattleCommand.Skip(1))
        val after = BattleStateSnapshotReader.capture(f.manager)
        assertEquals(formationBefore, before.troops.first().formation)
        assertEquals(100, before.troops.first().currentHealth)
        assertEquals(1, before.currentTroopId)
        assertEquals(50, after.troops.first().currentHealth)
        assertEquals(2, after.currentTroopId)
        assertEquals(BattleSide.DEFENDER, after.currentSide)
    }

    @Test
    fun finishedBattleHasAWinnerAndNoPlayableTurn() {
        val f = Fixture()
        f.defender.currentAmount = 0
        f.manager.removeTroop(f.defender)
        val snapshot = BattleStateSnapshotReader.capture(f.manager)
        assertTrue(snapshot.finished)
        assertEquals(BattleSide.ATTACKER, snapshot.winner)
        assertNull(snapshot.currentTroopId)
        assertNull(snapshot.currentSide)
        assertTrue(snapshot.turnQueue.isEmpty())
        assertEquals(0, snapshot.remainingTurnsInRound)
        assertEquals(listOf(1), snapshot.troops.map { it.id })
    }
}
