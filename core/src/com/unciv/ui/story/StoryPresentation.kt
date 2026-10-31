package com.unciv.ui.story

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.story.StoryBattleEvents
import com.unciv.logic.story.StoryRuntime
import com.unciv.pure.application.story.AdvanceStory
import com.unciv.pure.domain.story.StoryEffectKind
import com.unciv.ui.popup.hasOpenPopups
import com.unciv.ui.worldscreen.WorldScreen

/** Восстанавливает представление из прогресса партии; создание окна не исполняет последствия. */
class StoryPresentation(private val screen: WorldScreen) {
    private var chatter: StoryChatterPanel? = null
    private var chatterRevision = -1L
    private var chatterWidth = 0f
    private var chatterHeight = 0f

    private fun clearChatter() {
        chatter?.remove()
        chatter = null
        chatterRevision = -1L
    }

    /** Вызывается в GL-потоке перед обновлением обычных окон мирового экрана. */
    fun update() {
        if (!screen.isPlayersTurn || screen.viewingCiv.isSpectator() || screen.hasOpenPopups()) {
            clearChatter()
            return
        }
        val game = screen.gameInfo
        val scenario = game.tileMap.story
        if (scenario == null) {
            clearChatter()
            return
        }
        val progress = game.storyProgress
        val pending = progress.pending.firstOrNull()
        if (pending == null) {
            clearChatter()
            if (game.storyPendingAttack?.ownerName == screen.viewingCiv.civName) {
                StoryBattleEvents.resume(game, screen.viewingCiv.civName)
                screen.shouldUpdate = true
            }
            return
        }
        if (pending.event.ownerName != screen.viewingCiv.civName) {
            clearChatter()
            return
        }
        if (pending.effect.kind == StoryEffectKind.Cinematic) {
            clearChatter()
            val cinematic = scenario.cinematics.first { it.id == pending.effect.reference }
            screen.game.pushScreen(StoryCinematicScreen(
                cinematic.assetPath, cinematic.fallbackText, cinematic.skippable
            ) {
                val current = game.storyProgress
                if (current.revision == progress.revision && current.pending.firstOrNull()?.sequence == pending.sequence) {
                    StoryRuntime.finishCinematic(game, screen.viewingCiv.civName, pending.sequence, progress.revision)
                }
                screen.shouldUpdate = true
            })
            return
        }
        if (pending.effect.kind != StoryEffectKind.Dialogue) {
            clearChatter()
            return
        }
        if (!StoryRuntime.isMapActionBlocked(game)) {
            if (chatterRevision == progress.revision && chatter?.parent != null &&
                chatterWidth == screen.stage.width && chatterHeight == screen.stage.height) return
            clearChatter()
            chatterRevision = progress.revision
            chatterWidth = screen.stage.width
            chatterHeight = screen.stage.height
            chatter = StoryChatterPanel(screen, AdvanceStory.currentNode(scenario, progress)) {
                val current = game.storyProgress
                if (current.revision == progress.revision && current.pending.firstOrNull()?.sequence == pending.sequence) {
                    StoryRuntime.respond(game, screen.viewingCiv.civName, pending.sequence, progress.revision)
                }
                clearChatter()
                screen.shouldUpdate = true
            }.also { screen.stage.addActor(it) }
            return
        }
        clearChatter()
        val node = AdvanceStory.currentNode(scenario, progress)
        val choices = AdvanceStory.availableChoices(scenario, progress)
        val dialogue = scenario.dialogues.first { it.id == pending.effect.reference }
        dialogue.focus?.let { focus ->
            // Авторский фокус не раскрывает скрытую клетку другому участнику.
            val position = Vector2(focus.x, focus.y)
            if (screen.viewingCiv.viewableTiles.any { it.position == position })
                screen.mapHolder.setCenterPosition(position, immediately = true, selectUnit = false)
        }
        StoryDialoguePopup(screen, node, choices) { answer ->
            val current = game.storyProgress
            if (current.revision == progress.revision && current.pending.firstOrNull()?.sequence == pending.sequence) {
                StoryRuntime.respond(game, screen.viewingCiv.civName, pending.sequence, progress.revision, answer)
            }
            // Следующая реплика или бой открываются на следующем кадре после закрытия этого окна.
            screen.shouldUpdate = true
            true
        }.open()
    }
}
