package com.unciv.testing.story

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.MapSaver
import com.unciv.pure.domain.story.*
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.*
import org.junit.Test

/** Проверяет реальные JSON-границы карты и партии отдельно от чистых переходов сюжета. */
class StoryPersistenceTest {
    private fun story() = StoryScenario(dialogues = arrayListOf(StoryDialogue(
        id = "intro", startNodeId = "first", nodes = arrayListOf(
            StoryDialogueNode(id = "first", text = "Начало", nextNodeId = "second"),
            StoryDialogueNode(id = "second", text = "Продолжение")
        )
    )))

    /** Форматы карты сохраняют сюжет; чтение не создаёт очередь и не запускает вступление. */
    @Test
    fun mapFormatsPreserveStoryDescriptions() {
        CityTestFixture().use { fixture ->
            fixture.game.tileMap.story = story()
            val previousZip = MapSaver.saveZipped
            try {
                for (zipped in listOf(false, true)) {
                    MapSaver.saveZipped = zipped
                    val restored = MapSaver.mapFromSavedString(MapSaver.mapToSavedString(fixture.game.tileMap), false)
                    assertEquals(story(), restored.story)
                    assertTrue(fixture.game.storyProgress.pending.isEmpty())
                }
            } finally { MapSaver.saveZipped = previousZip }
        }
    }

    /** JSON партии восстанавливает флаги и текущую реплику без повторного выполнения уже поставленного события. */
    @Test
    fun gameJsonPreservesPendingDialogueAndCopyIsolation() {
        CityTestFixture().use { fixture ->
            val game = fixture.game
            game.tileMap.story = story()
            game.storyProgress = StoryProgress(
                flags = linkedSetOf("met"), firedTriggers = linkedSetOf("intro-trigger"),
                pending = arrayListOf(PendingStoryEffect(
                    7, StoryEffect(StoryEffectKind.Dialogue, "intro"),
                    StoryEvent(ownerName = "Rome", heroId = "guide"), "second"
                )), nextSequence = 8, revision = 11
            )
            // Проверяем GameInfo.write/read; связывание мира после загрузки проверяется отдельно игровыми тестами.
            val loaded = json().fromJson(GameInfo::class.java, json().toJson(game))
            assertEquals(game.storyProgress, loaded.storyProgress)
            assertEquals(story(), loaded.tileMap.story)
            val copy = game.clone()
            copy.storyProgress.pending.single().nodeId = "first"
            copy.storyProgress.flags.clear()
            copy.tileMap.story!!.dialogues.single().nodes.single { it.id == "second" }.text = "Изменено"
            assertEquals("second", game.storyProgress.pending.single().nodeId)
            assertEquals(setOf("met"), game.storyProgress.flags)
            assertEquals("Продолжение", game.tileMap.story!!.dialogues.single().nodes.last().text)
        }
    }

    /** Предыдущие версии данных получают пустой прогресс и отсутствие авторского сюжета. */
    @Test
    fun oldDataHasNoStoryOrPendingEffects() {
        val oldMap = """{"mapParameters":{"mapFormatVersion":4},"tileList":[]}"""
        assertNull(MapSaver.mapFromSavedString(oldMap, false).story)
        val oldGame = """{"version":{"number":7},"cities":[],"civilizations":[]}"""
        val loaded = json().fromJson(GameInfo::class.java, oldGame)
        assertEquals(StoryProgress(), loaded.storyProgress)
    }
}
