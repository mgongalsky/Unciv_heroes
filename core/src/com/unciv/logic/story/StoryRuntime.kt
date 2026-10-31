package com.unciv.logic.story

import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.pure.application.story.AdvanceStory
import com.unciv.pure.application.story.ProcessStoryEvent
import com.unciv.pure.domain.story.*

/** Координирует чистые переходы и игровые последствия без зависимости от WorldScreen. */
object StoryRuntime {
    /** Вступления других людей ждут их первого хода, иначе общая очередь не позволит передать управление. */
    fun startMission(game: GameInfo) {
        if (game.tileMap.story == null) return
        val owners = game.civilizations.filter { !it.isBarbarian() && !it.isSpectator() }
        owners.firstOrNull { it.civName == game.currentPlayer && it.playerType == PlayerType.Human }
            ?.let { startParticipant(game, it.civName) }
        for (owner in owners.filter { it.playerType == PlayerType.AI })
            startParticipant(game, owner.civName)
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

    /** Немодальные линейные реплики допускают действия на карте, но не передачу хода или бой.
     * Выбор, последствия за репликой и ожидающая атака удерживают мир независимо от pauseWorld.
     */
    fun isMapActionBlocked(game: GameInfo): Boolean {
        val scenario = game.tileMap.story ?: return false
        if (game.storyPendingAttack != null) return true
        return game.storyProgress.pending.any { pending ->
            if (pending.effect.kind != StoryEffectKind.Dialogue) true
            else {
                val dialogue = scenario.dialogues.firstOrNull { it.id == pending.effect.reference }
                dialogue == null || dialogue.pauseWorld || dialogue.nodes.any { it.choices.isNotEmpty() }
            }
        }
    }

    /** Публикует начало миссии один раз для участника; отметка сохраняется вместе с очередью. */
    fun startParticipant(game: GameInfo, ownerName: String) {
        val scenario = game.tileMap.story ?: return
        if (ownerName in game.storyProgress.startedParticipants) return
        val owner = game.civilizations.firstOrNull { it.civName == ownerName } ?: return
        if (owner.isBarbarian() || owner.isSpectator()) return
        if (owner.playerType == PlayerType.Human && ownerName != game.currentPlayer) return
        val event = StoryEvent(
            kind = StoryEventKind.MissionStarted, ownerName = ownerName,
            ownerSlotId = game.scenarioVictoryParticipants.entries
                .firstOrNull { it.value == ownerName }?.key.orEmpty()
        )
        val next = ProcessStoryEvent.execute(scenario, game.storyProgress, event)
        next.startedParticipants.add(ownerName)
        game.storyProgress = next
        drain(game)
    }
}
