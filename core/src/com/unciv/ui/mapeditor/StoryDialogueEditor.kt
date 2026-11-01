package com.unciv.ui.mapeditor

import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.domain.story.*
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import java.util.UUID

/** Редактор графа разговора через поля и списки; JSON не требуется. */
object StoryDialogueEditor {
    fun condition(form: StoryEditorForm, condition: StoryCondition) {
        fun flags(text: String) = ArrayList(text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct())
        form.text("Required flags (comma-separated)", condition.requiredFlags.joinToString(", ")) {
            condition.requiredFlags = flags(it)
        }
        form.text("Forbidden flags (comma-separated)", condition.forbiddenFlags.joinToString(", ")) {
            condition.forbiddenFlags = flags(it)
        }
    }

    /** Форма одной реплики; переименование ответа сохраняет выбранное назначение AI. */
    fun createNode(
        dialogue: StoryDialogue,
        node: StoryDialogueNode,
        map: TileMap,
        ruleset: Ruleset,
        width: Float,
        aiChoice: String,
        saveAiChoice: (String) -> Unit,
        mutate: (() -> Unit) -> Unit
    ): StoryEditorForm {
        val form = StoryEditorForm(width)
        var capturedAiChoice = aiChoice
        fun updateAi(value: String) {
            capturedAiChoice = value
            saveAiChoice(value)
        }
        form.text("Node ID", node.id) { node.id = it.trim() }
        form.text("Speaker", node.speaker) { node.speaker = it }
        form.text("Portrait image (optional)", node.portrait) { node.portrait = it.trim() }
        form.text("Text", node.text, multiline = true) { node.text = it }
        val targets = listOf("" to "End dialogue") + dialogue.nodes.map { it.id to it.id }
        if (node.choices.isEmpty()) {
            form.choice("Next node", node.nextNodeId, targets) { node.nextNodeId = it }
        } else {
            form.choice(
                "AI answer",
                aiChoice,
                listOf("" to "Select an AI answer") + node.choices.map { it.id to it.text.ifBlank { it.id } },
                ::updateAi
            )
        }
        for (choice in node.choices) {
            form.add("Answer".toLabel()).row()
            form.text("Answer ID", choice.id) { raw ->
                val newId = raw.trim()
                if (capturedAiChoice == choice.id) updateAi(newId)
                choice.id = newId
            }
            form.text("Answer text", choice.text, multiline = true) { choice.text = it }
            form.choice("Next node", choice.nextNodeId, targets) { choice.nextNodeId = it }
            condition(form, choice.condition)
            form.include(StoryEffectsEditor.create(choice.effects, map, ruleset, width, mutate))
            form.add("Remove answer: ${choice.id}".toTextButton().onClick {
                mutate {
                    node.choices.remove(choice)
                    if (capturedAiChoice == choice.id) updateAi("")
                }
            }).row()
        }
        form.add("Add answer".toTextButton().onClick {
            mutate {
                val answer = StoryChoice(id = UUID.randomUUID().toString(), text = "Continue")
                node.choices.add(answer)
                node.nextNodeId = ""
                if (node.choices.size == 1) updateAi(answer.id)
            }
        }).row()
        return form
    }
}
