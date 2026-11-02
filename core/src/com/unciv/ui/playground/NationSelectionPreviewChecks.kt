package com.unciv.ui.playground

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.UncivGame
import com.unciv.logic.event.EventBus
import com.unciv.models.metadata.Player
import com.unciv.ui.UncivStage
import com.unciv.ui.newgamescreen.PlayerPickerTable
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.extensions.OnClickListener
import com.unciv.ui.utils.extensions.stageBoundingBox

internal fun verifyNationSelection(screen: BaseScreen, picker: PlayerPickerTable, player: Player) {
    if (UncivGame.Current.settings.skin != "Minimal") return
    val stage = screen.stage as UncivStage
    val originalViewport = stage.viewport
    val originalArea = Rectangle(stage.lastKnownVisibleArea)
    val originalNation = player.chosenCiv
    val target = picker.getAvailablePlayerCivs().first { it.name != originalNation }.name
    fun click(actor: Actor, detail: String) {
        var ancestor = actor.parent
        while (ancestor != null) {
            if (ancestor is ScrollPane) {
                val local = actor.localToAscendantCoordinates(ancestor.actor, Vector2())
                ancestor.scrollTo(local.x, local.y, actor.width, actor.height)
                ancestor.updateVisualScroll()
                stage.draw()
            }
            ancestor = ancestor.parent
        }
        val point = actor.localToStageCoordinates(Vector2(actor.width / 2f, actor.height / 2f))
        val hit = stage.hit(point.x, point.y, true)
        check(hit === actor || hit?.isDescendantOf(actor) == true) { "Unreachable control: $detail" }
        stage.stageToScreenCoordinates(point)
        stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
        stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
    }
    try {
        for (width in listOf(360f, 1000f)) {
            stage.viewport = FitViewport(width, 480f)
            stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
            EventBus.send(UncivStage.VisibleAreaChanged(Rectangle(0f, 0f, width, 480f)))
            for (confirm in listOf(false, true)) {
                player.chosenCiv = originalNation
                picker.update()
                val previousActors = stage.actors.toList()
                val playerRow = picker.playerListTable.children.first() as Table
                val nationControl = playerRow.children.first()
                nationControl.listeners.filterIsInstance<OnClickListener>().single().function(null, 0f, 0f)
                val popup = stage.actors.filterIsInstance<Popup>().single { it !in previousActors }
                val detail = "Nation selection at ${width}x480, confirm=$confirm"
                try {
                    popup.validate()
                    stage.draw()
                    check(popup.innerTable.background is RoyalPanelDrawable) { detail }
                    val outer = popup.children.filterIsInstance<ScrollPane>().single()
                    val list = popup.findActor<ScrollPane>("nation-list-scroll")
                    val details = popup.findActor<ScrollPane>("nation-details-scroll")
                    val bounds = outer.stageBoundingBox
                    check(bounds.x >= -1f && bounds.y >= -1f && bounds.x + bounds.width <= width + 1f && bounds.y + bounds.height <= 481f) { detail }
                    check(outer.maxX <= 1f && list.maxX <= 1f && details.maxX <= 1f) {
                        "Horizontal overflow: $detail, outer=${outer.maxX}, list=${list.maxX}, details=${details.maxX}"
                    }
                    val option = popup.findActor<Actor>("nation-option-$target")
                    click(option, detail)
                    stage.draw()
                    check(player.chosenCiv == originalNation) { "Selection committed before confirmation: $detail" }
                    check(details.maxX <= 1f) { "Selected description overflows: $detail, maxX=${details.maxX}" }
                    val action = popup.findActor<Actor>(if (confirm) "nation-picker-confirm" else "nation-picker-close")
                    click(action, detail)
                    check(popup.parent == null) { detail }
                    check(player.chosenCiv == if (confirm) target else originalNation) { detail }
                } finally {
                    popup.events.stopReceiving()
                    popup.remove()
                }
            }
        }
    } finally {
        player.chosenCiv = originalNation
        stage.viewport = originalViewport
        originalViewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        EventBus.send(UncivStage.VisibleAreaChanged(originalArea))
        picker.update()
        for (actor in stage.actors) (actor as? Layout)?.invalidateHierarchy()
    }
}
