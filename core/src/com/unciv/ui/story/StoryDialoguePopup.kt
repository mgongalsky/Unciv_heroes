package com.unciv.ui.story

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.unciv.pure.domain.story.StoryChoice
import com.unciv.pure.domain.story.StoryDialogueNode
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel

/** Показывает снимок реплики; игровые переходы выполняет переданный координатор. */
class StoryDialoguePopup(
    screen: BaseScreen,
    node: StoryDialogueNode,
    choices: List<StoryChoice>,
    private val onResponse: (String?) -> Boolean
) : Popup(screen) {
    private var submitted = false

    init {
        val royal = screen.game.settings.skin == "Minimal"
        val contentWidth: Float
        if (royal) {
            innerTable.background = com.unciv.ui.utils.RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
            val scroll =
                children.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>().single()
            val scrollStyle = scroll.style
            val chrome = innerTable.padLeft + innerTable.padRight + 10f +
                    maxOf(
                        scrollStyle.vScroll?.minWidth ?: 0f,
                        scrollStyle.vScrollKnob?.minWidth ?: 0f
                    ) +
                    (scrollStyle.background?.leftWidth ?: 0f) + (scrollStyle.background?.rightWidth
                ?: 0f)
            contentWidth = minOf(700f, stageToShowOn.width - 24f - chrome).coerceAtLeast(1f)
            getCell(scroll).width(contentWidth + chrome)
                .maxHeight((stageToShowOn.height - 24f).coerceAtLeast(1f))
        } else {
            contentWidth = minOf(700f, stageToShowOn.width - 100f).coerceAtLeast(120f)
        }
        val ink = if (royal) com.badlogic.gdx.graphics.Color.valueOf("453925")
        else com.badlogic.gdx.graphics.Color.WHITE
        val header = Table()
        val portrait: Actor =
            if (node.portrait.isNotBlank() && ImageGetter.imageExists(node.portrait)) {
                ImageGetter.getImage(node.portrait).apply { setScaling(Scaling.fit) }
            } else {
                ImageGetter.getRandomNationIndicator(64f)
            }
        portrait.name = "story-portrait"
        val speaker = node.speaker.toLabel(fontColor = ink).apply {
            name = "story-speaker"
            wrap = true
            setAlignment(Align.left)
        }
        if (royal && contentWidth < 240f) {
            header.add(portrait).size(minOf(64f, contentWidth)).left().padBottom(10f).row()
            header.add(speaker).width(contentWidth).left()
        } else {
            header.add(portrait).size(64f).padRight(16f)
            header.add(speaker).width((contentWidth - 80f).coerceAtLeast(40f)).left()
        }
        add(header).width(contentWidth).left().row()
        add(node.text.toLabel(fontColor = ink).apply {
            name = "story-text"
            wrap = true
            setAlignment(Align.left)
        }).width(contentWidth).padTop(12f).padBottom(16f).left().row()
        if (node.choices.isEmpty()) {
            addResponse("Continue", null, contentWidth)
        } else {
            for (choice in choices) addResponse(choice.text, choice.id, contentWidth)
        }
    }

    /** Кнопки переносят длинные ответы; окно допускает прокрутку на небольшом экране. */
    private fun addResponse(text: String, choiceId: String?, contentWidth: Float) {
        val cell = addButton(text) { submit(choiceId) }
        val button = cell.actor
        button.name = "story-choice:${choiceId ?: "continue"}"
        button.label.wrap = true
        button.label.setAlignment(Align.left)
        if (com.unciv.UncivGame.Current.settings.skin == "Minimal") {
            val labelWidth =
                    (contentWidth - button.padLeft - button.padRight - 20f).coerceAtLeast(1f)
            button.labelCell.width(labelWidth).pad(10f)
        } else {
            button.labelCell.width((contentWidth - 40f).coerceAtLeast(40f)).pad(10f)
        }
        cell.width(contentWidth).minHeight(48f).fillX().row()
    }

    /** Устаревшую команду координатор отклоняет; принятую нельзя повторить старым обработчиком. */
    private fun submit(choiceId: String?) {
        if (submitted || parent == null) return
        submitted = true
        try {
            if (onResponse(choiceId)) close()
            else submitted = false
        } catch (failure: Exception) {
            submitted = false
            throw failure
        }
    }
}
