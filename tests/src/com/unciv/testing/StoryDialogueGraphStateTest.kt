package com.unciv.testing

import com.unciv.pure.application.story.EditStoryDialogue
import com.unciv.pure.domain.story.StoryDialogue
import com.unciv.pure.domain.story.StoryDialogueNode
import com.unciv.ui.mapeditor.StoryDialogueGraphState
import org.junit.Assert.*
import org.junit.Test

/** Проверяет историю состояния представления без Scene2D, файлов и запуска игры. */
class StoryDialogueGraphStateTest {
    private fun source() = StoryDialogue(id = "talk", startNodeId = "a", nodes = arrayListOf(
        StoryDialogueNode(id = "a", text = "A"), StoryDialogueNode(id = "b", text = "B")
    ))

    /** Начальная раскладка не создаёт команд; законченный drag отменяется и повторяется целиком. */
    @Test fun movementHasIndependentUndoAndRedo() {
        val source = source()
        val state = StoryDialogueGraphState(source)
        val initial = StoryDialogueGraphState.Position(60f, 80f)
        state.ensurePosition("a", initial)
        assertFalse(state.canUndo)
        state.move("a", 220f, 160f)
        assertTrue(state.undo())
        assertEquals(initial, state.positions["a"])
        assertTrue(state.redo())
        assertEquals(StoryDialogueGraphState.Position(220f, 160f), state.positions["a"])
        assertEquals(source, state.dialogue)
        state.undo()
        state.move("a", 300f, 200f)
        assertFalse(state.canRedo)
    }

    /** Переименование переносит позицию; история и результат не разделяют изменяемые узлы. */
    @Test fun renamedNodeKeepsPositionAndHistoryOwnsCopies() {
        val source = source()
        val state = StoryDialogueGraphState(source)
        val position = StoryDialogueGraphState.Position(120f, 140f)
        state.ensurePosition("a", position)
        val updated = EditStoryDialogue.replaceNode(source, "a", source.nodes.first().copy(id = "renamed"))
        state.publish(updated, "renamed", "a")
        updated.nodes.first().text = "Outside mutation"
        assertEquals("A", state.dialogue.nodes.first().text)
        assertEquals(position, state.positions["renamed"])
        assertFalse(state.positions.containsKey("a"))
        state.undo()
        assertEquals("a", state.selectedId)
        assertEquals(position, state.positions["a"])
        state.dialogue.nodes.first().text = "Local mutation"
        state.redo()
        assertEquals("A", state.dialogue.nodes.first().text)
        assertEquals("A", source.nodes.first().text)
    }

    /** Размер текста ограничен, а явная автораскладка может быть отменена. */
    @Test fun fontAndLayoutChangesAreReversible() {
        val state = StoryDialogueGraphState(source())
        val position = StoryDialogueGraphState.Position(150f, 200f)
        state.ensurePosition("a", position)
        state.changeTextScale(10f)
        assertEquals(1f, state.textScale, 0f)
        state.undo()
        assertEquals(0.65f, state.textScale, 0f)
        state.autoLayout()
        assertTrue(state.positions.isEmpty())
        state.undo()
        assertEquals(position, state.positions["a"])
    }

    /** Ввод одного поля отменяется целиком; другое поле и ввод после Undo создают новые действия. */
    @Test
    fun consecutiveTypingIsGroupedWithoutLosingHistoryBoundaries() {
        val source = source()
        val state = StoryDialogueGraphState(source)
        val textField = Any()
        val speakerField = Any()
        fun type(text: String, speaker: String, group: Any) {
            val node = state.dialogue.nodes.first().deepCopy().apply {
                this.text = text
                this.speaker = speaker
            }
            state.publish(
                EditStoryDialogue.replaceNode(state.dialogue, "a", node),
                "a",
                editGroup = group
            )
        }
        type("H", "", textField)
        type("Hello", "", textField)
        type("Hello", "Hero", speakerField)
        assertTrue(state.undo())
        assertEquals("Hello", state.dialogue.nodes.first().text)
        assertEquals("", state.dialogue.nodes.first().speaker)
        assertTrue(state.undo())
        assertEquals("A", state.dialogue.nodes.first().text)
        assertTrue(state.redo())
        assertEquals("Hello", state.dialogue.nodes.first().text)
        type("Replacement", "", textField)
        assertFalse(state.canRedo)
        assertTrue(state.undo())
        assertEquals("Hello", state.dialogue.nodes.first().text)
        assertEquals("A", source.nodes.first().text)
    }
}
