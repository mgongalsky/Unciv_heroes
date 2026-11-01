package com.unciv.testing.pure.application.story

import com.unciv.pure.application.story.EditStoryDialogue
import com.unciv.pure.domain.story.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет сохранение связей и изоляцию команд графа без движка и файлов. */
class EditStoryDialogueTest {
    private fun dialogue() = StoryDialogue(
        id = "talk", startNodeId = "question",
        nodes = arrayListOf(
            StoryDialogueNode(id = "question", text = "Вопрос", choices = arrayListOf(
                StoryChoice("again", "Повторить", "question",
                    StoryCondition(arrayListOf("known")), arrayListOf(StoryEffect(reference = "asked"))),
                StoryChoice("finish", "Завершить", "end")
            )),
            StoryDialogueNode(id = "end", text = "Конец"),
            StoryDialogueNode(id = "entry", text = "Вступление", nextNodeId = "question")
        ), aiChoices = linkedMapOf("question" to "finish")
    )

    private fun rejects(action: () -> Unit) {
        try { action(); fail("Ожидался отказ команды") }
        catch (_: IllegalArgumentException) { }
    }

    /** Переименование обновляет старт, входящее ребро, самоссылку и ключ AI. */
    @Test fun renamePreservesConnectionsAndStoryData() {
        val source = dialogue()
        val saved = source.deepCopy()
        val replacement = source.nodes.first().deepCopy().apply { id = "renamed" }
        val result = EditStoryDialogue.replaceNode(source, "question", replacement)
        assertEquals("renamed", result.startNodeId)
        assertEquals("renamed", result.nodes.last().nextNodeId)
        assertEquals("renamed", result.nodes.first().choices.first().nextNodeId)
        assertEquals(linkedMapOf("renamed" to "finish"), result.aiChoices)
        assertEquals(source.nodes.first().choices.first().condition, result.nodes.first().choices.first().condition)
        assertEquals(source.nodes.first().choices.first().effects, result.nodes.first().choices.first().effects)
        result.nodes.first().choices.first().condition.requiredFlags.add("later")
        result.nodes.first().choices.first().effects.first().reference = "changed"
        assertEquals(saved, source)
        assertEquals(listOf("known"), replacement.choices.first().condition.requiredFlags)
        assertEquals("asked", replacement.choices.first().effects.first().reference)
    }

    /** Неверные ID и ссылки отклоняются без частичного переноса входящих рёбер. */
    @Test fun invalidReplacementDoesNotMutateSource() {
        val source = dialogue()
        val saved = source.deepCopy()
        for (id in listOf("", "end")) {
            rejects { EditStoryDialogue.replaceNode(source, "question", source.nodes.first().copy(id = id)) }
        }
        rejects { EditStoryDialogue.replaceNode(source, "end", StoryDialogueNode(id = "end", nextNodeId = "missing")) }
        assertEquals(saved, source)
    }

    /** Удаление старта или цели стрелки требует явного изменения связей автором. */
    @Test fun deletionProtectsStartAndIncomingLinks() {
        val source = dialogue()
        val saved = source.deepCopy()
        rejects { EditStoryDialogue.removeNode(source, "question") }
        rejects { EditStoryDialogue.removeNode(source, "end") }
        rejects { EditStoryDialogue.removeNode(source, "missing") }
        val result = EditStoryDialogue.removeNode(source, "entry")
        assertEquals(listOf("question", "end"), result.nodes.map { it.id })
        assertEquals(saved, source)
    }

    /** Новый узел копируется, а выбор старта допускает только существующий ID. */
    @Test fun addingAndSelectingStartAreIndependent() {
        val source = dialogue()
        val node = StoryDialogueNode(id = "new", text = "Новый", nextNodeId = "end")
        val added = EditStoryDialogue.addNode(source, node)
        node.text = "Изменён снаружи"
        assertEquals("Новый", added.nodes.last().text)
        val result = EditStoryDialogue.setStart(added, "new")
        assertEquals("new", result.startNodeId)
        assertEquals("question", added.startNodeId)
        rejects { EditStoryDialogue.setStart(added, "missing") }
        rejects { EditStoryDialogue.addNode(source, source.nodes.first()) }
    }

    /** Замена ID ответа требует согласованного назначения AI и сохраняет источник. */
    @Test fun answerRenameRequiresValidAiAssignment() {
        val source = dialogue()
        val replacement = source.nodes.first().deepCopy()
        replacement.choices.last().id = "done"
        rejects { EditStoryDialogue.replaceNode(source, "question", replacement) }
        val result = EditStoryDialogue.replaceNode(source, "question", replacement, "done")
        assertEquals("done", result.aiChoices["question"])
        assertEquals("finish", source.aiChoices["question"])
    }

    /** Противоречивое условие и дубли ответов не попадают в результат команды. */
    @Test fun rejectsAmbiguousAnswersAndConditions() {
        val source = dialogue()
        val replacement = source.nodes.first().deepCopy()
        replacement.choices.first().condition.forbiddenFlags.add("known")
        rejects { EditStoryDialogue.replaceNode(source, "question", replacement) }
        replacement.choices.first().condition.forbiddenFlags.clear()
        replacement.choices.first().id = "finish"
        rejects { EditStoryDialogue.replaceNode(source, "question", replacement) }
        assertEquals(listOf("again", "finish"), source.nodes.first().choices.map { it.id })
    }

    /** Соединение адресует один выход, допускает возврат и отключение, сохраняет данные и отклоняет неверный drop. */
    @Test
    fun transitionsAddressOneOutputWithoutMutatingSource() {
        val source = dialogue()
        val saved = source.deepCopy()
        val linked = EditStoryDialogue.setTransition(source, "question", "again", "end")
        assertEquals("end", linked.nodes.first().choices.first().nextNodeId)
        assertEquals(saved.nodes.first().choices.last(), linked.nodes.first().choices.last())
        assertEquals(saved.aiChoices, linked.aiChoices)
        assertEquals(
            saved.nodes.first().choices.first().condition,
            linked.nodes.first().choices.first().condition
        )
        assertEquals(
            saved.nodes.first().choices.first().effects,
            linked.nodes.first().choices.first().effects
        )
        val disconnected = EditStoryDialogue.setTransition(linked, "question", "again", "")
        assertEquals("", disconnected.nodes.first().choices.first().nextNodeId)
        assertEquals("end", linked.nodes.first().choices.first().nextNodeId)
        val loop = EditStoryDialogue.setTransition(disconnected, "question", "again", "question")
        assertEquals("question", loop.nodes.first().choices.first().nextNodeId)

        val continuation = EditStoryDialogue.setTransition(source, "entry", null, "end")
        assertEquals("end", continuation.nodes.last().nextNodeId)
        assertEquals(
            "",
            EditStoryDialogue.setTransition(continuation, "entry", null, "").nodes.last().nextNodeId
        )
        rejects { EditStoryDialogue.setTransition(source, "missing", null, "end") }
        rejects { EditStoryDialogue.setTransition(source, "entry", null, "missing") }
        rejects { EditStoryDialogue.setTransition(source, "question", null, "end") }
        rejects { EditStoryDialogue.setTransition(source, "question", "missing", "end") }
        rejects { EditStoryDialogue.setTransition(source, "entry", "again", "end") }
        linked.nodes.first().choices.first().condition.requiredFlags.add("external")
        linked.nodes.first().choices.first().effects.first().reference = "external"
        assertEquals(saved, source)
        assertEquals(
            listOf("known"),
            disconnected.nodes.first().choices.first().condition.requiredFlags
        )
        assertEquals("asked", disconnected.nodes.first().choices.first().effects.first().reference)
    }
}
