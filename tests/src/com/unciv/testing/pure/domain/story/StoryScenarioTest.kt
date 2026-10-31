package com.unciv.testing.pure.domain.story

import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.story.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет локальные условия и владение изменяемыми данными сценария без движка. */
class StoryScenarioTest {
    /** Невыставленный флаг считается ложным; обязательные и запрещённые флаги проверяются совместно. */
    @Test
    fun conditionsCombineRequiredAndForbiddenFlags() {
        val condition = StoryCondition(arrayListOf("met"), arrayListOf("hostile"))
        assertFalse(condition.matches(emptySet()))
        assertTrue(condition.matches(setOf("met", "unrelated")))
        assertFalse(condition.matches(setOf("met", "hostile")))
        assertTrue(StoryCondition().matches(emptySet()))
    }

    /** Противоречивое условие отклоняется вместо создания недостижимой ветки. */
    @Test
    fun contradictoryConditionIsRejected() {
        try {
            StoryCondition(arrayListOf("met"), arrayListOf("met")).validate()
            fail("Противоречивое условие должно быть отклонено")
        } catch (_: IllegalArgumentException) { }
    }

    /** Все вложенные черновики отделены: изменение ответа, эффекта, зоны или AI не меняет исходную карту. */
    @Test
    fun copySeparatesNestedScenarioData() {
        val source = StoryScenario(
            zones = arrayListOf(StoryZone("gate", "Ворота", arrayListOf(CityPosition(0f, 0f)))),
            triggers = arrayListOf(StoryTrigger(
                id = "entry", condition = StoryCondition(arrayListOf("ready")),
                effects = arrayListOf(StoryEffect(reference = "met"))
            )),
            dialogues = arrayListOf(StoryDialogue(
                id = "talk", startNodeId = "hello",
                nodes = arrayListOf(StoryDialogueNode(
                    id = "hello", text = "Кто ты?",
                    choices = arrayListOf(StoryChoice(
                        "ask", "Расскажи", condition = StoryCondition(forbiddenFlags = arrayListOf("hostile")),
                        effects = arrayListOf(StoryEffect(StoryEffectKind.GrantGold, amount = 5))
                    ))
                )), aiChoices = linkedMapOf("hello" to "ask")
            )),
            cinematics = arrayListOf(StoryCinematic("intro", "intro.webm", "Начало"))
        )
        val copy = source.deepCopy()
        copy.zones.single().positions.clear()
        copy.triggers.single().condition.requiredFlags.clear()
        copy.triggers.single().effects.single().reference = "changed"
        copy.dialogues.single().nodes.single().choices.single().apply {
            text = "Изменено"
            condition.forbiddenFlags.clear()
            effects.single().amount = 100
        }
        copy.dialogues.single().aiChoices.clear()
        copy.cinematics.single().assetPath = "changed"
        assertEquals(1, source.zones.single().positions.size)
        assertEquals(listOf("ready"), source.triggers.single().condition.requiredFlags)
        assertEquals("met", source.triggers.single().effects.single().reference)
        val answer = source.dialogues.single().nodes.single().choices.single()
        assertEquals("Расскажи", answer.text)
        assertEquals(listOf("hostile"), answer.condition.forbiddenFlags)
        assertEquals(5, answer.effects.single().amount)
        assertEquals("ask", source.dialogues.single().aiChoices["hello"])
        assertEquals("intro.webm", source.cinematics.single().assetPath)
    }
}
