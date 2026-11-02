package com.unciv.app.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.ui.UncivStage
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalCheckBoxDrawable
import com.unciv.ui.utils.extensions.toCheckBox

/** Проверка поведения настоящего CheckBox; внешний вид требует отдельной приёмки. */
internal object RoyalCheckBoxGoldenChecks {
    fun open(screen: BaseScreen) {
        check(screen.game.settings.skin == "Minimal")
        for (width in listOf(360f, 1000f)) {
            val stage = UncivStage(FitViewport(width, 480f))
            try {
                stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
                val changes = mutableListOf<Boolean>()
                val checkbox = "Royal checkbox".toCheckBox(false) { changes.add(it) }
                val style = checkbox.style
                check(listOf(style.checkboxOff, style.checkboxOn, style.checkboxOver,
                    style.checkboxOnOver, style.checkboxOffDisabled, style.checkboxOnDisabled)
                    .all { it is RoyalCheckBoxDrawable })
                checkbox.label.setWrap(true)
                val table = Table().apply {
                    setSize(width - 40f, 200f)
                    setPosition(20f, 140f)
                    add(checkbox).width(width - 60f).fillX()
                }
                stage.addActor(table)
                fun draw() {
                    table.validate()
                    stage.draw()
                }
                fun click() {
                    draw()
                    val point = checkbox.localToStageCoordinates(Vector2(checkbox.width / 2f, checkbox.height / 2f))
                    val hit = stage.hit(point.x, point.y, true)
                    check(hit != null && (hit === checkbox || hit.isDescendantOf(checkbox)))
                    stage.stageToScreenCoordinates(point)
                    stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                    stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                    draw()
                }
                click()
                check(checkbox.isChecked && changes == listOf(true))
                checkbox.isDisabled = true
                click()
                check(checkbox.isChecked && changes == listOf(true))
                check(checkbox.image.drawable === style.checkboxOnDisabled)
                checkbox.isDisabled = false
                click()
                check(!checkbox.isChecked && changes == listOf(true, false))
                checkbox.isDisabled = true
                click()
                check(!checkbox.isChecked && changes == listOf(true, false))
                check(checkbox.image.drawable === style.checkboxOffDisabled)
                checkbox.isDisabled = false
                checkbox.setText(List(8) { "Длинная подпись флажка" }.joinToString(" "))
                checkbox.invalidateHierarchy()
                draw()
                check(checkbox.label.glyphLayout.runs.size > 1)
                check(checkbox.label.height + 1f >= checkbox.label.prefHeight)
                check(checkbox.width <= width - 40f)
                check(changes == listOf(true, false))
            } finally {
                stage.dispose()
            }
        }
        screen.stage.viewport.apply()
        screen.stage.addActor(com.badlogic.gdx.scenes.scene2d.ui.Label(
            "Royal checkbox behavior checks passed", BaseScreen.skin
        ).apply { setPosition(24f, 24f) })
    }
}
