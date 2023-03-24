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
        defaults().fillX()

        // TODO: remove code dubbing, create function for a button
        addButton("Main menu", null, BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)) {
            worldScreen.game.goToMainMenu()
        }.apply {
            actor.label.setFontScale(0.5f, 0.5f)
            actor.label.setAlignment(Align.center)
            actor.pad(0f,10f,0f, 10f)

        }.padTop(25f).row()
        addButton("Civilopedia", null, BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)) {
            close()
            worldScreen.game.pushScreen(CivilopediaScreen(worldScreen.gameInfo.ruleSet))
        }.apply {
            actor.label.setFontScale(0.5f, 0.5f)
            actor.label.setAlignment(Align.center)
            actor.pad(0f,10f,0f, 10f)

        }.row()
        addButton("Save game", null, BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)) {
            close()
            worldScreen.game.pushScreen(SaveGameScreen(worldScreen.gameInfo))
        }.apply {
            actor.label.setFontScale(0.5f, 0.5f)
            actor.label.setAlignment(Align.center)
            actor.pad(0f,10f,0f, 10f)

        }.row()
        addButton("Load game", null, BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)) {
            close()
            worldScreen.game.pushScreen(LoadGameScreen(worldScreen))
        }.apply {
            actor.label.setFontScale(0.5f, 0.5f)
            actor.label.setAlignment(Align.center)
            actor.pad(0f,10f,0f, 10f)

        }.row()

        addButton("New game", null, BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)) {
            close()
            val newGameSetupInfo = GameSetupInfo(worldScreen.gameInfo)
            newGameSetupInfo.mapParameters.reseed()
            val newGameScreen = NewGameScreen(newGameSetupInfo)
            worldScreen.game.pushScreen(newGameScreen)
        }.apply {
            actor.label.setFontScale(0.5f, 0.5f)
            actor.label.setAlignment(Align.center)

            actor.pad(0f,10f,0f, 10f)
        }.row()

        addButton("Victory status", null, BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)) {
            close()
            worldScreen.game.pushScreen(VictoryScreen(worldScreen))
        }.apply {
            actor.label.setFontScale(0.5f, 0.5f)
            actor.label.setAlignment(Align.center)
            actor.pad(0f,10f,0f, 10f)

        }.row()
        addButton("Options", null, BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)) {
            close()
            worldScreen.openOptionsPopup()
        }.apply {
            actor.label.setFontScale(0.5f, 0.5f)
            actor.label.setAlignment(Align.center)

            actor.pad(0f,10f,0f, 10f)
        }.row()
        addButton("Community", null, BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)) {
            close()
            WorldScreenCommunityPopup(worldScreen).open(force = true)
        }.apply {
            actor.label.setFontScale(0.5f, 0.5f)
            actor.label.setAlignment(Align.center)
            actor.pad(0f,10f,0f, 10f)

        }.row()
        addCloseButton("Close", null, BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)).apply {
            actor.label.setFontScale(0.5f, 0.5f)
            actor.label.setAlignment(Align.center)
            actor.pad(0f,10f,0f, 10f)
        }
        pack()
    }
}

class WorldScreenCommunityPopup(val worldScreen: WorldScreen) : Popup(worldScreen) {
    init {
        defaults().fillX()
        addButton("Discord") {
            Gdx.net.openURI("https://discord.gg/bjrB4Xw")
            close()
        }.row()

        addButton("Github") {
            Gdx.net.openURI("https://github.com/yairm210/Unciv")
            close()
        }.row()

        addButton("Reddit") {
            Gdx.net.openURI("https://www.reddit.com/r/Unciv/")
            close()
        }.row()

        addCloseButton()
    }
}
