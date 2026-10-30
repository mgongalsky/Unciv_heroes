package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.UncivFiles
import com.unciv.logic.city.InstantiateScenarioCitiesUseCase
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test

class ScenarioCityInstantiationTest {
    private fun prepare(fixture: CityTestFixture) {
        fixture.game.tileMap.apply {
            scenarioPlayerSlots.add(ScenarioPlayerSlot("first", "First player"))
            scenarioCities.add(ScenarioCity(id = "home", name = "Home",
                owner = ScenarioCityOwner.PlayerSlot, ownerSlotId = "first",
                population = 3, foodStored = 17, buildings = arrayListOf("Granary"),
                isCapital = true, isOriginalCapital = true))
            scenarioCities.add(ScenarioCity(id = "neutral", name = "Neutral",
                position = CityPosition(4f, 0f), population = 2,
                typeMode = ScenarioCityTypeMode.Fixed, fixedType = "neutral-type",
                garrison = arrayListOf(ScenarioCityTroop("Spearman", 5))))
        }
    }

    @Test
    fun createsSpecifiedStateWithoutFoundingOrConquestEffects() {
        CityTestFixture().use { fixture ->
            prepare(fixture)
            val game = fixture.game
            val descriptions = game.tileMap.scenarioCities.map { it.deepCopy() }
            val gold = fixture.civilization.gold
            val cities = InstantiateScenarioCitiesUseCase.execute(game, mapOf("first" to fixture.civilization))
            val home = cities.first { it.id == "home" }
            val neutral = cities.first { it.id == "neutral" }
            assertEquals(2, game.getCities().count())
            assertSame(fixture.civilization, home.ownerOrNull)
            assertSame(home, fixture.civilization.getCapital())
            assertTrue(home.isOriginalCapital)
            assertFalse(fixture.civilization.isDefeated())
            assertTrue(fixture.civilization.getCivUnits().none())
            assertEquals(3, home.population.population)
            assertEquals(17, home.population.foodStored)
            assertEquals(setOf("Granary"), home.cityConstructions.builtBuildings)
            assertEquals(setOf(Vector2(0f, 0f)), home.tiles)
            assertEquals("Rome", home.state.resolvedCityType)
            assertNull(neutral.ownerOrNull)
            assertNull(neutral.garrisonInfo.ownerOrNull)
            assertEquals(5, neutral.garrisonInfo.getTroopAt(0)!!.currentAmount)
            assertEquals("neutral-type", neutral.state.resolvedCityType)
            assertSame(neutral, game.tileMap[Vector2(4f, 0f)].getCity())
            assertEquals(gold, fixture.civilization.gold)
            assertFalse(home.isInResistance())
            assertEquals(descriptions, game.tileMap.scenarioCities)
        }
    }

    @Test
    fun invalidLaterDescriptionLeavesWorldUntouched() {
        CityTestFixture().use { fixture ->
            prepare(fixture)
            fixture.game.tileMap.scenarioCities.last().buildings.add("MissingBuilding")
            try {
                InstantiateScenarioCitiesUseCase.execute(fixture.game, mapOf("first" to fixture.civilization))
                fail("Invalid scenario must be rejected")
            } catch (invalid: IllegalArgumentException) {
                assertTrue(invalid.message.orEmpty().contains("MissingBuilding"))
            }
            assertEquals(0, fixture.game.getCities().count())
            assertEquals(0, fixture.civilization.citiesCreated)
            assertFalse(fixture.game.tileMap.values.any { it.isCityCenter() })
        }
    }

    @Test
    fun saveLoadingPreservesResolvedTypesWithoutInstantiatingAgain() {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                prepare(fixture)
                InstantiateScenarioCitiesUseCase.execute(fixture.game, mapOf("first" to fixture.civilization))
                for (compressed in listOf(false, true)) {
                    val loaded = UncivFiles.gameInfoFromString(
                        UncivFiles.gameInfoToString(fixture.game.clone(), compressed))
                    assertEquals(2, loaded.getCities().count())
                    assertEquals("Rome", loaded.cityRegistry.byId(CityId("home"))!!.state.resolvedCityType)
                    assertEquals("neutral-type", loaded.cityRegistry.byId(CityId("neutral"))!!.state.resolvedCityType)
                    assertFalse(loaded.civilizations.single().isDefeated())
                }
                try {
                    InstantiateScenarioCitiesUseCase.execute(fixture.game, mapOf("first" to fixture.civilization))
                    fail("Repeated initialization must be rejected")
                } catch (invalid: IllegalArgumentException) {
                    assertTrue(invalid.message.orEmpty().contains("already exists"))
                }
                assertEquals(2, fixture.game.getCities().count())
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }
}
