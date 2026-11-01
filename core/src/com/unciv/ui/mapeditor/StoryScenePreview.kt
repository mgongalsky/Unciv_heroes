package com.unciv.ui.mapeditor

import com.unciv.pure.application.story.AdvanceStory
import com.unciv.pure.domain.story.*
import com.unciv.ui.popup.Popup
import com.unciv.ui.story.StoryCinematicScreen
import com.unciv.ui.story.StoryDialoguePopup
import com.unciv.ui.utils.BaseScreen

/** Предпросмотр использует игровые переходы на отдельном снимке, не обращаясь к GameInfo и ресурсам карты. */
class StoryScenePreview(
    private val screen: BaseScreen,
    scenario: StoryScenario,
    effect: StoryEffect
) {
    private val scenario = scenario.deepCopy()
    private var progress = StoryProgress(
        pending = arrayListOf(PendingStoryEffect(sequence = 1, effect = effect.copy())),
        nextSequence = 2
    )
    private var stopped = false

    /** Открывает следующую сцену или показывает последствие, которое в игре изменило бы мир. */
    fun open() {
        if (stopped) return
        val head = progress.pending.firstOrNull()
        if (head == null) {
            Popup(screen).apply {
                addGoodSizedLabel("Preview complete").row()
                addCloseButton()
            }.open()
            return
        }
        when (head.effect.kind) {
            StoryEffectKind.Dialogue -> {
                val node = AdvanceStory.currentNode(scenario, progress)
                val choices = AdvanceStory.availableChoices(scenario, progress)
                StoryDialoguePopup(screen, node, choices) { answer ->
                    progress = AdvanceStory.respond(scenario, progress, head.sequence, progress.revision, answer)
                    this@StoryScenePreview.open()
                    true
                }.apply {
                    addGoodSizedLabel("Preview: changes are not applied to the map").row()
                    addCloseButton("Close preview", action = { stopped = true })
                }.open()
            }
            StoryEffectKind.Cinematic -> {
                val cinematic = scenario.cinematics.first { it.id == head.effect.reference }
                screen.game.pushScreen(StoryCinematicScreen(
                    cinematic.assetPath, cinematic.fallbackText, cinematic.skippable
                ) {
                    progress = AdvanceStory.complete(progress, head.sequence, progress.revision)
                    // Следующая сцена запускается после возврата с экрана ролика.
                    screen.stage.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.run {
                        this@StoryScenePreview.open()
                    })
                })
            }
            else -> {
                // Подтверждение изменяет только локальные флаги и очередь; адаптер мировых последствий не вызывается.
                Popup(screen).apply {
                    addGoodSizedLabel("Preview: changes are not applied to the map").row()
                    addGoodSizedLabel(
                        "${head.effect.kind}: ${head.effect.reference}\n" +
                            "${head.effect.amount} ${head.effect.unitName} ${head.effect.recipientSlotId}"
                    ).row()
                    addButton("Continue") {
                        progress = AdvanceStory.complete(progress, head.sequence, progress.revision)
                        close()
                        this@StoryScenePreview.open()
                    }.row()
                    addCloseButton("Close preview", action = { stopped = true })
                }.open()
            }
        }
    }
}
