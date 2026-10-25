package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.battle.session.BattlePresentationReader
import com.unciv.pure.application.battle.*
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeArmy
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.*
import org.junit.Test

class MovementImmunitySimulationTest {
    private class Fixture(formed: Boolean) {
        val rider = Troop("Runner", 10, 4, 10, 100, id = 1,
            formationHealthPercent = if (formed) 10 else 0,
            formationDamageReductionPercent = if (formed) 50 else 0).apply {
            ignoresMovementFormationPenalty = formed
        }
        val enemy = Troop("Enemy", 10, 1, 10, 100, id = 2)
        val tiles = (0..8).map { FakeBattleTile(Vector2(it.toFloat(), 0f)) }
        val manager: BattleManager
        init {
            tiles.zipWithNext().forEach { (a, b) -> a.addNeighbor(b); b.addNeighbor(a) }
            manager = BattleManager(FakeArmy(rider), FakeArmy(enemy), FakeBattleField(tiles),
                SeededBattleRandom(42), moraleProbability = 0.0, luckProbability = 0.0)
            manager.setTroopPosition(rider, tiles.first())
            tiles.first().receiveTroop(rider)
            manager.setTroopPosition(enemy, tiles.last())
            tiles.last().receiveTroop(enemy)
            manager.initializeTurnQueue()
        }
    }

    @Test
    fun `troops without formation and immune riders have dark long range session highlights`() {
        for (formed in listOf(false, true)) {
            val f = Fixture(formed)
            val presentation = BattlePresentationReader.capture(f.manager, emptyList())
            assertEquals(0.5f,
                presentation.tiles.single { it.position == Point(3, 0) }.appearance.terrainAlpha, 0f)
            val range = presentation.movementRanges.single { it.troopId == 1 }
            assertTrue(range.tiles.isNotEmpty())
            assertTrue(range.tiles.all { it.style == BattleMovementPreviewUseCase.TileStyle.SAFE })
        }
    }

    @Test
    fun `immune rider keeps formation after long move and completes reproducible battle`() {
        fun simulate(): BattleSimulationResult {
            val f = Fixture(true)
            val formationBefore = f.rider.formation.current
            val ai = AIBattlePolicy(f.manager)
            var moved = false
            var checked = false
            val attackerPolicy = BattlePolicy { id ->
                if (!moved) {
                    moved = true
                    BattleCommand.Move(id, Point(3, 0))
                } else ai.chooseCommand(id)
            }
            val defenderPolicy = BattlePolicy { id ->
                if (!checked) {
                    assertEquals(formationBefore, f.rider.formation.current)
                    checked = true
                }
                ai.chooseCommand(id)
            }
            val result = BattleSimulationRunner(f.manager, attackerPolicy, defenderPolicy,
                maxTurns = 100, maxTurnsWithoutProgress = 20, seed = 42L).run()
            assertTrue(checked)
            return result
        }
        val first = simulate()
        val second = simulate()
        assertEquals(BattleCommand.Move(1, Point(3, 0)), first.commands.first())
        assertEquals(BattleTermination.VICTORY, first.termination)
        assertTrue(first.events.last() is BattleEvent.BattleEnded)
        assertEquals(first.commands, second.commands)
        assertEquals(first.events, second.events)
        assertEquals(first.winnerIsAttacker, second.winnerIsAttacker)
    }
}
