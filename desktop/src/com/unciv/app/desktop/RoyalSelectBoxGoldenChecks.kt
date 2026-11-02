package com.unciv.app.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.ui.UncivStage
import com.unciv.ui.newgamescreen.TranslatedSelectBox
import com.unciv.ui.utils.BaseScreen

/** Exercises the real dropdown through Stage; does not approve a visual baseline. */
internal object RoyalSelectBoxGoldenChecks {
    fun open(screen: BaseScreen) {
        check(screen.game.settings.skin == "Minimal")
        try {
            for (width in listOf(360f, 1000f)) for (bottom in listOf(20f, 400f)) {
                val stage = UncivStage(FitViewport(width, 480f))
                try {
                    stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
                    val box = TranslatedSelectBox(listOf("Gold", "Production", "Science"), "Gold", BaseScreen.skin)
                    box.setBounds(20f, bottom, minOf(280f, width - 40f), box.prefHeight)
                    stage.addActor(box)
                    val changes = mutableListOf<String>()
                    box.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            changes.add(box.selected.value)
                        }
                    })
                    fun draw() {
                        repeat(3) { stage.act(1f / 3f) }
                        stage.draw()
                    }
                    fun click(actor: Actor, x: Float = actor.width / 2f, y: Float = actor.height / 2f) {
                        val point = actor.localToStageCoordinates(Vector2(x, y))
                        val hit = stage.hit(point.x, point.y, true)
                        check(hit === actor || hit?.isDescendantOf(actor) == true) { "Dropdown click target is obscured" }
                        stage.stageToScreenCoordinates(point)
                        stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                        stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                        draw()
                    }
                    fun openList() {
                        click(box)
                        val pane = box.scrollPane
                        check(pane.stage === stage && pane.isVisible)
                        check(pane.x >= -1f && pane.x + pane.width <= width + 1f)
                        check(pane.y >= -1f && pane.y + pane.height <= stage.height + 1f)
                    }
                    draw()
                    openList()
                    check(changes.isEmpty())
                    val list = box.list
                    click(list, list.width / 2f, list.height - list.itemHeight * 1.5f)
                    check(box.selected.value == "Production")
                    check(changes == listOf("Production")) { "Selection callback was missing or duplicated" }
                    check(box.scrollPane.parent == null)
                    box.isDisabled = true
                    click(box)
                    check(box.scrollPane.parent == null && box.selected.value == "Production")
                    check(changes == listOf("Production"))
                    box.isDisabled = false
                    openList()
                    click(list, list.width / 2f, list.height - list.itemHeight * 2.5f)
                    check(box.selected.value == "Science")
                    check(changes == listOf("Production", "Science"))
                    check(box.scrollPane.parent == null)
                } finally {
                    stage.dispose()
                }
            }
        } finally {
            screen.stage.viewport.apply()
        }
        screen.stage.addActor(com.badlogic.gdx.scenes.scene2d.ui.Label(
            "Royal dropdown input checks passed", BaseScreen.skin
        ).apply { setPosition(24f, 24f) })
    }
}
