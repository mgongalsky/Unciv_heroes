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

    @Test
    fun editedMapSurvivesStartupAcquisitionSaveAndRestart() {
        withMap(true, emptyNeutral = true) { file, fixture ->
            val map = fixture.game.tileMap
            val draft = map.scenarioCities.last().deepCopy().apply {
                name = "Edited town"
                population = 2
                foodStored = 17
                buildings.add("Granary")
            }
            assertTrue(com.unciv.logic.map.ScenarioCityEditorCommands.putCity(map, fixture.ruleset, draft))
            file.writeText(MapSaver.mapToSavedString(map.clone().apply {
                setTransients(setUnitCivTransients = false)
            }))
            val game = GameStarter.startNewGame(setup(file, true)) { }
            game.turns = 1
            val owner = game.civilizations.single()
            val town = game.cityRegistry.byId(CityId("neutral"))!!
            assertEquals("Edited town", town.name)
            game.nextTurn()
            assertNull(town.ownerOrNull)
            assertEquals(2, town.population.population)
            assertEquals(17, town.population.foodStored)
            assertEquals(setOf("Granary"), town.cityConstructions.builtBuildings)
            val hero = owner.placeUnitNearTile(com.badlogic.gdx.math.Vector2(3f, 0f), "Warrior")!!
            assertEquals(com.badlogic.gdx.math.Vector2(3f, 0f), hero.currentTile.position)
            hero.currentMovement = 2f
            hero.movement.moveToTile(town.getCenterTile())
            assertSame(owner, town.ownerOrNull)
            assertEquals("home", owner.getCapital()!!.id)
            assertEquals(17, town.population.foodStored)
            val loaded = com.unciv.logic.UncivFiles.gameInfoFromString(
                com.unciv.logic.UncivFiles.gameInfoToString(game.clone(), true))
            val restored = loaded.cityRegistry.byId(CityId("neutral"))!!
            assertEquals(owner.civName, restored.ownerOrNull!!.civName)
            assertEquals("neutral-type", restored.state.resolvedCityType)
            assertSame(restored, restored.getCenterTile().getCity())
            assertEquals(2, loaded.getCities().count())
            loaded.nextTurn()
            assertEquals(2, loaded.getCities().count())
            val restarted = GameStarter.startNewGame(setup(file, true)) { }
            assertNull(restarted.cityRegistry.byId(CityId("neutral"))!!.ownerOrNull)
            assertEquals(2, restarted.getCities().count())
            assertEquals(draft, MapSaver.loadMap(FileHandle(file)).scenarioCities.last())
        }
    }

    @Test
    fun missingCurrentCapitalExplainsFailureAndRepairStartsWithNationMarkers() {
        withMap(true) { file, fixture ->
            val map = fixture.game.tileMap
            map.scenarioPlayerSlots.single().name = "Player 1"
            map.scenarioCities.first().apply {
                name = "Dom"
                isCapital = false
                isOriginalCapital = true
            }
            map.addStartingLocation("Russia", map[com.badlogic.gdx.math.Vector2(5f, 0f)])
            map.addStartingLocation("Sweden", map[com.badlogic.gdx.math.Vector2(-5f, 0f)])
            fun parameters() = setup(file, true).apply {
                gameParameters.players = arrayListOf(
                    Player("Russia").apply { playerType = PlayerType.Human; scenarioSlotId = "first" },
                    Player("Sweden").apply { playerType = PlayerType.AI }
                )
            }
            file.writeText(MapSaver.mapToSavedString(map))
            try {
                GameStarter.startNewGame(parameters()) { }
                fail("A missing current capital must be explained")
            } catch (invalid: IllegalArgumentException) {
                assertTrue(invalid.message.orEmpty().contains("Player 1"))
                assertTrue(invalid.message.orEmpty().contains("Dom"))
                assertTrue(invalid.message.orEmpty().contains("Current capital"))
            }
            map.scenarioCities.first().isCapital = true
            file.writeText(MapSaver.mapToSavedString(map))
            val game = GameStarter.startNewGame(parameters()) { }
            val owner = game.getCivilization("Russia")
            assertEquals("home", owner.getCapital()!!.id)
            assertSame(owner, game.cityRegistry.byId(CityId("home"))!!.ownerOrNull)
            assertTrue(owner.getCivUnits().any())
            assertTrue(game.getCivilization("Sweden").getCivUnits().any())
            assertNull(game.cityRegistry.byId(CityId("neutral"))!!.ownerOrNull)
            assertEquals(2, game.getCities().count())
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
