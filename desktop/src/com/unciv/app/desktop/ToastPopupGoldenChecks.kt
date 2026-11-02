package com.unciv.app.desktop

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.UncivGame
import com.unciv.ui.UncivStage
import com.unciv.ui.popup.Popup
import com.unciv.ui.popup.ToastPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.stageBoundingBox

/** Проверяет настоящую раскладку уведомления и однократное закрытие без утверждения эталона. */
internal object ToastPopupGoldenChecks {
    fun open(screen: BaseScreen) {
        check(UncivGame.Current.settings.skin == "Minimal")
        val message = List(60) {
            "The visiting army has arrived at the royal city. Review the available troops before continuing."
        }.joinToString(" ")
        for (width in listOf(360f, 1000f)) {
            // Отдельная сцена сразу получает согласованные viewport и видимую область.
            val stage = UncivStage(FitViewport(width, 480f))
            try {
                check(stage.width == width && stage.height == 480f) { "Unexpected stage size: ${stage.width}x${stage.height}" }
                val toast = ToastPopup(message, stage, time = 60_000L)
                var closed = 0
                toast.closeListeners.add { closed++ }
                try {
                    toast.validate()
                    val scroll = toast.children.filterIsInstance<ScrollPane>().single()
                    scroll.validate()
                    toast.innerTable.validate()
                    val bounds = toast.stageBoundingBox
                    val dimensions = "stage=$width toast=$bounds scroll=${scroll.width}x${scroll.height} maxX=${scroll.maxX} maxY=${scroll.maxY}"
                    check(toast.isVisible)
                    check(bounds.x >= -1f && bounds.x + bounds.width <= width + 1f) { dimensions }
                    check(bounds.y >= -1f && bounds.y + bounds.height <= 480f + 1f) { dimensions }
                    check(scroll.width > 0f && scroll.height > 0f) { dimensions }
                    check(scroll.maxX <= 1f) { dimensions }
                    check(scroll.maxY > 0f) { "Длинное уведомление должно прокручиваться: $dimensions" }
                    scroll.scrollY = scroll.maxY
                    scroll.updateVisualScroll()
                    scroll.layout()
                    check(kotlin.math.abs(scroll.scrollY - scroll.maxY) <= 1f) { dimensions }

                    val queued = Popup(stage)
                    var shown = 0
                    queued.showListeners.add { shown++ }
                    queued.addGoodSizedLabel("Next notification")
                    queued.open()
                    try {
                        check(!queued.isVisible)
                        val clickPosition = toast.localToStageCoordinates(
                            com.badlogic.gdx.math.Vector2(toast.width / 2f, toast.height / 2f)
                        )
                        stage.stageToScreenCoordinates(clickPosition)
                        stage.touchDown(clickPosition.x.toInt(), clickPosition.y.toInt(), 0, com.badlogic.gdx.Input.Buttons.LEFT)
                        stage.touchUp(clickPosition.x.toInt(), clickPosition.y.toInt(), 0, com.badlogic.gdx.Input.Buttons.LEFT)
                        check(toast.parent == null && closed == 1)
                        check(queued.isVisible && shown == 1)
                        toast.close()
                        check(closed == 1 && shown == 1)
                    } finally {
                        queued.close()
                    }
                } finally {
                    toast.close()
                }
            } finally {
                stage.dispose()
            }
        }
        // Статическая подпись для захвата; тестовые уведомления и их таймеры уже закрыты.
        screen.stage.addActor(com.badlogic.gdx.scenes.scene2d.ui.Label(
            "Toast layout and close checks passed", BaseScreen.skin
        ).apply { setPosition(24f, 24f) })
    }
}
