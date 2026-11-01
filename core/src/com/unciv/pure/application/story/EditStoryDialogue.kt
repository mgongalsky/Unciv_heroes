package com.unciv.pure.application.story

import com.unciv.pure.domain.story.StoryDialogue
import com.unciv.pure.domain.story.StoryDialogueNode

/** Команды редактора возвращают независимый граф; неудачная команда не меняет источник.
 * Проверяется структура разговора. Связи со всей миссией проверяет ValidateStoryScenario при Apply.
 */
object EditStoryDialogue {
    fun addNode(source: StoryDialogue, node: StoryDialogueNode): StoryDialogue {
        require(source.nodes.none { it.id == node.id }) { "Node ID already exists: ${node.id}" }
        return source.deepCopy().apply {
            nodes.add(node.deepCopy())
            if (startNodeId.isBlank()) startNodeId = node.id
        }.also(::validate)
    }

    /** Переименование переносит входящие переходы, самоссылки, старт и назначение AI. */
    fun replaceNode(
        source: StoryDialogue,
        nodeId: String,
        replacement: StoryDialogueNode,
        aiChoiceId: String? = source.aiChoices[nodeId]
    ): StoryDialogue {
        val index = source.nodes.indexOfFirst { it.id == nodeId }
        require(index >= 0) { "Unknown node: $nodeId" }
        require(source.nodes.none { it.id != nodeId && it.id == replacement.id }) {
            "Node ID already exists: ${replacement.id}"
        }
        return source.deepCopy().apply {
            nodes[index] = replacement.deepCopy()
            if (startNodeId == nodeId) startNodeId = replacement.id
            for (node in nodes) {
                if (node.nextNodeId == nodeId) node.nextNodeId = replacement.id
                for (choice in node.choices)
                    if (choice.nextNodeId == nodeId) choice.nextNodeId = replacement.id
            }
            aiChoices.remove(nodeId)
            if (!aiChoiceId.isNullOrBlank()) aiChoices[replacement.id] = aiChoiceId
        }.also(::validate)
    }

    /** Автор сначала перенаправляет входящие стрелки и старт; удаление не обрывает их молча. */
    fun removeNode(source: StoryDialogue, nodeId: String): StoryDialogue {
        require(source.nodes.any { it.id == nodeId }) { "Unknown node: $nodeId" }
        require(source.startNodeId != nodeId) { "Choose another start node before deleting this node" }
        require(source.nodes.none { node ->
            node.id != nodeId && (node.nextNodeId == nodeId || node.choices.any { it.nextNodeId == nodeId })
        }) { "Redirect incoming transitions before deleting this node" }
        return source.deepCopy().apply {
            nodes.removeAll { it.id == nodeId }
            aiChoices.remove(nodeId)
        }.also(::validate)
    }

    fun setStart(source: StoryDialogue, nodeId: String): StoryDialogue =
        source.deepCopy().apply { startNodeId = nodeId }.also(::validate)

    /** Структурная проверка допускает циклы человеческих ответов; правила маршрута AI проверяет миссия. */
    fun validate(dialogue: StoryDialogue) {
        val ids = dialogue.nodes.map { it.id }
        require(ids.isNotEmpty() && ids.all { it.isNotBlank() } && ids.distinct().size == ids.size) {
            "Node IDs must be non-empty and unique"
        }
        require(dialogue.startNodeId in ids) { "Start node does not exist" }
        fun link(id: String) {
            require(id.isEmpty() || id in ids) { "Transition points to an unknown node: $id" }
        }
        for (node in dialogue.nodes) {
            link(node.nextNodeId)
            require(node.choices.isEmpty() || node.nextNodeId.isEmpty()) {
                "A node with answers cannot also have a continuation"
            }
            val choices = node.choices.map { it.id }
            require(choices.all { it.isNotBlank() } && choices.distinct().size == choices.size) {
                "Answer IDs must be non-empty and unique within a node"
            }
            for (choice in node.choices) {
                link(choice.nextNodeId)
                choice.condition.validate()
            }
            dialogue.aiChoices[node.id]?.let {
                require(it in choices) { "AI answer does not exist in node: ${node.id}" }
            }
        }
        require(dialogue.aiChoices.keys.all { it in ids }) { "AI assignment points to an unknown node" }
    }
}
