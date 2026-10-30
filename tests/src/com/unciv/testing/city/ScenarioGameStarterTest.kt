package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.files.FileHandle
import com.unciv.logic.GameInfo
import com.unciv.logic.GameStarter
import com.unciv.logic.MapSaver
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.MapSizeNew
import com.unciv.models.metadata.GameParameters
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.metadata.Player
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ScenarioGameStarterTest {
    private fun withMap(scenario: Boolean, preplacedHero: Boolean = false, emptyNeutral: Boolean = false, check: (File, CityTestFixture) -> Unit) {
        val previousInput = Gdx.input
        Gdx.input = MockInput()
        val previousMonsterGame = CivilizationInfo.monsterGameInfo
        CivilizationInfo.monsterGameInfo = GameInfo()
        val setupField = GameStarter::class.java.getDeclaredField("gameSetupInfo").apply { isAccessible = true }
        val previousSetup = setupField.get(GameStarter)
        val previousZip = MapSaver.saveZipped
        val file = File.createTempFile("unciv-scenario-start-", ".json")
        try {
            CityTestFixture().use { fixture ->
                val map = fixture.game.tileMap
                map.mapParameters.mapSize = MapSizeNew(6)
                if (scenario) {
                    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("first"))
                    map.scenarioCities.add(ScenarioCity(id = "home", name = "Home",
                        owner = ScenarioCityOwner.PlayerSlot, ownerSlotId = "first",
                        population = 3, isCapital = true, isOriginalCapital = true))
                    map.scenarioCities.add(ScenarioCity(id = "neutral", name = "Neutral",
                        position = CityPosition(4f, 0f), typeMode = ScenarioCityTypeMode.Fixed,
                        fixedType = "neutral-type", garrison = arrayListOf(ScenarioCityTroop("Spearman", 5))))
                }
                if (emptyNeutral) map.scenarioCities.last().garrison.clear()
                if (preplacedHero) fixture.civilization.placeUnitNearTile(
                    com.badlogic.gdx.math.Vector2(1f, 0f), "Warrior")!!
                MapSaver.saveZipped = false
                file.writeText(MapSaver.mapToSavedString(map))
                check(file, fixture)
            }
        } finally {
            file.delete()
            setupField.set(GameStarter, previousSetup)
            MapSaver.saveZipped = previousZip
            CivilizationInfo.monsterGameInfo = previousMonsterGame
            Gdx.input = previousInput
        }
    }

    private fun setup(file: File, scenario: Boolean) = GameSetupInfo().apply {
        mapParameters.name = "Test map"
        mapFile = FileHandle(file)
        gameParameters = GameParameters().apply {
            noBarbarians = true
            numberOfCityStates = 0
            players = arrayListOf(Player("Rome").apply {
                playerType = PlayerType.Human
                if (scenario) scenarioSlotId = "first"
            })
        }
    }

    @Test
    fun scenarioStartsTwiceWithIndependentCitiesAndNoExtraSettler() {
        withMap(true) { file, _ ->
            val first = GameStarter.startNewGame(setup(file, true)) { }
            val second = GameStarter.startNewGame(setup(file, true)) { }
            for (game in listOf(first, second)) {
                val owner = game.civilizations.single()
                assertEquals(2, game.getCities().count())
                assertEquals("home", owner.getCapital()!!.id)
                assertFalse(owner.isDefeated())
                assertTrue(owner.getCivUnits().any { it.isMilitary() })
                assertFalse(owner.getCivUnits().any { it.isCivilian() && it.hasUnique(UniqueType.FoundCity) })
                assertNull(game.cityRegistry.byId(CityId("neutral"))!!.ownerOrNull)
                assertEquals(3, owner.cities.single().population.population)
            }
            assertNotSame(first.cityRegistry.byId(CityId("home")), second.cityRegistry.byId(CityId("home")))
            assertEquals(2, MapSaver.loadMap(FileHandle(file)).scenarioCities.size)
        }
    }

    @Test
    fun ordinaryMapStillStartsWithUnitsAndWithoutCities() {
        withMap(false) { file, _ ->
            val game = GameStarter.startNewGame(setup(file, false)) { }
            assertEquals(0, game.getCities().count())
            val owner = game.civilizations.single()
            assertTrue(owner.getCivUnits().any { it.hasUnique(UniqueType.FoundCity) })
            assertTrue(owner.getCivUnits().any { it.isMilitary() })
            assertFalse(owner.isDefeated())
        }
    }
    @Test
    fun preplacedHeroDoesNotReceiveAdditionalStartingUnits() {
        withMap(true, preplacedHero = true) { file, _ ->
            val game = GameStarter.startNewGame(setup(file, true)) { }
            val units = game.civilizations.single().getCivUnits().toList()
            assertEquals(1, units.size)
            assertEquals(com.badlogic.gdx.math.Vector2(1f, 0f), units.single().currentTile.position)
        }
    }

    @Test
    fun startingUnitsDoNotAcquireEmptyNeutralCity() {
        withMap(true, emptyNeutral = true) { file, _ ->
            val game = GameStarter.startNewGame(setup(file, true)) { }
            val neutral = game.cityRegistry.byId(CityId("neutral"))!!
            assertNull(neutral.ownerOrNull)
            assertTrue(neutral.garrisonInfo.getAllTroops().all { it == null })
            assertEquals(1, game.civilizations.single().cities.size)
        }
    }
}
