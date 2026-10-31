package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.battle.BattleWorldOutcomeHandler
import com.unciv.logic.battle.CityCombatant
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.city.CityInfo
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattlePolicy
import com.unciv.pure.application.battle.BattleTermination
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.*
import org.junit.Test

/** Связывает настоящую боевую симуляцию с очисткой гарнизона и применением результата к городу. */
class NeutralCityBattleSimulationTest {
    /** Город передаётся только после событий победы и удаления погибших защитников из армии. */
    @Test
    fun seededBattleDefeatsOwnerlessGarrisonBeforeAcquiringCity() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            hero.army = ArmyInfo(fixture.civilization, 2).apply { addUnits("Spearman", 10) }
            val city = CityInfo().apply {
                state.ownership = CityOwnership.Neutral
                location = Vector2(4f, 0f)
                tiles.add(location)
                health = 91
                population.foodStored = 17
                garrisonInfo = ArmyInfo(null, 2).apply { addUnits("Spearman", 2) }
                bindNeutralWorld(fixture.game)
                fixture.game.cityRegistry.register(this)
                setTransients()
            }
            // Минимальное поле из двух соседних клеток исключает влияние генерации рельефа и поиска пути.
            val attackerTile = FakeBattleTile(Vector2(0f, 0f))
            val defenderTile = FakeBattleTile(Vector2(1f, 0f))
            attackerTile.addNeighbor(defenderTile)
            defenderTile.addNeighbor(attackerTile)
            val attacker = hero.army.getTroopAt(0)!!
            val defender = city.garrisonInfo.getTroopAt(0)!!
            val manager = BattleManager(
                hero.army, city.garrisonInfo, FakeBattleField(listOf(attackerTile, defenderTile)),
                SeededBattleRandom(42L), moraleProbability = 0.0, luckProbability = 0.0
            )
            attackerTile.receiveTroop(attacker)
            defenderTile.receiveTroop(defender)
            manager.setTroopPosition(attacker, attackerTile)
            manager.setTroopPosition(defender, defenderTile)
            manager.initializeTurnQueue()
            // Нападающий атакует, защитник пропускает ход: проверяется боевой механизм, а не выбор тактики AI.
            val result = BattleSimulationRunner(
                manager,
                BattlePolicy { BattleCommand.Attack(it, Point(1, 0), Point(0, 0)) },
                BattlePolicy { null },
                maxTurns = 20, maxTurnsWithoutProgress = 10, seed = 42L
            ).run()
            assertEquals(BattleTermination.VICTORY, result.termination)
            assertEquals(true, result.winnerIsAttacker)
            assertEquals(42L, result.seed)
            assertTrue(result.events.last() is BattleEvent.BattleEnded)
            assertTrue(result.events.dropLast(1).any { it is BattleEvent.TroopAttacked && it.defenderDied })
            assertNull(city.ownerOrNull)
            assertNull(city.garrisonInfo.ownerOrNull)
            // Завершение боя очищает слоты; только после этого обработчик мировой карты получает город.
            manager.finishBattle()
            assertTrue(city.garrisonInfo.getAllTroops().all { it == null })
            val gold = fixture.civilization.gold
            BattleWorldOutcomeHandler(MapUnitCombatant(hero), CityCombatant(city)).apply(result.winnerIsAttacker)
            assertSame(fixture.civilization, city.ownerOrNull)
            assertSame(fixture.civilization, city.garrisonInfo.ownerOrNull)
            assertEquals(91, city.health)
            assertEquals(17, city.population.foodStored)
            assertEquals(gold, fixture.civilization.gold)
        }
    }
}
