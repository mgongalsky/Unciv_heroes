package com.unciv.testing.pure.application.story

import com.unciv.pure.application.story.ValidateStoryScenario
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.story.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет межобъектные связи сценария и завершимость авторского пути AI без движка. */
class ValidateStoryScenarioTest {
    private fun story() = StoryScenario(
        zones = arrayListOf(StoryZone("gate", "Ворота", arrayListOf(CityPosition(0f, 0f)))),
        dialogues = arrayListOf(StoryDialogue(
            id = "talk", startNodeId = "hello",
            nodes = arrayListOf(StoryDialogueNode(id = "hello", text = "Привет", choices = arrayListOf(
                StoryChoice("ask", "Расскажи ещё", "hello"), StoryChoice("leave", "До встречи")
            ))), aiChoices = linkedMapOf("hello" to "leave")
        )),
        triggers = arrayListOf(StoryTrigger(
            id = "entry", heroId = "hero", zoneId = "gate",
            effects = arrayListOf(StoryEffect(StoryEffectKind.Dialogue, "talk"))
        ))
    )

    private fun validate(value: StoryScenario) = ValidateStoryScenario.execute(
        value, setOf("hero"), setOf("main"), setOf("town"), setOf(CityPosition(0f, 0f))
    )

    private fun rejects(value: StoryScenario) {
        val before = value.deepCopy()
        try {
            validate(value)
            fail("Некорректный сценарий должен быть отклонён")
        } catch (_: IllegalArgumentException) { }
        assertEquals(before, value)
    }

    /** Игрок может возвращаться к вопросам, а заданный автором путь AI завершается. */
    @Test
    fun permitsPlayerQuestionsWithTerminatingAiPath() {
        validate(story())
    }

    /** Неизвестные ссылки и выход за карту отклоняются без изменения входа. */
    @Test
    fun rejectsBrokenReferencesAndOffMapZones() {
        rejects(story().apply { triggers.single().heroId = "missing" })
        rejects(story().apply { triggers.single().zoneId = "missing" })
        rejects(story().apply { triggers.single().effects.single().reference = "missing" })
        rejects(story().apply { dialogues.single().nodes.single().choices.first().nextNodeId = "missing" })
        rejects(story().apply { zones.single().positions.add(CityPosition(99f, 0f)) })
    }

    /** Назначенный ответ AI должен оставаться доступным независимо от флагов; это не требование выхода игрока. */
    @Test
    fun rejectsConditionalAiChoice() {
        rejects(story().apply {
            dialogues.single().nodes.single().choices.last().condition.requiredFlags.add("secret")
        })
    }

    /** Авторский цикл AI отклоняется, хотя аналогичный повтор вопроса допустим для человека. */
    @Test
    fun rejectsAiQuestionLoop() {
        rejects(story().apply { dialogues.single().aiChoices["hello"] = "ask" })
    }

    /** Некорректные награды и отсутствующий fallback ролика не доходят до игровых адаптеров. */
    @Test
    fun rejectsInvalidEffectsAndCinematics() {
        rejects(story().apply { triggers.single().effects = arrayListOf(StoryEffect(StoryEffectKind.GrantGold, amount = -1)) })
        rejects(story().apply { cinematics.add(StoryCinematic("intro")) })
        rejects(story().apply { cinematics.add(StoryCinematic("intro", "../outside.mp4", "Начало")) })
    }

    /** Превращение Continue в ответ разрешает цепочку реплик без немедленного выхода и не меняет данные при проверке. */
    @Test
    fun permitsAnswerChainWithoutImmediateExit() {
        val linear = StoryDialogue(
            id = "intro", startNodeId = "start", nodes = arrayListOf(
                StoryDialogueNode(id = "start", text = "Начало", nextNodeId = "next"),
                StoryDialogueNode(id = "next", text = "Продолжение", nextNodeId = "end"),
                StoryDialogueNode(id = "end", text = "Конец")
            )
        )
        val first = com.unciv.pure.application.story.EditStoryDialogue.addAnswer(
            linear,
            "start",
            "accepted",
            "Принято"
        )
        val dialogue = com.unciv.pure.application.story.EditStoryDialogue.addAnswer(
            first,
            "next",
            "continue",
            "Дальше"
        )
        val scenario = StoryScenario(dialogues = arrayListOf(dialogue))
        val before = scenario.deepCopy()
        validate(scenario)
        assertEquals(before, scenario)
        assertEquals("next", dialogue.nodes[0].choices.single().nextNodeId)
        assertEquals("end", dialogue.nodes[1].choices.single().nextNodeId)
        assertTrue(linear.nodes.all { it.choices.isEmpty() })
    }
}
