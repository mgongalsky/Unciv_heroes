package com.unciv.testing.victory

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.*
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.MapSizeNew
import com.unciv.models.metadata.*
import com.unciv.pure.domain.city.*
import com.unciv.pure.domain.scenario.*
import com.unciv.pure.domain.victory.CaptureCityObjective
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Проверяет ограничения сценария и захват через настоящий запуск с временным файлом карты. */
class ScenarioVictoryStartupTest {
    private fun withScenario(check: (File, CityTestFixture) -> Unit) {
        val previousInput = Gdx.input
        val previousMonsterGame = CivilizationInfo.monsterGameInfo
        val setupField = GameStarter::class.java.getDeclaredField("gameSetupInfo").apply { isAccessible = true }
        val previousSetup = setupField.get(GameStarter)
        val previousZip = MapSaver.saveZipped
        val file = File.createTempFile("unciv-victory-start-", ".json")
        Gdx.input = MockInput()
        CivilizationInfo.monsterGameInfo = GameInfo()
        try {
            CityTestFixture().use { fixture ->
                val map = fixture.game.tileMap
                map.mapParameters.mapSize = MapSizeNew(6)
                map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one"))
                map.scenarioCities.add(ScenarioCity(id = "home", name = "Home",
                    owner = ScenarioCityOwner.PlayerSlot, ownerSlotId = "one", isCapital = true))
                map.scenarioCities.add(ScenarioCity(id = "target", name = "Fort", position = CityPosition(4f, 0f),
                    typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle"))
                map.scenarioVictorySettings = ScenarioVictorySettings(
                    rules = ArrayList(fixture.ruleset.victories.keys.map {
                        ScenarioVictoryRule(it, if (it == "CaptureCity") ScenarioVictoryMode.Required else ScenarioVictoryMode.Forbidden)
                    }),
                    captureObjectives = arrayListOf(CaptureCityObjective("target", arrayListOf("one")))
                )
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

    private fun setup(file: File) = GameSetupInfo().apply {
        mapParameters.name = "Victory test"
        mapFile = FileHandle(file)
        gameParameters = GameParameters().apply {
            noBarbarians = true
            numberOfCityStates = 0
            players = arrayListOf(Player("Rome").apply { playerType = PlayerType.Human; scenarioSlotId = "one" })
            victoryTypes = arrayListOf("Domination")
        }
    }

    /** Прямой запуск исправляет запрещённый выбор и сохраняет назначение слота без стартовой победы. */
    @Test fun directStartupEnforcesRequiredAndForbidden() {
        withScenario { file, _ ->
            val request = setup(file)
            val game = GameStarter.startNewGame(request) { }
            assertEquals(listOf("CaptureCity"), game.gameParameters.victoryTypes)
            assertEquals(mapOf("one" to "Rome"), game.scenarioVictoryParticipants)
            assertEquals("", game.victoryWinner)
            assertNull(game.getCivilization("Rome").victoryManager.getVictoryTypeAchieved())
            assertEquals(listOf("Domination"), request.gameParameters.victoryTypes)
            assertEquals(ScenarioCityOwner.Neutral, MapSaver.loadMap(FileHandle(file)).scenarioCities.last().owner)
        }
    }

    /** Реальный захват после запуска даёт результат, который переживает clone, загрузку и следующий вызов хода. */
    @Test fun captureResultSurvivesFullSaveLoadAndTurnBoundary() {
        withScenario { file, _ ->
            val game = GameStarter.startNewGame(setup(file)) { }
            val owner = game.getCivilization("Rome")
            val town = game.cityRegistry.byId(CityId("target"))!!
            val hero = owner.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            hero.currentMovement = 2f
            hero.movement.moveToTile(town.getCenterTile())
            assertEquals("Rome", game.victoryWinner)
            assertEquals("CaptureCity", game.victoryType)
            val loaded = UncivFiles.gameInfoFromString(UncivFiles.gameInfoToString(game.clone(), true))
            assertEquals("Rome", loaded.victoryWinner)
            assertEquals("CaptureCity", loaded.getCivilization("Rome").victoryManager.getVictoryTypeAchieved())
            val turn = loaded.turns
            loaded.nextTurn()
            assertEquals(turn, loaded.turns)
            val restarted = GameStarter.startNewGame(setup(file)) { }
            assertEquals("", restarted.victoryWinner)
            assertNull(restarted.cityRegistry.byId(CityId("target"))!!.ownerOrNull)
        }
    }

    /** Optional без целей можно сохранить в черновике, но включённый тип не запускает неисполняемую партию. */
    @Test fun enabledCaptureWithoutObjectivesIsRejected() {
        withScenario { file, fixture ->
            val map = fixture.game.tileMap
            map.scenarioVictorySettings = ScenarioVictorySettings(rules = arrayListOf(ScenarioVictoryRule("CaptureCity")))
            file.writeText(MapSaver.mapToSavedString(map))
            val request = setup(file).apply { gameParameters.victoryTypes = arrayListOf("CaptureCity") }
            try {
                GameStarter.startNewGame(request) { fail("Invalid startup must not save settings") }
                fail("Capture without objectives must be rejected")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("no objectives"))
            }
        }
    }
}
