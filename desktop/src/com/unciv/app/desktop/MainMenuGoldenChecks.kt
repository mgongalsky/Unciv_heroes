package com.unciv.app.desktop

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.unciv.MainMenuScreen
import com.unciv.ui.mapeditor.EditorMapHolder
import com.unciv.ui.utils.AutoScrollPane

/** Проверяет геометрию настоящего меню после завершения фоновой загрузки. */
internal object MainMenuGoldenChecks {
    private val phases = java.util.WeakHashMap<MainMenuScreen, Int>()
    private fun descendants(actor: Actor): Sequence<Actor> = sequence {
        yield(actor)
        if (actor is Group)
            for (child in actor.children.toList()) yieldAll(descendants(child))
    }

    fun ready(screen: MainMenuScreen): Boolean {
        if (phases[screen] == 2) return true
        val map = descendants(screen.stage.root).filterIsInstance<EditorMapHolder>().firstOrNull()
            ?: return false
        if (map.parent?.hasActions() == true) return false
        val scroll = screen.stage.actors.filterIsInstance<AutoScrollPane>().single()
        scroll.validate()
        val buttons = descendants(scroll).filterIsInstance<Button>().toList()
        check(buttons.size >= 7) { "Главное меню потеряло обязательные действия" }
        val last = buttons.last()
        if (phases[screen] == 1) {
            // Предыдущий кадр задал прокрутку; draw обновил положение содержимого.
            val center = last.localToStageCoordinates(Vector2(last.width / 2f, last.height / 2f))
            val hit = screen.stage.hit(center.x, center.y, true)
            check(hit != null && (hit === last || hit.isDescendantOf(last))) {
                "Последний пункт меню недоступен после прокрутки: " +
                        "center=$center, scrollY=${scroll.scrollY}, maxY=${scroll.maxY}, " +
                        "stage=${screen.stage.width}x${screen.stage.height}, hit=${hit?.javaClass?.name}"
            }
            scroll.scrollY = 0f
            scroll.updateVisualScroll()
            phases[screen] = 2
            return true
        }
        for (button in buttons) {
            val position = button.localToStageCoordinates(Vector2())
            check(position.x >= -1f && position.x + button.width <= screen.stage.width + 1f) {
                "Кнопка главного меню выходит за горизонтальные границы сцены"
            }
        }
        val label = buttons.first().children.filterIsInstance<Label>().single()
        val original = label.text.toString()
        try {
            label.setText("Очень длинное название действия главного меню для проверки переноса текста")
            (label.parent as Layout).invalidateHierarchy()
            scroll.validate()
            check(label.wrap && label.glyphLayout.runs.size > 1) {
                "Длинная подпись главного меню не переносится"
            }
            check(label.height + 1f >= label.prefHeight) { "Длинная подпись обрезана по высоте" }
        } finally {
            label.setText(original)
            (label.parent as Layout).invalidateHierarchy()
            scroll.validate()
        }
        val position = last.localToActorCoordinates(scroll.actor, Vector2())
        scroll.scrollTo(position.x, position.y, last.width, last.height, false, true)
        scroll.updateVisualScroll()
        phases[screen] = 1
        return false
    }
}
