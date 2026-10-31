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

/** Проверяет сценарные города через настоящий запуск новой игры с картой из временного файла. */
class ScenarioGameStarterTest {
    /** Готовит карту с нужным вариантом сценария и восстанавливает глобальные зависимости после проверки. */
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

    /** Изменённая карта проходит запуск, нейтральный ход, вход героя, загрузку и повторный независимый запуск. */
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
            // Пустой callback изолирует запись настроек; создание мира выполняется настоящим GameStarter.
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
            // Состояние сыгранной партии не должно попадать в исходные описания карты.
            val restarted = GameStarter.startNewGame(setup(file, true)) { }
            assertNull(restarted.cityRegistry.byId(CityId("neutral"))!!.ownerOrNull)
            assertEquals(2, restarted.getCities().count())
            assertEquals(draft, MapSaver.loadMap(FileHandle(file)).scenarioCities.last())
        }
    }

    /** Столица выбирается из старого флага или первого города; её центр приоритетнее старой стартовой метки нации. */
    @Test
    fun legacyCapitalAndMissingFlagsStartAutomaticallyWithNationMarkers() {
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
            val legacy = GameStarter.startNewGame(parameters()) { }
            assertEquals("home", legacy.getCivilization("Russia").getCapital()!!.id)
            assertTrue(legacy.cityRegistry.byId(CityId("home"))!!.isOriginalCapital)
            assertFalse(MapSaver.loadMap(FileHandle(file)).scenarioCities.first().isCapital)
            map.scenarioCities.first().isOriginalCapital = false
            file.writeText(MapSaver.mapToSavedString(map))
            val game = GameStarter.startNewGame(parameters()) { }
            val owner = game.getCivilization("Russia")
            assertEquals("home", owner.getCapital()!!.id)
            assertSame(owner, game.cityRegistry.byId(CityId("home"))!!.ownerOrNull)
            assertTrue(owner.getCivUnits().any())
            val homeArea = owner.getCapital()!!.getCenterTile().getTilesInDistance(2).toSet()
            assertTrue("Starting units must be near the city, not the old Russia marker",
                owner.getCivUnits().all { it.currentTile in homeArea })
            assertFalse(owner.getCivUnits().any { it.isCivilian() && it.hasUnique(UniqueType.FoundCity) })
            assertTrue(game.getCivilization("Sweden").getCivUnits().any())
            assertNull(game.cityRegistry.byId(CityId("neutral"))!!.ownerOrNull)
            assertEquals(2, game.getCities().count())
        }
    }

    /** Минимальные параметры запуска: один человек, без варваров и городов-государств, с явным слотом сценария. */
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

    /** Повторный старт создаёт независимые города с теми же ID и не добавляет поселенца владельцу готовой столицы. */
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

    /** Карта без сценарных городов сохраняет обычный старт с военным юнитом и основателем. */
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
    /** Уже размещённый герой регистрируется один раз; дополнительный стартовый отряд не появляется. */
    @Test
    fun preplacedHeroDoesNotReceiveAdditionalStartingUnits() {
        withMap(true, preplacedHero = true) { file, _ ->
            val game = GameStarter.startNewGame(setup(file, true)) { }
            val units = game.civilizations.single().getCivUnits().toList()
            assertEquals(1, units.size)
            assertEquals(com.badlogic.gdx.math.Vector2(1f, 0f), units.single().currentTile.position)
        }
    }

    /** Расстановка стартовых войск не должна случайно занимать пустой нейтральный город. */
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

    /** Миссия без города создаёт ровно одного сценарного героя; загрузка сохраняет личность, повторный старт берёт исходное описание. */
    @Test
    fun scenarioHeroWithoutCitySurvivesSaveAndIndependentRestart() {
        withMap(false) { file, fixture ->
            val map = fixture.game.tileMap
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("first"))
            val description = com.unciv.pure.domain.hero.ScenarioHero(
                "guide", "Проводник", "Warrior", "first", CityPosition(1f, 0f)
            )
            map.scenarioHeroes.add(description)
            file.writeText(MapSaver.mapToSavedString(map))
            val game = GameStarter.startNewGame(setup(file, true)) { }
            val owner = game.civilizations.single()
            val hero = owner.getCivUnits().single()
            assertTrue(owner.cities.isEmpty())
            assertFalse(owner.isDefeated())
            assertEquals("guide", hero.scenarioHeroId)
            assertEquals("Проводник", hero.instanceName)
            assertEquals(com.badlogic.gdx.math.Vector2(1f, 0f), hero.currentTile.position)
            hero.instanceName = "Новое имя"
            val loaded = com.unciv.logic.UncivFiles.gameInfoFromString(
                com.unciv.logic.UncivFiles.gameInfoToString(game.clone(), true)
            )
            val restored = loaded.civilizations.single().getCivUnits().single()
            assertEquals("guide", restored.scenarioHeroId)
            assertEquals("Новое имя", restored.instanceName)
            assertSame(restored, restored.currentTile.militaryUnit)
            assertEquals(hero.currentTile.position, restored.currentTile.position)
            assertEquals("Проводник", loaded.tileMap.scenarioHeroes.single().name)
            val restarted = GameStarter.startNewGame(setup(file, true)) { }
            val fresh = restarted.civilizations.single().getCivUnits().single()
            assertEquals("guide", fresh.scenarioHeroId)
            assertEquals("Проводник", fresh.instanceName)
            assertNotSame(hero, fresh)
            assertEquals(description, MapSaver.loadMap(FileHandle(file)).scenarioHeroes.single())
        }
    }

    /** Настоящий запуск публикует вступление после стартовых ресурсов; загрузка не повторяет награду. */
    @Test
    fun missionOpeningRunsOnStartupButNotOnLoad() {
        withMap(false) { file, fixture ->
            val story = com.unciv.pure.domain.story.StoryScenario(
                triggers = arrayListOf(
                    com.unciv.pure.domain.story.StoryTrigger(
                        id = "opening",
                        event = com.unciv.pure.domain.story.StoryEventKind.MissionStarted,
                        effects = arrayListOf(
                            com.unciv.pure.domain.story.StoryEffect(
                                com.unciv.pure.domain.story.StoryEffectKind.GrantGold, amount = 17
                            )
                        )
                    )
                )
            )
            val ordinary = GameStarter.startNewGame(setup(file, false)) { }
            val startingGold = ordinary.civilizations.single().gold
            fixture.game.tileMap.story = story
            file.writeText(MapSaver.mapToSavedString(fixture.game.tileMap))
            val game = GameStarter.startNewGame(setup(file, false)) { }
            assertEquals(startingGold + 17, game.civilizations.single().gold)
            assertTrue("opening" in game.storyProgress.firedTriggers)
            assertTrue(game.storyProgress.pending.isEmpty())
            val loaded = com.unciv.logic.UncivFiles.gameInfoFromString(
                com.unciv.logic.UncivFiles.gameInfoToString(game.clone(), true)
            )
            assertEquals(startingGold + 17, loaded.civilizations.single().gold)
            assertEquals(game.storyProgress, loaded.storyProgress)
        }
    }

    /** Демонстрация проходит настоящий запуск и загрузку ожидающей атаки для обоих исходов. */
    @Test
    fun storyDemoSupportsPeaceAndBattleAfterReload() {
        withMap(false) { file, fixture ->
            val demo = com.unciv.logic.story.StoryDemoScenario.create(fixture.ruleset)
            file.writeText(MapSaver.mapToSavedString(demo))
            fun respond(game: GameInfo, choice: String? = null) {
                val progress = game.storyProgress
                com.unciv.logic.story.StoryRuntime.respond(
                    game, "Rome", progress.pending.first().sequence, progress.revision, choice
                )
            }

            fun reload(game: GameInfo): GameInfo = com.unciv.logic.UncivFiles.gameInfoFromString(
                com.unciv.logic.UncivFiles.gameInfoToString(game.clone(), true)
            )
            for (choice in listOf("peace", "fight")) {
                var game = GameStarter.startNewGame(setup(file, true)) { }
                assertTrue(game.storyProgress.flags.isEmpty())
                assertEquals(1, game.civilizations.single().getCivUnits().count())
                assertEquals(
                    "guide",
                    game.civilizations.single().getCivUnits().single().scenarioHeroId
                )
                assertEquals(
                    com.unciv.pure.domain.story.StoryEffectKind.Cinematic,
                    game.storyProgress.pending.first().effect.kind
                )
                val opening = game.storyProgress
                com.unciv.logic.story.StoryRuntime.finishCinematic(
                    game, "Rome", opening.pending.first().sequence, opening.revision
                )
                respond(game)
                var hero = game.civilizations.single().getCivUnits().single()
                hero.currentMovement = 10f
                hero.movement.moveToTile(game.tileMap[com.badlogic.gdx.math.Vector2(3f, 0f)])
                assertEquals(com.badlogic.gdx.math.Vector2(2f, 0f), hero.currentTile.position)
                assertEquals("approach", game.storyProgress.pending.first().effect.reference)
                respond(game)
                hero.movement.moveToTile(game.tileMap[com.badlogic.gdx.math.Vector2(3f, 0f)])
                hero.army.fillArmy("Spearman", 50)
                hero.updateVisibleTiles()
                game.civilizations.single().updateSightAndResources()
                val gate = game.cityRegistry.byId(CityId("gate"))!!
                com.unciv.logic.battle.Battle.attack(
                    com.unciv.logic.battle.MapUnitCombatant(hero),
                    com.unciv.logic.battle.CityCombatant(gate)
                )
                assertNotNull(game.storyPendingAttack)
                assertEquals("parley", game.storyProgress.pending.first().effect.reference)
                val pending = game.storyProgress.deepCopy()
                val attack = game.storyPendingAttack!!.copy()
                game = reload(game)
                assertEquals(pending, game.storyProgress)
                assertEquals(attack, game.storyPendingAttack)
                hero = game.civilizations.single().getCivUnits().single()
                assertSame(hero, hero.currentTile.militaryUnit)
                val owner = game.civilizations.single()
                val restoredGate = game.cityRegistry.byId(CityId("gate"))!!
                val gold = owner.gold
                val attacks = hero.attacksThisTurn
                val movement = hero.currentMovement
                respond(game, "ask")
                assertEquals("answer", game.storyProgress.pending.first().nodeId)
                respond(game)
                assertEquals("offer", game.storyProgress.pending.first().nodeId)
                val command = game.storyProgress.deepCopy()
                respond(game, choice)
                assertTrue(choice in game.storyProgress.flags)
                try {
                    com.unciv.logic.story.StoryRuntime.respond(
                        game, "Rome",
                        command.pending.first().sequence, command.revision, choice
                    )
                    fail("Duplicate response must be rejected")
                } catch (_: IllegalArgumentException) {
                }
                // AI исполняет тактический бой без графического экрана; сюжетный ответ уже выбран человеком.
                if (choice == "fight") owner.playerType = PlayerType.AI
                com.unciv.logic.story.StoryBattleEvents.resume(game, "Rome")
                assertNull(game.storyPendingAttack)
                if (choice == "peace") {
                    assertEquals(gold + 17, owner.gold)
                    assertEquals(attacks, hero.attacksThisTurn)
                    assertEquals(movement, hero.currentMovement, 0f)
                    assertNull(restoredGate.ownerOrNull)
                    assertEquals(1, restoredGate.garrisonInfo.getTroopAt(0)!!.currentAmount)
                } else {
                    assertEquals(gold, owner.gold)
                    assertEquals(attacks + 1, hero.attacksThisTurn)
                    assertSame(owner, restoredGate.ownerOrNull)
                }
                val completedAttacks = hero.attacksThisTurn
                com.unciv.logic.story.StoryBattleEvents.resume(game, "Rome")
                assertEquals(completedAttacks, hero.attacksThisTurn)
                owner.playerType = PlayerType.Human
                hero.currentMovement = 10f
                hero.movement.moveToTile(game.tileMap[com.badlogic.gdx.math.Vector2(2f, 0f)])
                assertTrue("$choice-reaction" in game.storyProgress.firedTriggers)
                while (game.storyProgress.pending.isNotEmpty()) respond(game)
                val loaded = reload(game)
                assertEquals(game.storyProgress, loaded.storyProgress)
                assertEquals(owner.gold, loaded.civilizations.single().gold)
                assertNull(loaded.storyPendingAttack)
                assertEquals(demo.story, MapSaver.loadMap(FileHandle(file)).story)
            }
        }
    }

    /** Второе человеческое вступление появляется на своём ходу и не повторяется после полного восстановления мира. */
    @Test
    fun secondHumanOpeningRunsOnActualTurnTransfer() {
        withMap(false) { file, fixture ->
            val map = com.unciv.logic.story.StoryDemoScenario.create(fixture.ruleset)
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("second", "Second traveller"))
            // Раздельная видимость исключает знакомство цивилизаций и запись настроек обучения.
            map.scenarioHeroes.add(
                com.unciv.pure.domain.hero.ScenarioHero(
                    "second-guide", "Другой проводник", "Warrior", "second", CityPosition(-5f, 0f)
                )
            )
            map.story!!.triggers.add(
                com.unciv.pure.domain.story.StoryTrigger(
                    id = "second-opening",
                    event = com.unciv.pure.domain.story.StoryEventKind.MissionStarted,
                    ownerSlotId = "second",
                    once = false,
                    effects = arrayListOf(
                        com.unciv.pure.domain.story.StoryEffect(
                            com.unciv.pure.domain.story.StoryEffectKind.Dialogue, "intro"
                        )
                    )
                )
            )
            file.writeText(MapSaver.mapToSavedString(map))
            val parameters = setup(file, true).apply {
                gameParameters.players.add(Player("Greece").apply {
                    playerType = PlayerType.Human
                    scenarioSlotId = "second"
                })
            }
            var game = GameStarter.startNewGame(parameters) { }
            assertEquals("Rome", game.currentPlayer)
            assertTrue(game.storyProgress.pending.all { it.event.ownerName == "Rome" })
            var progress = game.storyProgress
            com.unciv.logic.story.StoryRuntime.finishCinematic(
                game, "Rome", progress.pending.first().sequence, progress.revision
            )
            progress = game.storyProgress
            com.unciv.logic.story.StoryRuntime.respond(
                game, "Rome", progress.pending.first().sequence, progress.revision
            )
            assertTrue(game.storyProgress.pending.isEmpty())
            game.turns = 1
            game.nextTurn()
            assertEquals("Greece", game.currentPlayer)
            assertEquals("Greece", game.storyProgress.pending.single().event.ownerName)
            val waiting = game.storyProgress.deepCopy()
            game.nextTurn()
            assertEquals("Greece", game.currentPlayer)
            assertEquals(waiting, game.storyProgress)
            game = com.unciv.logic.UncivFiles.gameInfoFromString(
                com.unciv.logic.UncivFiles.gameInfoToString(game.clone(), true)
            )
            assertEquals(waiting, game.storyProgress)
            progress = game.storyProgress
            com.unciv.logic.story.StoryRuntime.respond(
                game, "Greece", progress.pending.single().sequence, progress.revision
            )
            game.nextTurn()
            assertEquals("Rome", game.currentPlayer)
            assertTrue(game.storyProgress.pending.isEmpty())
            game.nextTurn()
            assertEquals("Greece", game.currentPlayer)
            assertTrue(game.storyProgress.pending.isEmpty())
            assertEquals(setOf("Rome", "Greece"), game.storyProgress.startedParticipants)
        }
    }
}
