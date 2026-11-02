package com.unciv.app.desktop

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.ui.utils.extensions.stageBoundingBox
import com.unciv.ui.worldscreen.WorldScreen
import com.unciv.ui.worldscreen.mainmenu.WorldScreenMenuPopup

/** Проверяет размеры игрового меню и закрытие настоящим событием указателя. */
internal object WorldMenuGoldenChecks {
    fun verify(world: WorldScreen) {
        check(world.game.settings.skin == "Minimal")
        val stage = world.stage
        val popup = WorldScreenMenuPopup(world)
        var closed = 0
        popup.closeListeners.add { closed++ }
        try {
            popup.open(force = true)
            popup.validate()
            val scroll = popup.children.filterIsInstance<ScrollPane>().single()
            scroll.validate()
            popup.innerTable.validate()
            val bounds = scroll.stageBoundingBox
            check(bounds.x >= -1f && bounds.x + bounds.width <= stage.width + 1f)
            check(bounds.y >= -1f && bounds.y + bounds.height <= stage.height + 1f)
            check(scroll.maxX <= 1f) { "Игровое меню переполнено по горизонтали" }
            val buttons = popup.innerTable.children.filterIsInstance<TextButton>()
            check(buttons.size == 9) { "Изменился набор действий игрового меню" }
            val first = buttons.first()
            val original = first.text.toString()
            try {
                first.setText("Очень длинное название действия игрового меню для проверки переноса")
                first.invalidateHierarchy()
                popup.validate()
                scroll.validate()
                popup.innerTable.validate()
                first.validate()
                check(first.label.wrap && first.label.glyphLayout.runs.size > 1)
                check(first.label.height + 1f >= first.label.prefHeight) {
                    "Подпись игрового меню обрезана по высоте"
                }
            } finally {
                first.setText(original)
                first.invalidateHierarchy()
            }
            popup.validate()
            scroll.validate()
            val close = buttons.last()
            scroll.scrollY = scroll.maxY
            scroll.updateVisualScroll()
            // Отрисовка применяет визуальную позицию содержимого перед Stage.hit.
            stage.draw()
            val point = close.localToStageCoordinates(Vector2(close.width / 2f, close.height / 2f))
            val hit = stage.hit(point.x, point.y, true)
            check(hit != null && (hit === close || hit.isDescendantOf(close))) {
                "Close недоступен после прокрутки игрового меню: $point"
            }
            stage.stageToScreenCoordinates(point)
            stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
            stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
            check(popup.parent == null && closed == 1) { "Close не закрыл игровое меню" }
        } finally {
            if (popup.parent != null) popup.close()
        }
    }
}
