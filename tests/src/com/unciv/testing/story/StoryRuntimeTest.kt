package com.unciv.testing.story

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.story.StoryRuntime
import com.unciv.pure.domain.story.*
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.*
import org.junit.Test

/** Проверяет исполнение очереди и выборы участников в настоящем мире без Scene2D. */
class StoryRuntimeTest {
    private fun scenario() = StoryScenario(
        dialogues = arrayListOf(StoryDialogue(
            id = "intro", startNodeId = "question",
            nodes = arrayListOf(StoryDialogueNode(id = "question", text = "Поможешь?", choices = arrayListOf(
                StoryChoice("yes", "Да", effects = arrayListOf(
                    StoryEffect(StoryEffectKind.SetFlag, "accepted"),
                    StoryEffect(StoryEffectKind.GrantGold, amount = 17)
                )), StoryChoice("no", "Нет")
            ))), aiChoices = linkedMapOf("question" to "yes")
        )),
        triggers = arrayListOf(StoryTrigger(id = "opening", event = StoryEventKind.MissionStarted,
            effects = arrayListOf(StoryEffect(StoryEffectKind.Dialogue, "intro"))))
    )

    /** Человек получает последствия только после ответа; повторный клик не выдаёт награду снова. */
    @Test
    fun humanChoiceAppliesRewardExactlyOnce() {
        CityTestFixture().use { fixture ->
            val game = fixture.game
            game.tileMap.story = scenario()
            val before = fixture.civilization.gold
            StoryRuntime.startMission(game)
            assertTrue(StoryRuntime.isBlocked(game))
            assertEquals(before, fixture.civilization.gold)
            val sequence = game.storyProgress.pending.first().sequence
            val revision = game.storyProgress.revision
            StoryRuntime.respond(game, "Rome", sequence, revision, "yes")
            assertEquals(before + 17, fixture.civilization.gold)
            assertTrue("accepted" in game.storyProgress.flags)
            assertFalse(StoryRuntime.isBlocked(game))
            try {
                StoryRuntime.respond(game, "Rome", sequence, revision, "yes")
                fail("Повторный ответ должен быть отклонён")
            } catch (_: IllegalArgumentException) { }
            StoryRuntime.startMission(game)
            assertEquals(before + 17, fixture.civilization.gold)
            assertTrue(game.storyProgress.pending.isEmpty())
        }
    }

    /** AI проходит авторский маршрут без создания экрана и получает те же последствия. */
    @Test
    fun aiUsesAuthoredChoiceWithoutUi() {
        CityTestFixture().use { fixture ->
            fixture.civilization.playerType = PlayerType.AI
            fixture.game.tileMap.story = scenario()
            val before = fixture.civilization.gold
            StoryRuntime.startMission(fixture.game)
            assertEquals(before + 17, fixture.civilization.gold)
            assertTrue("accepted" in fixture.game.storyProgress.flags)
            assertFalse(StoryRuntime.isBlocked(fixture.game))
        }
    }

    /** Чужой участник не отвечает за владельца сцены, очередь остаётся неизменной. */
    @Test
    fun anotherParticipantCannotChoose() {
        CityTestFixture().use { fixture ->
            fixture.game.tileMap.story = scenario()
            StoryRuntime.startMission(fixture.game)
            val before = fixture.game.storyProgress.deepCopy()
            try {
                StoryRuntime.respond(fixture.game, "Other", before.pending.first().sequence, before.revision, "yes")
                fail("Чужой выбор должен быть отклонён")
            } catch (_: IllegalArgumentException) { }
            assertEquals(before, fixture.game.storyProgress)
        }
    }

    /** Сериализация незавершённой сцены не выдаёт награду и сохраняет точную команду для возобновления. */
    @Test
    fun pendingChoiceSurvivesGameSerialization() {
        CityTestFixture().use { fixture ->
            fixture.game.tileMap.story = scenario()
            StoryRuntime.startMission(fixture.game)
            val before = fixture.civilization.gold
            val restored = json().fromJson(GameInfo::class.java, json().toJson(fixture.game))
            assertEquals(fixture.game.storyProgress, restored.storyProgress)
            assertEquals(before, restored.civilizations.single().gold)
            assertTrue(StoryRuntime.isBlocked(restored))
        }
    }
}
