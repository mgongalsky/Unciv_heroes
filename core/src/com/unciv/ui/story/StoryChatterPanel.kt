package com.unciv.ui.story

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.pure.domain.story.StoryDialogueNode
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onActivation
import com.unciv.ui.utils.extensions.toLabel

/** Немодальная реплика: принимает ввод только внутри панели, оставляя карту доступной. */
class StoryChatterPanel(screen: BaseScreen, node: StoryDialogueNode, onContinue: () -> Unit) : Table() {
    private var submitted = false

    init {
        name = "story-chatter"
        touchable = Touchable.enabled
        background = BaseScreen.skinStrings.getUiBackground("WorldScreen/UnitTable")
        pad(12f)
        val panelWidth = minOf(620f, screen.stage.width * 0.6f)
        val contentWidth = (panelWidth - 24f).coerceAtLeast(40f)
        val text = Table()
        if (node.speaker.isNotBlank()) {
            text.add(node.speaker.toLabel().apply { wrap = true }).width(contentWidth).left().row()
        }
        text.add(node.text.toLabel().apply { wrap = true }).width(contentWidth).padTop(8f).left().row()
        val scroll = ScrollPane(text, BaseScreen.skin).apply { setScrollingDisabled(true, false) }
        add(scroll).width(contentWidth).height(minOf(text.prefHeight, screen.stage.height * 0.25f)).row()
        val next = TextButton("Continue", BaseScreen.skin)
        next.name = "story-chatter-continue"
        next.onActivation {
            if (!submitted && parent != null) {
                submitted = true
                try {
                    onContinue()
                } catch (failure: Exception) {
                    submitted = false
                    throw failure
                }
            }
        }
        add(next).right().minHeight(44f).padTop(8f).row()
        pack()
        setPosition((screen.stage.width - width) / 2f,
            (screen.stage.height - height - 100f).coerceAtLeast(0f))
    }
}
