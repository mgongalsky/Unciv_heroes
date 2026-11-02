package com.unciv.app.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.pure.domain.story.StoryChoice
import com.unciv.pure.domain.story.StoryDialogueNode
import com.unciv.ui.story.StoryDialoguePopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.activate

/** Checks layout and response delivery without changing a running story or visual baselines. */
internal object StoryDialogueGoldenChecks {
    fun open(screen: BaseScreen) {
        val stage = screen.stage
        val viewport = stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        try {
            for (width in listOf(360f, 1000f)) {
                stage.viewport = com.badlogic.gdx.utils.viewport.FitViewport(width, 480f)
                stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
                val choices = arrayListOf(
                    StoryChoice("ask", "Расскажи о переправе. ".repeat(8)),
                    StoryChoice("leave", "Мы вернёмся к этому разговору после встречи с разведчиком. ".repeat(8))
                )
                val node = StoryDialogueNode(
                    id = "royal-layout", speaker = "Хранитель старой переправы",
                    portrait = "missing-story-portrait",
                    text = "Разведчик ещё не вернулся. Нам нужно решить, как помочь отряду. ".repeat(20),
                    choices = choices
                )
                val responses = mutableListOf<String?>()
                var accept = false
                val popup = StoryDialoguePopup(screen, node, choices) {
                    responses.add(it)
                    accept
                }
                try {
                    stage.addActor(popup)
                    popup.setFillParent(false)
                    popup.setBounds(0f, 0f, width, 480f)
                    popup.isVisible = true
                    val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                    fun layout() {
                        popup.validate()
                        scroll.validate()
                        popup.innerTable.validate()
                        stage.draw()
                    }
                    layout()
                    check(scroll.maxX <= 1f) { "Story popup has horizontal overflow at $width" }
                    check(scroll.height <= 456f + 1f && scroll.maxY > 0f) { "Story scroll at $width: height=${scroll.height}, maxY=${scroll.maxY}, stage=${stage.width}x${stage.height}" }
                    check(scroll.x >= -1f && scroll.x + scroll.width <= width + 1f)
                    val buttons = popup.innerTable.children.filterIsInstance<TextButton>()
                    check(buttons.size == 2)
                    for (button in buttons) {
                        check(button.width <= scroll.width)
                        check(button.label.height + 1f >= button.label.prefHeight)
                    }
                    val target = buttons.last()
                    scroll.scrollTo(target.x, target.y, target.width, target.height)
                    scroll.updateVisualScroll()
                    layout()
                    fun click() {
                        val point = target.localToStageCoordinates(Vector2(target.width / 2f, target.height / 2f))
                        val hit = stage.hit(point.x, point.y, true)
                        check(hit === target || hit?.isDescendantOf(target) == true) { "Story response is not reachable" }
                        stage.stageToScreenCoordinates(point)
                        stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                        stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                    }
                    click()
                    check(responses == listOf("leave") && popup.parent != null)
                    accept = true
                    click()
                    check(responses == listOf("leave", "leave") && popup.parent == null)
                    target.activate()
                    check(responses.size == 2) { "Detached response was delivered again" }
                } finally {
                    if (popup.parent != null) popup.close()
                }
            }
        } finally {
            stage.viewport = viewport
            viewport.setWorldSize(oldWidth, oldHeight)
            stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        }
        com.unciv.ui.playground.openStoryDialoguePreview(screen)
    }
}
