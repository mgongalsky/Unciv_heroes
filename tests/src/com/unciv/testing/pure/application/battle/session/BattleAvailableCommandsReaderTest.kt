package com.unciv.testing.pure.application.battle.session

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.IBattleTile
import com.unciv.logic.battle.session.BattleAvailableCommandsReader
import com.unciv.logic.battle.session.BattleManagerSessionEngine
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class BattleAvailableCommandsReaderTest {
    private class Fixture(ranged: Boolean, adjacent: Boolean) {
        var randomCalls = 0
        val attacker = Troop("Attacker", 10, 5, 10, 100, rangedStrength = if (ranged) 10 else 0, id = 1)
        val defender = Troop("Defender", 10, 4, 10, 100, id = 2)
        val field = TacticalBattleField.rectangular(14, 8) { false }
        val manager = BattleManager(
            Army(1).apply { addTroop(attacker) }, Army(1).apply { addTroop(defender) }, field,
            object : IBattleRandom {
                override fun nextDouble(): Double { randomCalls++; return 0.5 }
            }, moraleProbability = 0.0, luckProbability = 0.0
        ).also { it.initializeBattle() }
        init {
            if (adjacent) {
                manager.getTroopTile(defender)!!.clearTroop()
                val target = requireNotNull(field.getTileAt(Vector2(0f, 0f)))
                manager.getTroopTile(attacker)!!.clearTroop()
                manager.setTroopPosition(attacker, target)
                target.receiveTroop(attacker)
                val neighbor = target.neighbors.filterIsInstance<IBattleTile>().first()
                manager.setTroopPosition(defender, neighbor)
                neighbor.receiveTroop(defender)
            }
        }
    }

    @Test
    fun everyOfferedCommandExecutesFromTheStateItDescribes() {
        for (ranged in listOf(false, true)) for (adjacent in listOf(false, true)) {
            val fixture = Fixture(ranged, adjacent)
            val commands = BattleAvailableCommandsReader.capture(fixture.manager)
            assertTrue(commands.contains(BattleCommand.Skip(1)))
            assertTrue(commands.any { it is BattleCommand.Move })
            if (ranged && !adjacent) assertTrue(commands.any { it is BattleCommand.Shoot })
            if (adjacent) {
                assertTrue(commands.any { it is BattleCommand.Attack })
                assertFalse(commands.any { it is BattleCommand.Shoot })
            }
            assertEquals(commands, BattleAvailableCommandsReader.capture(fixture.manager))
            assertEquals(0, fixture.randomCalls)
            commands.forEach { command ->
                val fresh = Fixture(ranged, adjacent)
                val result = BattleManagerSessionEngine(fresh.manager).execute(command).result
                assertTrue("Offered command rejected: $command -> $result", result.success)
            }
        }
    }

    @Test
    fun finishedBattleOffersNoCommands() {
        val fixture = Fixture(false, false)
        fixture.manager.removeTroop(fixture.defender)
        assertTrue(BattleAvailableCommandsReader.capture(fixture.manager).isEmpty())
        assertEquals(0, fixture.randomCalls)
    }
}
