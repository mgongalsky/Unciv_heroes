package com.unciv.ui.playground

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.UncivGame
import com.unciv.logic.event.EventBus
import com.unciv.logic.multiplayer.FriendList
import com.unciv.models.metadata.Player
import com.unciv.ui.UncivStage
import com.unciv.ui.newgamescreen.FriendSelectionPopup
import com.unciv.ui.newgamescreen.PlayerPickerTable
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.extensions.isEnabled
import com.unciv.ui.utils.extensions.stageBoundingBox

internal fun verifyFriendSelection(screen: BaseScreen, picker: PlayerPickerTable, player: Player) {
    if (UncivGame.Current.settings.skin != "Minimal") return
    val friends = UncivGame.Current.settings.multiplayer.friendList
    val originalFriends = friends.toList()
    val originalId = player.playerId
    val stage = screen.stage as UncivStage
    val originalViewport = stage.viewport
    val originalArea = Rectangle(stage.lastKnownVisibleArea)
    val friend = FriendList.Friend(
        "A friend with a long display name ".repeat(3).trim(),
        "00000000-0000-0000-0000-000000000123"
    )
    fun buttons(actor: Actor): List<TextButton> = when (actor) {
        is TextButton -> listOf(actor)
        is Group -> actor.children.flatMap { buttons(it) }
        else -> emptyList()
    }
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
        friends.clear()
        friends.add(friend)
        for (width in listOf(360f, 1000f)) {
            stage.viewport = FitViewport(width, 480f)
            stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
            EventBus.send(UncivStage.VisibleAreaChanged(Rectangle(0f, 0f, width, 480f)))
            for (confirm in listOf(false, true)) {
                player.playerId = ""
                val popup = FriendSelectionPopup(picker, player, screen)
                val detail = "Friend selection at ${width}x480, confirm=$confirm"
                try {
                    popup.open(force = true)
                    popup.validate()
                    stage.draw()
                    check(popup.innerTable.background is RoyalPanelDrawable) { detail }
                    check(!popup.pickerPane.rightSideButton.isEnabled) { detail }
                    val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                    val bounds = scroll.stageBoundingBox
                    check(bounds.x >= -1f && bounds.y >= -1f && bounds.x + bounds.width <= width + 1f && bounds.y + bounds.height <= 481f) { detail }
                    val friendButton = buttons(popup.pickerPane.topTable).single { it.text.toString() == friend.name }
                    check(friendButton.label.wrap && friendButton.label.height + 1f >= friendButton.label.prefHeight) { "Clipped friend name: $detail" }
                    check(scroll.maxX <= 1f && popup.pickerPane.scrollPane.maxX <= 1f) { detail }
                    click(friendButton, detail)
                    stage.draw()
                    check(popup.pickerPane.rightSideButton.isEnabled && player.playerId.isEmpty()) { detail }
                    val action = popup.pickerPane.rightSideButton
                    check(action.label.wrap && action.label.height + 1f >= action.label.prefHeight) { "Clipped selection action: $detail" }
                    click(if (confirm) action else popup.pickerPane.closeButton, detail)
                    check(popup.parent == null) { detail }
                    check(player.playerId == if (confirm) friend.playerID else "") { detail }
                } finally {
                    popup.events.stopReceiving()
                    popup.remove()
                }
            }
        }
    } finally {
        friends.clear()
        friends.addAll(originalFriends)
        player.playerId = originalId
        picker.update()
        stage.viewport = originalViewport
        originalViewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        EventBus.send(UncivStage.VisibleAreaChanged(originalArea))
        for (actor in stage.actors) (actor as? Layout)?.invalidateHierarchy()
    }
}
