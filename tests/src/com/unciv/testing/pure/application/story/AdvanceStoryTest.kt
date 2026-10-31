package com.unciv.testing.pure.application.story

import com.unciv.pure.application.story.AdvanceStory
import com.unciv.pure.domain.story.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет выборы и подтверждения как чистые переходы сохраняемого состояния. */
class AdvanceStoryTest {
    private fun scenario() = StoryScenario(dialogues = arrayListOf(StoryDialogue(
        id = "talk", startNodeId = "question", nodes = arrayListOf(
            StoryDialogueNode(id = "question", text = "Поможешь?", choices = arrayListOf(
                StoryChoice("ask", "Расскажи", "question", effects = arrayListOf(StoryEffect(reference = "asked"))),
                StoryChoice("secret", "Я знаю тайну", condition = StoryCondition(arrayListOf("secret"))),
                StoryChoice("accept", "Помогу", "thanks", effects = arrayListOf(StoryEffect(StoryEffectKind.GrantGold, amount = 5))),
                StoryChoice("leave", "Уйти")
            )),
            StoryDialogueNode(id = "thanks", text = "Спасибо")
        )
    )))
    private fun progress() = StoryProgress(
        pending = arrayListOf(PendingStoryEffect(1, StoryEffect(StoryEffectKind.Dialogue, "talk"),
            StoryEvent(heroId = "guide", ownerName = "Rome"))), nextSequence = 2
    )
    private fun rejects(action: () -> Unit) {
        try { action(); fail("Ожидался отказ команды") } catch (_: IllegalArgumentException) { }
    }

    /** Ответ сначала ставит награду, затем продолжение; оригинал и контекст остаются независимыми. */
    @Test
    fun choiceQueuesRewardBeforeNextLine() {
        val source = progress()
        val chosen = AdvanceStory.respond(scenario(), source, 1, 0, "accept")
        assertEquals(StoryEffectKind.GrantGold, chosen.pending.first().effect.kind)
        assertEquals("thanks", chosen.pending.last().nodeId)
        assertEquals("guide", chosen.pending.first().event.heroId)
        assertEquals(1, source.pending.size)
        val rewarded = AdvanceStory.complete(chosen, chosen.pending.first().sequence, chosen.revision)
        assertEquals("Спасибо", AdvanceStory.currentNode(scenario(), rewarded).text)
        val done = AdvanceStory.respond(scenario(), rewarded, rewarded.pending.first().sequence, rewarded.revision)
        assertTrue(done.pending.isEmpty())
        rejects { AdvanceStory.complete(rewarded, chosen.pending.first().sequence, chosen.revision) }
    }

    /** Возврат к тому же вопросу имеет новую ревизию и не допускает повтор прежнего клика. */
    @Test
    fun returningQuestionRejectsStaleClick() {
        val first = AdvanceStory.respond(scenario(), progress(), 1, 0, "ask")
        val returned = AdvanceStory.complete(first, first.pending.first().sequence, first.revision)
        assertTrue("asked" in returned.flags)
        assertEquals("question", AdvanceStory.currentNode(scenario(), returned).id)
        rejects { AdvanceStory.respond(scenario(), returned, 1, 0, "ask") }
        assertEquals(1, returned.pending.size)
    }

    /** Недоступный ответ и попытка обойти выбор не меняют очередь и флаги. */
    @Test
    fun rejectsUnavailableOrMissingChoiceWithoutMutation() {
        val source = progress()
        val snapshot = source.deepCopy()
        assertFalse(AdvanceStory.availableChoices(scenario(), source).any { it.id == "secret" })
        rejects { AdvanceStory.respond(scenario(), source, 1, 0, "secret") }
        rejects { AdvanceStory.respond(scenario(), source, 1, 0) }
        rejects { AdvanceStory.complete(source, 1, 0) }
        assertEquals(snapshot, source)
    }

    /** Снятие флага и подтверждение внешнего эффекта работают на копиях и не допускают повторной доставки. */
    @Test
    fun completingFlagProducesIndependentState() {
        val source = StoryProgress(flags = linkedSetOf("met"), pending = arrayListOf(
            PendingStoryEffect(1, StoryEffect(reference = "met", flagValue = false))
        ), nextSequence = 2)
        val result = AdvanceStory.complete(source, 1, 0)
        assertTrue(result.flags.isEmpty())
        assertTrue(result.pending.isEmpty())
        assertEquals(setOf("met"), source.flags)
        rejects { AdvanceStory.complete(result, 1, 0) }
    }
}
