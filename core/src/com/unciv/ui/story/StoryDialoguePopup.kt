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
        val contentWidth = minOf(700f, stageToShowOn.width - 100f).coerceAtLeast(120f)
        val header = Table()
        val portrait: Actor = if (node.portrait.isNotBlank() && ImageGetter.imageExists(node.portrait)) {
            ImageGetter.getImage(node.portrait).apply { setScaling(Scaling.fit) }
        } else {
            ImageGetter.getRandomNationIndicator(64f)
        }
        portrait.name = "story-portrait"
        header.add(portrait).size(64f).padRight(16f)
        header.add(node.speaker.toLabel().apply {
            name = "story-speaker"
            wrap = true
            setAlignment(Align.left)
        }).width((contentWidth - 80f).coerceAtLeast(40f)).left()
        add(header).width(contentWidth).left().row()
        add(node.text.toLabel().apply {
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
        cell.actor.name = "story-choice:${choiceId ?: "continue"}"
        cell.actor.label.wrap = true
        cell.actor.label.setAlignment(Align.left)
        cell.actor.labelCell.width((contentWidth - 40f).coerceAtLeast(40f)).pad(10f)
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
