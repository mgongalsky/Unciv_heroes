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

    fun create(dialogue: StoryDialogue, map: TileMap, ruleset: Ruleset, width: Float,
               mutate: (() -> Unit) -> Unit): StoryEditorForm {
        val form = StoryEditorForm(width)
        form.text("Dialogue ID", dialogue.id) { dialogue.id = it.trim() }
        form.choice("Start node", dialogue.startNodeId, dialogue.nodes.map { it.id to it.id }) {
            dialogue.startNodeId = it
        }
        form.flag("Pause world", dialogue.pauseWorld) { dialogue.pauseWorld = it }
        form.position("Camera focus (x,y; optional)", dialogue.focus) { dialogue.focus = it }
        for (node in dialogue.nodes) {
            form.add("Dialogue node".toLabel()).row()
            val oldNodeId = node.id
            form.text("Node ID", node.id) { node.id = it.trim() }
            form.text("Speaker", node.speaker) { node.speaker = it }
            form.text("Portrait image (optional)", node.portrait) { node.portrait = it.trim() }
            form.text("Text", node.text, multiline = true) { node.text = it }
            form.choice("Next node (without choices)", node.nextNodeId,
                listOf("" to "End dialogue") + dialogue.nodes.map { it.id to it.id }) { node.nextNodeId = it }
            if (node.choices.isNotEmpty()) {
                form.choice("AI answer", dialogue.aiChoices[oldNodeId].orEmpty(),
                    node.choices.map { it.id to it.text.ifBlank { it.id } }) {
                    dialogue.aiChoices.remove(oldNodeId)
                    if (it.isNotEmpty()) dialogue.aiChoices[node.id] = it
                }
            }
            for (choice in node.choices) {
                form.add("Answer".toLabel()).row()
                form.text("Answer ID", choice.id) { choice.id = it.trim() }
                form.text("Answer text", choice.text, multiline = true) { choice.text = it }
                form.choice("Next node", choice.nextNodeId,
                    listOf("" to "End dialogue") + dialogue.nodes.map { it.id to it.id }) { choice.nextNodeId = it }
                condition(form, choice.condition)
                form.include(StoryEffectsEditor.create(choice.effects, map, ruleset, width, mutate))
                form.add("Remove answer".toTextButton().onClick {
                    mutate {
                        node.choices.remove(choice)
                        if (dialogue.aiChoices[node.id] == choice.id) dialogue.aiChoices.remove(node.id)
                    }
                }).row()
            }
            form.add("Add answer".toTextButton().onClick {
                mutate {
                    val answer = StoryChoice(id = UUID.randomUUID().toString(), text = "Continue")
                    node.choices.add(answer)
                    node.nextNodeId = ""
                    if (node.id !in dialogue.aiChoices) dialogue.aiChoices[node.id] = answer.id
                }
            }).row()
            form.add("Remove node".toTextButton().onClick {
                mutate {
                    dialogue.nodes.remove(node)
                    dialogue.aiChoices.remove(node.id)
                }
            }).row()
        }
        form.add("Add node".toTextButton().onClick {
            mutate {
                val node = StoryDialogueNode(id = UUID.randomUUID().toString(), text = "New dialogue line")
                dialogue.nodes.add(node)
                if (dialogue.startNodeId.isEmpty()) dialogue.startNodeId = node.id
            }
        }).row()
        return form
    }
}
