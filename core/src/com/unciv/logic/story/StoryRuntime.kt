package com.unciv.logic.story

import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.pure.application.story.AdvanceStory
import com.unciv.pure.application.story.ProcessStoryEvent
import com.unciv.pure.domain.story.*

/** Координирует чистые переходы и игровые последствия без зависимости от WorldScreen. */
object StoryRuntime {
    /** Вызывается только после подготовки новой миссии; восстановление сохранения не публикует события. */
    fun startMission(game: GameInfo) {
        if (game.tileMap.story == null) return
        for (owner in game.civilizations.filter { !it.isBarbarian() && !it.isSpectator() }
            .sortedBy { it.playerType != PlayerType.Human }) {
            emit(game, StoryEvent(
                kind = StoryEventKind.MissionStarted,
                ownerName = owner.civName,
                ownerSlotId = game.scenarioVictoryParticipants.entries.firstOrNull { it.value == owner.civName }?.key.orEmpty()
            ))
        }
    }

    /** Сначала публикует сохраняемую очередь, затем исполняет её до ближайшей сцены человека. */
    fun emit(game: GameInfo, event: StoryEvent) {
        val scenario = game.tileMap.story ?: return
        game.storyProgress = ProcessStoryEvent.execute(scenario, game.storyProgress, event)
        drain(game)
    }

    /** Наличие очереди запрещает начинать следующую игровую операцию до завершения обязательного взаимодействия. */
    fun isBlocked(game: GameInfo): Boolean = game.tileMap.story != null && game.storyProgress.pending.isNotEmpty()

    /** UI передаёт владельца и версию показанной реплики; чужой или устаревший выбор отклоняется. */
    fun respond(game: GameInfo, ownerName: String, sequence: Long, revision: Long, choiceId: String? = null) {
        val scenario = requireNotNull(game.tileMap.story) { "No story scenario" }
        val pending = requireNotNull(game.storyProgress.pending.firstOrNull()) { "Story queue is empty" }
        require(pending.event.ownerName == ownerName) { "This story scene belongs to another participant" }
        game.storyProgress = AdvanceStory.respond(scenario, game.storyProgress, sequence, revision, choiceId)
        drain(game)
    }

    /** Завершение или пропуск ролика имеет ту же защиту от повторного callback, что выбор ответа. */
    fun finishCinematic(game: GameInfo, ownerName: String, sequence: Long, revision: Long) {
        val pending = requireNotNull(game.storyProgress.pending.firstOrNull()) { "Story queue is empty" }
        require(pending.effect.kind == StoryEffectKind.Cinematic && pending.event.ownerName == ownerName) {
            "Cinematic does not belong to this participant"
        }
        game.storyProgress = AdvanceStory.complete(game.storyProgress, sequence, revision)
        drain(game)
    }

    /** Каждое последствие подтверждается вместе с изменением мира; ошибка подготовки оставляет его в очереди. */
    fun drain(game: GameInfo) {
        val scenario = game.tileMap.story ?: return
        var operations = 0
        while (game.storyProgress.pending.isNotEmpty()) {
            require(++operations <= 1024) { "Story automatic sequence exceeds 1024 operations; check scene cycles" }
            val progress = game.storyProgress
            val pending = progress.pending.first()
            val owner = requireNotNull(game.civilizations.firstOrNull { it.civName == pending.event.ownerName }) {
                "Story participant no longer exists"
            }
            when (pending.effect.kind) {
                StoryEffectKind.Dialogue -> {
                    if (owner.playerType == PlayerType.Human) return
                    val node = AdvanceStory.currentNode(scenario, progress)
                    val dialogue = scenario.dialogues.first { it.id == pending.effect.reference }
                    val answer = if (node.choices.isEmpty()) null else dialogue.aiChoices[node.id]
                    game.storyProgress = AdvanceStory.respond(scenario, progress, pending.sequence, progress.revision, answer)
                }
                StoryEffectKind.Cinematic -> {
                    if (owner.playerType == PlayerType.Human) return
                    game.storyProgress = AdvanceStory.complete(progress, pending.sequence, progress.revision)
                }
                StoryEffectKind.SetFlag ->
                    game.storyProgress = AdvanceStory.complete(progress, pending.sequence, progress.revision)
                StoryEffectKind.CancelAttack -> {
                    require(game.storyPendingAttack != null && pending.event.kind == StoryEventKind.BeforeAttack) {
                        "There is no pending story attack to cancel"
                    }
                    val next = AdvanceStory.complete(progress, pending.sequence, progress.revision)
                    game.storyAttackCancelled = true
                    game.storyProgress = next
                }
                else -> {
                    val apply = StoryWorldEffects.prepare(game, pending)
                    val next = AdvanceStory.complete(progress, pending.sequence, progress.revision)
                    apply()
                    game.storyProgress = next
                }
            }
        }
    }
}
