package com.unciv.testing.story

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.story.StoryRuntime
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.story.*
import com.unciv.testing.city.CityTestFixture
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

/** Проверяет сюжетную остановку настоящего многоклеточного движения. */
class StoryMovementTest {
    private var previousInput: Input? = null

    /** Коды действий юнита требуют GDX-ввода даже при движении без интерфейса. */
    @Before
    fun prepareInput() {
        previousInput = Gdx.input
        Gdx.input = MockInput()
    }

    /** Не оставляет подмену глобального ввода другим тестам. */
    @After
    fun restoreInput() {
        Gdx.input = previousInput
    }

    private fun story() = StoryScenario(
        zones = arrayListOf(StoryZone("gate", positions = arrayListOf(CityPosition(1f, 0f)))),
        triggers = arrayListOf(StoryTrigger(id = "gate-entry", zoneId = "gate",
            effects = arrayListOf(StoryEffect(StoryEffectKind.Dialogue, "talk")))),
        dialogues = arrayListOf(StoryDialogue(id = "talk", startNodeId = "hello",
            nodes = arrayListOf(StoryDialogueNode(id = "hello", text = "Стой!", choices = arrayListOf(
                StoryChoice("leave", "Хорошо", effects = arrayListOf(StoryEffect(StoryEffectKind.GrantGold, amount = 5)))
            ))), aiChoices = linkedMapOf("hello" to "leave")))
    )

    /** Событие останавливает путь на клетке зоны, не оставляя героя вне карты и не исполняя награду до ответа. */
    @Test
    fun stopsInZoneAndContinuesAfterResponse() {
        CityTestFixture().use { fixture ->
            val game = fixture.game
            val hero = fixture.civilization.placeUnitNearTile(Vector2.Zero, "Warrior")!!
            hero.currentMovement = 10f
            game.tileMap.story = story()
            val gold = fixture.civilization.gold
            hero.movement.moveToTile(game.tileMap[Vector2(2f, 0f)])
            assertEquals(Vector2(1f, 0f), hero.currentTile.position)
            assertSame(hero, hero.currentTile.militaryUnit)
            assertEquals(1, fixture.civilization.getCivUnits().count())
            assertTrue(StoryRuntime.isBlocked(game))
            assertEquals(gold, fixture.civilization.gold)
            hero.movement.moveToTile(game.tileMap[Vector2(2f, 0f)])
            assertEquals(Vector2(1f, 0f), hero.currentTile.position)
            val progress = game.storyProgress
            StoryRuntime.respond(game, "Rome", progress.pending.first().sequence, progress.revision, "leave")
            assertEquals(gold + 5, fixture.civilization.gold)
            hero.movement.moveToTile(game.tileMap[Vector2(2f, 0f)])
            assertEquals(Vector2(2f, 0f), hero.currentTile.position)
            assertFalse(StoryRuntime.isBlocked(game))
        }
    }

    /** Неподходящий фильтр героя не прерывает маршрут и не создаёт событий для другого героя. */
    @Test
    fun unrelatedHeroCompletesRoute() {
        CityTestFixture().use { fixture ->
            val hero = fixture.civilization.placeUnitNearTile(Vector2.Zero, "Warrior")!!
            hero.currentMovement = 10f
            fixture.game.tileMap.story = story().apply { triggers.single().heroId = "someone-else" }
            hero.movement.moveToTile(fixture.game.tileMap[Vector2(2f, 0f)])
            assertEquals(Vector2(2f, 0f), hero.currentTile.position)
            assertTrue(fixture.game.storyProgress.pending.isEmpty())
        }
    }

    /** Ожидание ответа не завершает ход и не восстанавливает потраченное движение героя. */
    @Test
    fun pendingDialogueKeepsCurrentTurn() {
        CityTestFixture().use { fixture ->
            val game = fixture.game
            val hero = fixture.civilization.placeUnitNearTile(Vector2.Zero, "Warrior")!!
            hero.currentMovement = 10f
            game.tileMap.story = story()
            hero.movement.moveToTile(game.tileMap[Vector2(2f, 0f)])
            assertTrue(StoryRuntime.isBlocked(game))
            val turn = game.turns
            val player = game.currentPlayer
            val movement = hero.currentMovement
            val progress = game.storyProgress.deepCopy()
            game.nextTurn()
            assertEquals(turn, game.turns)
            assertEquals(player, game.currentPlayer)
            assertSame(fixture.civilization, game.currentPlayerCiv)
            assertEquals(movement, hero.currentMovement, 0f)
            assertEquals(progress.revision, game.storyProgress.revision)
            assertEquals(
                progress.pending.first().sequence,
                game.storyProgress.pending.first().sequence
            )
            assertSame(hero, hero.currentTile.militaryUnit)
        }
    }

    /** Остановка перед дальней целью не публикует посещение находящегося на ней объекта. */
    @Test
    fun visitRequiresReachingObject() {
        CityTestFixture().use { fixture ->
            val game = fixture.game
            val hero = fixture.civilization.placeUnitNearTile(Vector2.Zero, "Warrior")!!
            hero.currentMovement = 10f
            val destination = game.tileMap[Vector2(2f, 0f)]
            destination.improvement = "Farm"
            game.tileMap.story = story().apply {
                triggers.add(
                    StoryTrigger(
                        id = "farm-visit", event = StoryEventKind.ObjectVisited,
                        targetPosition = CityPosition(2f, 0f),
                        effects = arrayListOf(StoryEffect(StoryEffectKind.SetFlag, "visited-farm"))
                    )
                )
            }
            hero.movement.moveToTile(destination)
            assertEquals(Vector2(1f, 0f), hero.currentTile.position)
            assertFalse("farm-visit" in game.storyProgress.firedTriggers)
            val progress = game.storyProgress
            StoryRuntime.respond(
                game,
                "Rome",
                progress.pending.first().sequence,
                progress.revision,
                "leave"
            )
            assertFalse("visited-farm" in game.storyProgress.flags)
            hero.movement.moveToTile(destination)
            assertSame(destination, hero.currentTile)
            assertTrue("visited-farm" in game.storyProgress.flags)
        }
    }

    /** Обнаружение использует новую видимость героя и не раскрывает дальний скрытый объект. */
    @Test
    fun discoveryRequiresNewVisibility() {
        CityTestFixture().use { fixture ->
            val game = fixture.game
            val hero = fixture.civilization.placeUnitNearTile(Vector2.Zero, "Warrior")!!
            hero.currentMovement = 10f
            val nearby = game.tileMap[Vector2(3f, 0f)]
            val distant = game.tileMap[Vector2(6f, 0f)]
            nearby.improvement = "Farm"
            distant.improvement = "Farm"
            hero.updateVisibleTiles()
            assertFalse(nearby in hero.viewableTiles)
            assertFalse(distant in hero.viewableTiles)
            fun discovery(id: String, x: Float) = StoryTrigger(
                id = id, event = StoryEventKind.ObjectSeen,
                targetPosition = CityPosition(x, 0f),
                effects = arrayListOf(StoryEffect(StoryEffectKind.SetFlag, id))
            )
            game.tileMap.story = StoryScenario(
                triggers = arrayListOf(
                    discovery("nearby-seen", 3f), discovery("distant-seen", 6f)
                )
            )
            hero.movement.moveToTile(game.tileMap[Vector2(1f, 0f)])
            assertTrue(nearby in hero.viewableTiles)
            assertFalse(distant in hero.viewableTiles)
            assertTrue("nearby-seen" in game.storyProgress.flags)
            assertFalse("distant-seen" in game.storyProgress.firedTriggers)
            assertTrue(game.storyProgress.pending.isEmpty())
        }
    }

    /** Немодальная линейная реплика не обрывает маршрут, но удерживает передачу хода. */
    @Test
    fun nonModalLineAllowsMovementAndKeepsTurn() {
        CityTestFixture().use { fixture ->
            val game = fixture.game
            val hero = fixture.civilization.placeUnitNearTile(Vector2.Zero, "Warrior")!!
            hero.currentMovement = 10f
            game.tileMap.story = story().apply {
                dialogues.single().apply {
                    pauseWorld = false
                    nodes.single().choices.clear()
                    aiChoices.clear()
                }
            }
            hero.movement.moveToTile(game.tileMap[Vector2(2f, 0f)])
            assertEquals(Vector2(2f, 0f), hero.currentTile.position)
            assertTrue(StoryRuntime.isBlocked(game))
            assertFalse(StoryRuntime.isMapActionBlocked(game))
            hero.movement.moveToTile(game.tileMap[Vector2(3f, 0f)])
            assertEquals(Vector2(3f, 0f), hero.currentTile.position)
            assertSame(hero, hero.currentTile.militaryUnit)
            val movement = hero.currentMovement
            val turn = game.turns
            game.nextTurn()
            assertEquals(turn, game.turns)
            assertEquals(movement, hero.currentMovement, 0f)
            assertTrue(StoryRuntime.isBlocked(game))
        }
    }

    /** Создание войск не является обнаружением; движение открывает только ближнюю армию и сохраняет её ID. */
    @Test
    fun armyDiscoveryRequiresNewHeroVisibility() {
        CityTestFixture().use { fixture ->
            val game = fixture.game
            game.tileMap.story = StoryScenario(
                dialogues = arrayListOf(
                    StoryDialogue(
                        id = "seen", startNodeId = "line",
                        nodes = arrayListOf(StoryDialogueNode(id = "line", text = "Вижу войско."))
                    )
                ),
                triggers = arrayListOf(
                    StoryTrigger(
                        id = "near-army", event = StoryEventKind.ArmySeen, heroId = "scout",
                        targetPosition = CityPosition(3f, 0f),
                        effects = arrayListOf(StoryEffect(StoryEffectKind.Dialogue, "seen"))
                    ),
                    StoryTrigger(
                        id = "far-army", event = StoryEventKind.ArmySeen, heroId = "scout",
                        targetPosition = CityPosition(6f, 0f),
                        effects = arrayListOf(StoryEffect(StoryEffectKind.SetFlag, "far-seen"))
                    )
                )
            )
            val hero = fixture.civilization.placeUnitNearTile(Vector2.Zero, "Warrior")!!
            hero.scenarioHeroId = "scout"
            val nearby = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            val distant = fixture.civilization.placeUnitNearTile(Vector2(6f, 0f), "Warrior")!!
            assertEquals(Vector2(3f, 0f), nearby.currentTile.position)
            assertEquals(Vector2(6f, 0f), distant.currentTile.position)
            assertTrue(game.storyProgress.pending.isEmpty())
            assertTrue(game.storyProgress.firedTriggers.isEmpty())
            hero.updateVisibleTiles()
            assertFalse(nearby.currentTile in hero.viewableTiles)
            assertFalse(distant.currentTile in hero.viewableTiles)
            hero.currentMovement = 10f
            hero.movement.moveToTile(game.tileMap[Vector2(1f, 0f)])
            assertTrue(nearby.currentTile in hero.viewableTiles)
            assertFalse(distant.currentTile in hero.viewableTiles)
            val pending = game.storyProgress.pending.single()
            assertEquals(StoryEventKind.ArmySeen, pending.event.kind)
            assertEquals(
                com.unciv.logic.story.StoryWorldEffects.actorKey(nearby),
                pending.event.targetKey
            )
            assertEquals("scout", pending.event.heroId)
            assertFalse("far-army" in game.storyProgress.firedTriggers)
            StoryRuntime.respond(game, "Rome", pending.sequence, game.storyProgress.revision)
            hero.movement.moveToTile(game.tileMap[Vector2.Zero])
            hero.movement.moveToTile(game.tileMap[Vector2(1f, 0f)])
            assertTrue(game.storyProgress.pending.isEmpty())
            assertEquals(setOf("near-army"), game.storyProgress.firedTriggers)
        }
    }

    /** Назначенное действие не обходит обязательный ответ и не расходует движение при повторном запуске. */
    @Test
    fun assignedActionWaitsForMandatoryDialogue() {
        CityTestFixture().use { fixture ->
            val game = fixture.game
            val hero = fixture.civilization.placeUnitNearTile(Vector2.Zero, "Warrior")!!
            hero.currentMovement = 10f
            game.tileMap.story = story()
            hero.movement.moveToTile(game.tileMap[Vector2(2f, 0f)])
            assertTrue(StoryRuntime.isBlocked(game))
            val position = hero.currentTile.position.cpy()
            val movement = hero.currentMovement
            val progress = game.storyProgress.deepCopy()
            hero.action = "Explore"
            hero.doAction()
            hero.doAction()
            assertEquals(position, hero.currentTile.position)
            assertEquals(movement, hero.currentMovement, 0f)
            assertEquals(progress, game.storyProgress)
            assertSame(hero, hero.currentTile.militaryUnit)
        }
    }
}
