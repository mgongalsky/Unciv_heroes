package com.unciv.testing

import com.unciv.logic.HexMath
import com.unciv.logic.battle.BattleManager
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeArmy
import org.junit.Assert.*
import org.junit.Test

class BattleStartingPositionsTest {
    @Test
    fun armiesUseCompactCenteredColumnsTwoCellsFromEdges() {
        val source = HardcodedTroopDefinitionSource(
            speed = 5, damage = 10, maxHealth = 100,
            rangedStrength = 0, isSelfFeeding = false
        )
        for (attackerCount in 1..5) for (defenderCount in 1..5) {
            val attackers = List(attackerCount) { TroopFactory.create("Spearman", 10, source) }
            val defenders = List(defenderCount) { TroopFactory.create("Spearman", 10, source) }
            val field = TacticalBattleField.rectangular(14, 8) { false }
            val manager = BattleManager(
                FakeArmy(*attackers.toTypedArray()), FakeArmy(*defenders.toTypedArray()), field,
                moraleProbability = 0.0, luckProbability = 0.0
            )
            manager.initializeBattle()
            for ((troops, column) in listOf(attackers to -5f, defenders to 4f)) {
                val rows = troops.map { troop ->
                    val tile = requireNotNull(manager.getTroopTile(troop))
                    assertSame(troop, tile.getTroop())
                    val offset = HexMath.hex2EvenQCoords(tile.position)
                    assertEquals(column, offset.x, 0.001f)
                    offset.y.toInt()
                }
                assertEquals(troops.size, rows.distinct().size)
                assertTrue(rows.zipWithNext().all { (a, b) -> a - b == 1 })
                assertTrue(kotlin.math.abs(rows.average()) <= 0.5)
                if (troops.size == 5) assertEquals(listOf(2, 1, 0, -1, -2), rows)
            }
            assertEquals(attackerCount + defenderCount,
                (attackers + defenders).map { requireNotNull(manager.getTroopTile(it)).position }.distinct().size)
        }
    }
}
