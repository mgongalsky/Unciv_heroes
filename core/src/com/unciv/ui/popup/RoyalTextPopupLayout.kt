package com.unciv.ui.popup

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.UncivGame
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable

/** Opt-in layout for scrollable popups containing direct labels and buttons.
 * Call after populating the popup, and again after replacing its content.
 * Nested tables and multi-column layouts must be configured by their caller.
 */
fun Popup.applyRoyalTextLayout() {
    if (UncivGame.Current.settings.skin != "Minimal") return
    val scroll = children.filterIsInstance<ScrollPane>().single()
    innerTable.background = RoyalPanelDrawable(BaseScreen.skin)
    innerTable.pad(52f).padTop(58f)
    val style = scroll.style
    val chrome = innerTable.padLeft + innerTable.padRight + 10f +
        maxOf(style.vScroll?.minWidth ?: 0f, style.vScrollKnob?.minWidth ?: 0f) +
        (style.background?.leftWidth ?: 0f) + (style.background?.rightWidth ?: 0f)
    val contentWidth = minOf(700f, stageToShowOn.width - 24f - chrome).coerceAtLeast(1f)
    getCell(scroll).width(contentWidth + chrome)
        .maxHeight((stageToShowOn.height - 24f).coerceAtLeast(1f))
    for (actor in innerTable.children) {
        when (actor) {
            is Label -> {
                actor.color = Color.valueOf("453925")
                actor.wrap = true
                innerTable.getCell(actor).width(contentWidth)
            }
            is TextButton -> {
                actor.label.wrap = true
                actor.labelCell.width(
                    (contentWidth - actor.padLeft - actor.padRight - 20f).coerceAtLeast(1f)
                ).pad(10f)
                innerTable.getCell(actor).width(contentWidth).minHeight(48f).fillX()
            }
        }
    }
    invalidateHierarchy()
}
