package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.unciv.UncivGame
import com.unciv.models.translations.tr
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable

/** Displays a failed placement without retrying or changing the purchase. */
class ConstructionPurchaseFailurePopup(
    stage: Stage,
    constructionName: String,
    cityName: String
) : Popup(stage) {
    init {
        val message = "No space available to place [$constructionName] near [$cityName]"
        if (UncivGame.Current.settings.skin == "Minimal") {
            innerTable.background = RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
            val scroll = children.filterIsInstance<ScrollPane>().single()
            val style = scroll.style
            val chrome = innerTable.padLeft + innerTable.padRight + 10f +
                maxOf(style.vScroll?.minWidth ?: 0f, style.vScrollKnob?.minWidth ?: 0f) +
                (style.background?.leftWidth ?: 0f) + (style.background?.rightWidth ?: 0f)
            val contentWidth = minOf(700f, stageToShowOn.width - 24f - chrome).coerceAtLeast(1f)
            getCell(scroll).width(contentWidth + chrome)
                .maxHeight((stageToShowOn.height - 24f).coerceAtLeast(1f))
            addGoodSizedLabel(message).apply {
                actor.color = Color.valueOf("453925")
                width(contentWidth)
            }.row()
            val closeCell = addCloseButton()
            val button = closeCell.actor
            button.label.wrap = true
            button.labelCell.width(
                (contentWidth - button.padLeft - button.padRight - 20f).coerceAtLeast(1f)
            ).pad(10f)
            closeCell.width(contentWidth).minHeight(48f).fillX()
        } else {
            add(message.tr()).row()
            addCloseButton()
        }
    }
}
