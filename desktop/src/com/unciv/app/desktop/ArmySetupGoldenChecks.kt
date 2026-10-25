package com.unciv.app.desktop

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.unciv.ui.battlescreen.BattleArmyPicker
import com.unciv.ui.battlescreen.BattleArmySetupScreen
import com.unciv.ui.popup.ConfirmPopup
import com.unciv.ui.utils.BaseScreen

/** Exercises the production editor after its responsive layout has settled. */
internal object ArmySetupGoldenChecks {
    private fun descendants(actor: Actor): Sequence<Actor> = sequence {
        yield(actor)
        if (actor is Group) for (child in actor.children) yieldAll(descendants(child))
    }

    fun screen(): BaseScreen = networkSetup().also { screen ->
        screen.stage.addAction(object : Action() {
            private var frames = 0
            override fun act(delta: Float): Boolean {
                if (++frames < 5) return false
                val actors = descendants(screen.stage.root).toList()
                val picker = actors.filterIsInstance<BattleArmyPicker>().single()
                picker.useTemplate()
                val before = picker.selectedArmy()
                check(before.size == 7) { "Expected seven editable stacks" }
                picker.useTemplate()
                val confirmation = screen.stage.actors.filterIsInstance<ConfirmPopup>().single()
                check(picker.selectedArmy() == before) { "Preset changed army before confirmation" }
                confirmation.close()
                check(picker.selectedArmy() == before) { "Cancelling replacement changed army" }
                screen.stage.root.validate()
                val scroll =
                        actors.filterIsInstance<ScrollPane>().single { picker.isDescendantOf(it) }
                scroll.validate()
                check(scroll.maxY <= 1f) {
                    "Army editor requires vertical scrolling: ${scroll.maxY}, stage ${screen.stage.width}x${screen.stage.height}"
                }
                check(scroll.maxX <= 1f) { "Army editor requires horizontal scrolling" }
                val ready = actors.single { it.name == "armyReadyButton" }
                check(!ready.isDescendantOf(scroll)) { "Ready must remain outside the scroll area" }
                fun visible(actor: Actor) {
                    val bottom = actor.localToStageCoordinates(Vector2())
                    val top = actor.localToStageCoordinates(Vector2(actor.width, actor.height))
                    check(
                        bottom.x >= 0f && bottom.y >= 0f &&
                                top.x <= screen.stage.width + 1f && top.y <= screen.stage.height + 1f
                    ) {
                        "${actor.name} is outside the stage"
                    }
                }
                visible(ready)
                for (slot in 1..7) {
                    val row = actors.single { it.name == "armyStack$slot" }
                    visible(row)
                    val bottom = row.localToAscendantCoordinates(scroll, Vector2())
                    val top =
                            row.localToAscendantCoordinates(scroll, Vector2(row.width, row.height))
                    check(bottom.y >= 0f && top.y <= scroll.height + 1f) {
                        "Stack $slot is clipped by the editor viewport"
                    }
                }
                return true
            }
        })
    }
    private fun Group.validate() {
        descendants(this).filterIsInstance<com.badlogic.gdx.scenes.scene2d.utils.Layout>()
            .forEach { it.validate() }
    }

    private fun networkSetup(): BaseScreen =
            com.unciv.ui.playground.createNetworkArmySetupPreview()
}
