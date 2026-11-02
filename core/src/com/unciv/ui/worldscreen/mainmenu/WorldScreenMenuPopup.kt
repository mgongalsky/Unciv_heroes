package com.unciv.ui.worldscreen.mainmenu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.ui.civilopedia.CivilopediaScreen
import com.unciv.ui.newgamescreen.NewGameScreen
import com.unciv.ui.popup.Popup
import com.unciv.ui.saves.LoadGameScreen
import com.unciv.ui.saves.SaveGameScreen
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.victoryscreen.VictoryScreen
import com.unciv.ui.worldscreen.WorldScreen

class WorldScreenMenuPopup(val worldScreen: WorldScreen) : Popup(worldScreen) {
    init {
        val royal = worldScreen.game.settings.skin == "Minimal"
        val buttonStyle = BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)
        defaults().fillX()
        var buttonWidth = 0f
        if (royal) {
            innerTable.background = com.unciv.ui.utils.RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
            val scroll = children.filterIsInstance<com.unciv.ui.utils.AutoScrollPane>().single()
            scroll.setScrollingDisabled(true, false)
            scroll.setOverscroll(false, false)
            val scrollStyle = scroll.style
            val barWidth = maxOf(
                scrollStyle.vScroll?.minWidth ?: 0f,
                scrollStyle.vScrollKnob?.minWidth ?: 0f
            )
            val insets = (scrollStyle.background?.leftWidth ?: 0f) +
                    (scrollStyle.background?.rightWidth ?: 0f)
            // Полоса резервируется до первой раскладки, когда scrollBarWidth ещё равен нулю.
            val chrome = innerTable.padLeft + innerTable.padRight + 10f + barWidth + insets
            buttonWidth = minOf(360f, stageToShowOn.width - 24f - chrome).coerceAtLeast(1f)
            getCell(scroll).width(buttonWidth + chrome).minHeight(0f)
                .maxHeight((stageToShowOn.height - 40f).coerceAtLeast(1f))
        }

        fun configure(cell: com.badlogic.gdx.scenes.scene2d.ui.Cell<TextButton>) {
            cell.actor.label.setAlignment(Align.center)
            if (royal) {
                cell.actor.label.setWrap(true)
                cell.actor.pad(10f, 25f, 10f, 25f)
                cell.width(buttonWidth).minHeight(58f)
            } else {
                cell.actor.label.setFontScale(0.5f, 0.5f)
                cell.actor.pad(0f, 10f, 0f, 10f)
            }
        }

        val mainMenu = addButton("Main menu", null, buttonStyle) {
            worldScreen.game.goToMainMenu()
        }
        configure(mainMenu)
        if (!royal) mainMenu.padTop(25f)
        mainMenu.row()

        val civilopedia = addButton("Civilopedia", null, buttonStyle) {
            close()
            worldScreen.game.pushScreen(CivilopediaScreen(worldScreen.gameInfo.ruleSet))
        }
        configure(civilopedia)
        civilopedia.row()

        val save = addButton("Save game", null, buttonStyle) {
            close()
            worldScreen.game.pushScreen(SaveGameScreen(worldScreen.gameInfo))
        }
        configure(save)
        save.row()

        val load = addButton("Load game", null, buttonStyle) {
            close()
            worldScreen.game.pushScreen(LoadGameScreen(worldScreen))
        }
        configure(load)
        load.row()

        val newGame = addButton("New game", null, buttonStyle) {
            close()
            val newGameSetupInfo = GameSetupInfo(worldScreen.gameInfo)
            newGameSetupInfo.mapParameters.reseed()
            worldScreen.game.pushScreen(NewGameScreen(newGameSetupInfo))
        }
        configure(newGame)
        newGame.row()

        val victory = addButton("Victory status", null, buttonStyle) {
            close()
            worldScreen.game.pushScreen(VictoryScreen(worldScreen))
        }
        configure(victory)
        victory.row()

        val options = addButton("Options", null, buttonStyle) {
            close()
            worldScreen.openOptionsPopup()
        }
        configure(options)
        options.row()

        val community = addButton("Community", null, buttonStyle) {
            close()
            WorldScreenCommunityPopup(worldScreen).open(force = true)
        }
        configure(community)
        community.row()
        configure(addCloseButton("Close", null, buttonStyle))
        pack()
    }
}

class WorldScreenCommunityPopup(val worldScreen: WorldScreen) : Popup(worldScreen) {
    init {
        val royal = worldScreen.game.settings.skin == "Minimal"
        defaults().fillX()
        var buttonWidth = 0f
        if (royal) {
            innerTable.background = com.unciv.ui.utils.RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
            val scroll = children.filterIsInstance<com.unciv.ui.utils.AutoScrollPane>().single()
            scroll.setScrollingDisabled(true, false)
            scroll.setOverscroll(false, false)
            val style = scroll.style
            val barWidth = maxOf(style.vScroll?.minWidth ?: 0f, style.vScrollKnob?.minWidth ?: 0f)
            val insets = (style.background?.leftWidth ?: 0f) + (style.background?.rightWidth ?: 0f)
            val chrome = innerTable.padLeft + innerTable.padRight + 10f + barWidth + insets
            buttonWidth = minOf(360f, stageToShowOn.width - 24f - chrome).coerceAtLeast(1f)
            getCell(scroll).width(buttonWidth + chrome).minHeight(0f)
                .maxHeight((stageToShowOn.height - 40f).coerceAtLeast(1f))
        }
        fun configure(cell: com.badlogic.gdx.scenes.scene2d.ui.Cell<TextButton>) {
            if (!royal) return
            cell.actor.label.setAlignment(Align.center)
            cell.actor.label.setWrap(true)
            cell.actor.pad(10f, 25f, 10f, 25f)
            cell.width(buttonWidth).minHeight(58f)
        }

        val discord = addButton("Discord") {
            Gdx.net.openURI("https://discord.gg/bjrB4Xw")
            close()
        }
        configure(discord)
        discord.row()

        val github = addButton("Github") {
            Gdx.net.openURI("https://github.com/yairm210/Unciv")
            close()
        }
        configure(github)
        github.row()

        val reddit = addButton("Reddit") {
            Gdx.net.openURI("https://www.reddit.com/r/Unciv/")
            close()
        }
        configure(reddit)
        reddit.row()
        configure(addCloseButton())
        pack()
    }
}
