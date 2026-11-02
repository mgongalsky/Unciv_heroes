package com.unciv.ui.worldscreen.status

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.unciv.UncivGame
import com.unciv.logic.multiplayer.OnlineMultiplayerGame
import com.unciv.models.translations.tr
import com.unciv.ui.multiplayer.GameList
import com.unciv.ui.multiplayer.MultiplayerHelpers
import com.unciv.ui.pickerscreens.PickerPane
import com.unciv.ui.popup.Popup
import com.unciv.ui.popup.applyRoyalTextLayout
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick

class MultiplayerStatusPopup(
    screen: BaseScreen,
) : Popup(screen) {

    val pickerPane = PickerPane()
    var selectedGame: OnlineMultiplayerGame? = null

    init {
        val pickerCell = add()
            .width(700f).fillX().expandX()
            .minHeight(screen.stage.height * 0.5f)
            .maxHeight(screen.stage.height * 0.8f)

        val gameList = GameList(::gameSelected)
        pickerPane.topTable.add(gameList)
        pickerPane.rightSideButton.setText("Load game".tr())
        pickerPane.closeButton.onClick(::close)
        pickerCell.setActor<PickerPane>(pickerPane)
        pickerPane.rightSideButton.onClick {
            close()
            val game = selectedGame
            if (game != null) {
                MultiplayerHelpers.loadMultiplayerGame(screen, game)
            }
        }

        if (UncivGame.Current.settings.skin == "Minimal") {
            applyRoyalTextLayout()
            val scroll = children.filterIsInstance<ScrollPane>().single()
            val style = scroll.style
            val chrome = innerTable.padLeft + innerTable.padRight + 10f +
                maxOf(style.vScroll?.minWidth ?: 0f, style.vScrollKnob?.minWidth ?: 0f) +
                (style.background?.leftWidth ?: 0f) + (style.background?.rightWidth ?: 0f)
            val contentWidth = minOf(700f, screen.stage.width - 24f - chrome).coerceAtLeast(1f)
            val contentHeight = minOf(
                screen.stage.height * 0.8f,
                screen.stage.height - 24f - innerTable.padTop - innerTable.padBottom - 10f
            ).coerceAtLeast(240f)
            pickerCell.width(contentWidth).height(contentHeight)
            pickerPane.enableAdaptiveActions()
            pickerPane.descriptionLabel.color = Color.valueOf("453925")
            pickerPane.scrollPane.setScrollingDisabled(true, false)
            gameList.constrainItemWidth((contentWidth - 20f).coerceAtLeast(1f))
            pickerPane.topTable.getCell(gameList).width(contentWidth).growX()
        }
    }

    private fun gameSelected(gameName: String) {
        val multiplayerGame = UncivGame.Current.onlineMultiplayer.getGameByName(gameName)!!
        selectedGame = multiplayerGame
        pickerPane.setRightSideButtonEnabled(true)
        pickerPane.rightSideButton.setText("Load [$gameName]".tr())
        pickerPane.descriptionLabel.setText(MultiplayerHelpers.buildDescriptionText(multiplayerGame))
    }
}
