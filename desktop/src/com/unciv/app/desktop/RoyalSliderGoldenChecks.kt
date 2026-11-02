package com.unciv.app.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.ui.UncivStage
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalSliderDrawable

/** Проверяет ввод в обеих ориентациях; capture-only не утверждает внешний вид. */
internal object RoyalSliderGoldenChecks {
    fun open(screen: BaseScreen) {
        check(screen.game.settings.skin == "Minimal")
        for (vertical in listOf(false, true)) {
            val stage = UncivStage(FitViewport(360f, 480f))
            try {
                stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
                val slider = Slider(0f, 10f, 2f, vertical, BaseScreen.skin)
                slider.setBounds(40f, 80f, if (vertical) 40f else 280f, if (vertical) 320f else 40f)
                stage.addActor(slider)
                check(slider.style.knob is RoyalSliderDrawable)
                check(slider.style.disabledKnob is RoyalSliderDrawable)
                val changes = mutableListOf<Float>()
                slider.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        changes.add(slider.value)
                    }
                })
                fun point(fraction: Float): Vector2 {
                    val position = slider.localToStageCoordinates(Vector2(
                        if (vertical) slider.width / 2f else slider.width * fraction,
                        if (vertical) slider.height * fraction else slider.height / 2f
                    ))
                    stage.stageToScreenCoordinates(position)
                    return position
                }
                fun drag(from: Float, to: Float) {
                    stage.draw()
                    val start = point(from)
                    val end = point(to)
                    stage.touchDown(start.x.toInt(), start.y.toInt(), 0, Input.Buttons.LEFT)
                    stage.touchDragged(end.x.toInt(), end.y.toInt(), 0)
                    stage.touchUp(end.x.toInt(), end.y.toInt(), 0, Input.Buttons.LEFT)
                    stage.draw()
                    check(!slider.isDragging)
                }
                drag(0.01f, 0.99f)
                check(slider.value == 10f && changes.isNotEmpty())
                check(changes.all { it in 0f..10f && it % 2f == 0f })
                slider.isDisabled = true
                val before = changes.toList()
                drag(0.99f, 0.01f)
                check(slider.value == 10f && changes == before)
                slider.isDisabled = false
                drag(0.99f, 0.01f)
                check(slider.value == 0f)
                drag(0.01f, 0.5f)
                check(slider.value in 4f..6f && slider.value % 2f == 0f)
            } finally {
                stage.dispose()
            }
        }
        screen.stage.viewport.apply()
        screen.stage.addActor(com.badlogic.gdx.scenes.scene2d.ui.Label(
            "Royal slider input checks passed", BaseScreen.skin
        ).apply { setPosition(24f, 24f) })
    }
}
