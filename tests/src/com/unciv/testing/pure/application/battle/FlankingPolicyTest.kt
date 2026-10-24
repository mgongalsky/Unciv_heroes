package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.logic.battle.BattleManager
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.FlankingProfile
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class FlankingPolicyTest {
    private class Scenario {
        val field = TacticalBattleField((-3..3).flatMap { x ->
            (-3..3).map { y -> TacticalBattleField.Cell(Point(x, y), false) }
        })
        val attacker = Troop("Melee", 4, 5, 10, 100, id = 1).apply { flanking = FlankingProfile() }
        val allies = listOf(2, 3).map { id ->
            Troop("Melee", 4, 4, 10, 100, id = id).apply { flanking = FlankingProfile() }
        }
        val ordinary = Troop("Ordinary", 4, 3, 10, 100, id = 10)
        val surrounded = Troop("Surrounded", 4, 3, 10, 100, id = 11)
        var randomCalls = 0
        val manager = BattleManager(
            Army(3).apply {
                setTroopAt(0, attacker)
                allies.forEachIndexed { i, troop -> setTroopAt(i + 1, troop) }
            },
            Army(2).apply { setTroopAt(0, ordinary); setTroopAt(1, surrounded) },
            field, object : IBattleRandom {
                override fun nextDouble(): Double { randomCalls++; return 1.0 }
            }, moraleProbability = 0.0, luckProbability = 0.0
        )

        init {
            place(attacker, 0, 0)
            place(ordinary, 0, 1)
            place(surrounded, 1, 0)
            val ordinaryNeighbors = manager.getTroopTile(ordinary)!!.neighbors.toSet()
            val flankingTiles = manager.getTroopTile(surrounded)!!.neighbors
                .filterIsInstance<com.unciv.logic.battle.IBattleTile>()
                .filter { it.getTroop() == null && it !in ordinaryNeighbors }
                .take(allies.size).toList()
            require(flankingTiles.size == allies.size)
            allies.zip(flankingTiles).forEach { (troop, tile) ->
                tile.receiveTroop(troop)
                manager.setTroopPosition(troop, tile)
            }
            manager.initializeTurnQueue()
        }
        fun place(troop: Troop, x: Int, y: Int) {
            val tile = field.getTileAt(Vector2(x.toFloat(), y.toFloat()))!!
            tile.receiveTroop(troop)
            manager.setTroopPosition(troop, tile)
        }
    }

    @Test
    fun `AI breaks equal target priorities in favor of flanking without executing the command`() {
        val s = Scenario()
        assertEquals(0, s.manager.getFlanking(s.attacker, s.ordinary).flankLevel)
        assertEquals(1, s.manager.getFlanking(s.attacker, s.surrounded).flankLevel)
        val command = AIBattlePolicy(s.manager).chooseCommand(s.attacker.id)
        assertTrue(command is BattleCommand.Attack)
        assertEquals(Point(1, 0), (command as BattleCommand.Attack).target)
        assertEquals(Point(0, 0), s.manager.getTroopTile(s.attacker)!!.toPoint())
        assertEquals(100, s.surrounded.currentHealth)
        assertEquals(100, s.ordinary.currentHealth)
        assertEquals(0, s.randomCalls)
    }

    @Test
    fun `without flanking property AI retains the existing tie order`() {
        val s = Scenario()
        s.attacker.flanking = null
        val command = AIBattlePolicy(s.manager).chooseCommand(s.attacker.id) as BattleCommand.Attack
        assertEquals(Point(0, 1), command.target)
        assertEquals(0, s.randomCalls)
    }
}
