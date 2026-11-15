package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.UncivGame
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.Fonts

/** Оформление окон принадлежит городской сцене, общие ресурсы Skin и игровые действия не меняются. */
internal object CityPopupStyle {
    private val ink = Color.valueOf("453925")

    fun appliesTo(stage: Stage): Boolean =
        UncivGame.Current.settings.skin == "Minimal" &&
            stage.root.findActor<Actor>("cityCompactRoot") != null

    /** Вызывается после конструктора наследника, до измерения окна; сохраняет рассчитанные им отступы. */
    fun prepare(popup: Popup) {
        if (!appliesTo(popup.stageToShowOn)) return
        popup.innerTable.background = HeroicCityStyle.panel()
        popup.innerTable.padLeft(maxOf(24f, popup.innerTable.padLeft))
        popup.innerTable.padRight(maxOf(24f, popup.innerTable.padRight))
        popup.innerTable.padTop(maxOf(30f, popup.innerTable.padTop))
        popup.innerTable.padBottom(maxOf(30f, popup.innerTable.padBottom))
        for (scroll in popup.children.filterIsInstance<ScrollPane>()) {
            popup.getCell(scroll).maxWidth((popup.stageToShowOn.width - 24f).coerceAtLeast(1f))
                .maxHeight((popup.stageToShowOn.height - 24f).coerceAtLeast(1f))
            scroll.setOverscroll(false, false)
        }
        // Таблицы статистики прокручивают собственное содержимое, сохраняя закрытие под ним.
        if (popup is DetailedStatsPopup || popup is TestFantasyPopup) {
            for (scroll in popup.innerTable.children.filterIsInstance<ScrollPane>()) {
                val width = (popup.stageToShowOn.width - 34f - popup.innerTable.padLeft -
                        popup.innerTable.padRight).coerceAtLeast(1f)
                val height = (popup.stageToShowOn.height - 120f - popup.innerTable.padTop -
                        popup.innerTable.padBottom).coerceAtLeast(1f)
                popup.innerTable.getCell(scroll).width(width).height(height)
                scroll.setOverscroll(false, false)
            }
        }
        refresh(popup.innerTable)
    }

    /** Обрабатывает динамическое содержимое; смысловые цвета и поля ввода остаются собственными. */
    fun refresh(actor: Actor) {
        if (actor is CheckBox) return
        if (actor is TextButton) {
            HeroicCityStyle.applyActionStyle(actor)
            actor.label.setFontScale(18f / Fonts.ORIGINAL_FONT_SIZE)
            return
        }
        if (actor is Label && actor.color == Color.WHITE && actor.style.fontColor == Color.WHITE) {
            actor.style = Label.LabelStyle(actor.style).apply { fontColor = ink.cpy() }
        }
        if (actor is Group) for (child in actor.children) refresh(child)
    }
}
