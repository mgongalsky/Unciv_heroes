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

class NeutralCityBattleOutcomeTest {
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

    @Test
    fun victoryOverClearedGarrisonAcquiresCityWithoutConquestEffects() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = city(fixture)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            val gold = fixture.civilization.gold
            BattleWorldOutcomeHandler(MapUnitCombatant(hero), CityCombatant(city)).apply(true)
            assertSame(fixture.civilization, city.ownerOrNull)
            assertSame(fixture.civilization, city.garrisonInfo.ownerOrNull)
            assertEquals(91, city.health)
            assertEquals(17, city.population.foodStored)
            assertEquals(setOf("Granary"), city.cityConstructions.builtBuildings)
            assertEquals(mapOf("Scout" to 23), city.cityConstructions.inProgressConstructions)
            assertEquals(gold, fixture.civilization.gold)
        }
    }

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
