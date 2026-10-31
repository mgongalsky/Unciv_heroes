package com.unciv.pure.application.story

import com.unciv.pure.domain.story.*

/** Переходы очереди и разговоров; адаптер публикует результат только вместе с успешным игровым последствием. */
object AdvanceStory {
    /** Возвращает независимую реплику для UI; просмотр не запускает эффектов. */
    fun currentNode(scenario: StoryScenario, progress: StoryProgress): StoryDialogueNode {
        val pending = requireNotNull(progress.pending.firstOrNull()) { "Story queue is empty" }
        require(pending.effect.kind == StoryEffectKind.Dialogue) { "Current story effect is not a dialogue" }
        val dialogue = scenario.dialogues.first { it.id == pending.effect.reference }
        return dialogue.nodes.first { it.id == pending.nodeId.ifEmpty { dialogue.startNodeId } }.deepCopy()
    }

    /** Список ответов вычисляется по текущим флагам, а не по устаревшей копии виджета. */
    fun availableChoices(scenario: StoryScenario, progress: StoryProgress): List<StoryChoice> =
        currentNode(scenario, progress).choices.filter { it.condition.matches(progress.flags) }

    /** Подтверждает одно выполненное последствие. Диалог требует явного перехода, поэтому пропустить выбор нельзя. */
    fun complete(progress: StoryProgress, sequence: Long, revision: Long): StoryProgress {
        val pending = checkedHead(progress, sequence, revision)
        require(pending.effect.kind != StoryEffectKind.Dialogue) { "Dialogue requires an explicit response" }
        val result = progress.deepCopy()
        if (pending.effect.kind == StoryEffectKind.SetFlag) {
            require(pending.effect.reference.isNotBlank()) { "Empty story flag" }
            if (pending.effect.flagValue) result.flags.add(pending.effect.reference)
            else result.flags.remove(pending.effect.reference)
        }
        result.pending.removeAt(0)
        result.revision++
        return result
    }

    /** null означает продолжение линейной реплики; узел с ответами всегда требует ID доступного ответа. */
    fun respond(
        scenario: StoryScenario,
        progress: StoryProgress,
        sequence: Long,
        revision: Long,
        choiceId: String? = null
    ): StoryProgress {
        val head = checkedHead(progress, sequence, revision)
        val node = currentNode(scenario, progress)
        val choice = if (node.choices.isEmpty()) {
            require(choiceId == null) { "This dialogue node has no choices" }
            null
        } else {
            requireNotNull(node.choices.firstOrNull { it.id == choiceId && it.condition.matches(progress.flags) }) {
                "Dialogue choice is missing or unavailable"
            }
        }
        val destination = choice?.nextNodeId ?: node.nextNodeId
        val dialogue = scenario.dialogues.first { it.id == head.effect.reference }
        require(destination.isEmpty() || dialogue.nodes.any { it.id == destination }) { "Unknown dialogue destination" }
        val result = progress.deepCopy()
        result.pending.removeAt(0)
        val inserted = arrayListOf<PendingStoryEffect>()
        for (effect in choice?.effects.orEmpty()) {
            require(result.nextSequence > 0 && result.nextSequence < Long.MAX_VALUE) { "Story sequence exhausted" }
            inserted.add(PendingStoryEffect(result.nextSequence++, effect.copy(), head.event.copy()))
        }
        // Продолжение сохраняет контекст героя и следует после последствий ответа, включая вложенные сцены.
        if (destination.isNotEmpty()) inserted.add(head.deepCopy().apply { nodeId = destination })
        result.pending.addAll(0, inserted)
        result.revision++
        return result
    }

    private fun checkedHead(progress: StoryProgress, sequence: Long, revision: Long): PendingStoryEffect {
        require(progress.revision == revision) { "Stale story command" }
        require(progress.revision < Long.MAX_VALUE) { "Story revision exhausted" }
        val head = requireNotNull(progress.pending.firstOrNull()) { "Story queue is empty" }
        require(head.sequence == sequence) { "Story command refers to another effect" }
        return head
    }
}
