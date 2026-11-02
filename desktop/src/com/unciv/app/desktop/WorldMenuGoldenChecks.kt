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
        verifyCommunity(world)
    }

    fun verifyCommunity(world: WorldScreen) {
        val stage = world.stage as com.unciv.ui.UncivStage
        val viewport = stage.viewport
        val originalWidth = viewport.worldWidth
        val originalHeight = viewport.worldHeight
        val originalVisibleArea = com.badlogic.gdx.math.Rectangle(stage.lastKnownVisibleArea)
        fun sendVisibleArea(area: com.badlogic.gdx.math.Rectangle) {
            com.unciv.logic.event.EventBus.send(com.unciv.ui.UncivStage.VisibleAreaChanged(area))
        }
        try {
            for ((width, height) in listOf(360f to 480f, 1000f to 600f)) {
                viewport.setWorldSize(width, height)
                sendVisibleArea(com.badlogic.gdx.math.Rectangle(0f, 0f, width, height))
                val menu = WorldScreenMenuPopup(world)
                var community: com.unciv.ui.worldscreen.mainmenu.WorldScreenCommunityPopup? = null
                fun click(button: TextButton, scroll: ScrollPane) {
                    scroll.validate()
                    val local = button.localToAscendantCoordinates(scroll.actor, Vector2())
                    scroll.scrollTo(local.x, local.y, button.width, button.height)
                    scroll.updateVisualScroll()
                    stage.draw()
                    val point = button.localToStageCoordinates(
                        Vector2(
                            button.width / 2f,
                            button.height / 2f
                        )
                    )
                    val hit = stage.hit(point.x, point.y, true)
                    check(hit != null && (hit === button || hit.isDescendantOf(button))) {
                        "Кнопка Community недоступна: stage=${width}x$height text=${button.text}"
                    }
                    stage.stageToScreenCoordinates(point)
                    stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                    stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                }
                try {
                    menu.open(force = true)
                    menu.validate()
                    val menuButtons = menu.innerTable.children.filterIsInstance<TextButton>()
                    check(menuButtons.size == 9)
                    click(menuButtons[7], menu.children.filterIsInstance<ScrollPane>().single())
                    community =
                            stage.actors.filterIsInstance<com.unciv.ui.worldscreen.mainmenu.WorldScreenCommunityPopup>()
                                .single()
                    check(menu.parent == null) { "Переход Community оставил игровое меню открытым" }
                    val popup = community
                    var closed = 0
                    popup.closeListeners.add { closed++ }
                    popup.validate()
                    val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                    val buttons = popup.innerTable.children.filterIsInstance<TextButton>()
                    check(buttons.size == 4)
                    check(buttons.take(3).map { it.text.toString() } == listOf(
                        "Discord",
                        "Github",
                        "Reddit"
                    ))
                    val first = buttons.first()
                    val originalText = first.text.toString()
                    try {
                        first.setText("Очень длинное название сообщества для проверки переноса подписи")
                        first.invalidateHierarchy()
                        popup.validate()
                        stage.draw()
                        val bounds = scroll.stageBoundingBox
                        check(
                            bounds.x >= -1f && bounds.y >= -1f &&
                                    bounds.x + bounds.width <= width + 1f && bounds.y + bounds.height <= height + 1f
                        ) {
                            "Community выходит за сцену ${width}x$height: $bounds"
                        }
                        check(scroll.maxX <= 1f) { "Community переполнено по горизонтали" }
                        check(first.label.wrap && first.label.glyphLayout.runs.size > 1)
                        check(first.label.height + 1f >= first.label.prefHeight) { "Подпись Community обрезана" }
                    } finally {
                        first.setText(originalText)
                        first.invalidateHierarchy()
                    }
                    popup.validate()
                    click(buttons.last(), scroll)
                    check(popup.parent == null && closed == 1) { "Close не закрыл Community однократно" }
                } finally {
                    community?.let { if (it.parent != null) it.close() }
                    if (menu.parent != null) menu.close()
                }
            }
        } finally {
            viewport.setWorldSize(originalWidth, originalHeight)
            sendVisibleArea(originalVisibleArea)
            for (actor in stage.actors)
                (actor as? com.badlogic.gdx.scenes.scene2d.utils.Layout)?.invalidateHierarchy()
        }
    }
}
