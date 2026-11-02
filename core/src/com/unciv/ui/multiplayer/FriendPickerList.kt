package com.unciv.ui.multiplayer

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.unciv.logic.multiplayer.FriendList
import com.unciv.ui.newgamescreen.PlayerPickerTable
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick

class FriendPickerList(
    playerPicker: PlayerPickerTable,
    onSelected: (String) -> Unit
) : VerticalGroup() {

    private val friendDisplays = mutableMapOf<String, FriendDisplay>()
    private var friendList: MutableList<FriendList.Friend>

    init {
        padTop(10f)
        padBottom(10f)

        friendList = playerPicker.getAvailableFriends().toMutableList()

        for (friend in friendList) {
            addFriend(friend, onSelected)
        }
    }

    private fun addFriend(friend: FriendList.Friend, onSelected: (String) -> Unit) {
        val friendDisplay = FriendDisplay(friend, onSelected)
        friendDisplays[friend.name] = friendDisplay
        addActor(friendDisplay)
    }

    /** Opt-in wrapping for constrained picker popups. */
    fun constrainItemWidth(width: Float) {
        val constrained = width.coerceAtLeast(1f)
        for (display in friendDisplays.values) display.constrainWidth(constrained)
        invalidateHierarchy()
    }
}

private class FriendDisplay(
    friend: FriendList.Friend,
    private val onSelected: (String) -> Unit
) : Table() {
    var friendName: String = friend.name
        private set
    val friendButton = TextButton(friendName, BaseScreen.skin)

    init {
        padBottom(5f)

        add(friendButton)
        onClick { onSelected(friendName) }
    }

    fun constrainWidth(width: Float) {
        friendButton.label.wrap = true
        friendButton.labelCell.width(
            (width - friendButton.padLeft - friendButton.padRight - 20f).coerceAtLeast(1f)
        ).pad(10f)
        getCell(friendButton).width(width).minHeight(48f).fillX()
        invalidateHierarchy()
    }
}
