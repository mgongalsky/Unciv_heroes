package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleWorldOutcomeHandler
import com.unciv.logic.battle.CityCombatant
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

/** Проверяет применение уже определённого результата тактического боя к мировой карте. */
class NeutralCityBattleOutcomeTest {
    /** Готовит город с пустым гарнизоном и прогрессом строительства для проверки последствий победы. */
    private fun city(fixture: CityTestFixture) = CityInfo().apply {
        state.ownership = CityOwnership.Neutral
        location = Vector2(4f, 0f)
        tiles.add(location)
        garrisonInfo = ArmyInfo(null, 2)
        health = 91
        population.foodStored = 17
        cityConstructions.builtBuildings.add("Granary")
        cityConstructions.constructionQueue.add("Scout")
        cityConstructions.inProgressConstructions["Scout"] = 23
        bindNeutralWorld(fixture.game)
        fixture.game.cityRegistry.register(this)
        setTransients()
    }

    /** Победа над очищенным гарнизоном передаёт город и перемещает героя без добычи и потери прогресса. */
    @Test
    fun victoryOverClearedGarrisonAcquiresCityWithoutConquestEffects() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = city(fixture)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            val gold = fixture.civilization.gold
            val previousTile = hero.currentTile
            val movement = hero.currentMovement
            val attacks = hero.attacksThisTurn
            BattleWorldOutcomeHandler(MapUnitCombatant(hero), CityCombatant(city)).apply(true)
            assertSame(city.getCenterTile(), hero.currentTile)
            assertSame(hero, city.getCenterTile().militaryUnit)
            assertNull(previousTile.militaryUnit)
            // Расход атаки относится к сессии; обработчик результата не должен списывать его повторно.
            assertEquals(movement, hero.currentMovement, 0f)
            assertEquals(attacks, hero.attacksThisTurn)
            assertSame(fixture.civilization, city.ownerOrNull)
            assertSame(fixture.civilization, city.garrisonInfo.ownerOrNull)
            assertEquals(91, city.health)
            assertEquals(17, city.population.foodStored)
            assertEquals(setOf("Granary"), city.cityConstructions.builtBuildings)
            assertEquals(mapOf("Scout" to 23), city.cityConstructions.inProgressConstructions)
            assertEquals(gold, fixture.civilization.gold)
        }
    }

    /** Одного флага победы недостаточно: живые защитники блокируют передачу города. */
    @Test
    fun reportedVictoryCannotBypassLivingGarrison() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = city(fixture)
            city.garrisonInfo.addUnits("Archer", 1)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            val history = city.state.history
            try {
                BattleWorldOutcomeHandler(MapUnitCombatant(hero), CityCombatant(city)).apply(true)
                fail("Living defenders must prevent acquisition")
            } catch (_: IllegalArgumentException) {
                assertNull(city.ownerOrNull)
                assertNull(city.garrisonInfo.ownerOrNull)
                assertEquals(history, city.state.history)
                assertSame(hero, hero.currentTile.militaryUnit)
            }
        }
    }

    /** При поражении герой удаляется из войск владельца, а город и его гарнизон остаются нейтральными. */
    @Test
    fun defeatedHeroDoesNotAcquireNeutralCity() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = city(fixture)
            city.garrisonInfo.addUnits("Archer", 1)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            BattleWorldOutcomeHandler(MapUnitCombatant(hero), CityCombatant(city)).apply(false)
            assertEquals(CityOwnership.Neutral, city.state.ownership)
            assertNull(city.ownerOrNull)
            assertNull(city.garrisonInfo.ownerOrNull)
            assertEquals(1, city.garrisonInfo.getTroopAt(0)!!.amount)
            assertFalse(hero in fixture.civilization.getCivUnits().toList())
        }
    }
}
