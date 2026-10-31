package com.unciv.ui.newgamescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Array
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.logic.GameStarter
import com.unciv.logic.IdChecker
import com.unciv.logic.MapSaver
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.MapType
import com.unciv.logic.multiplayer.OnlineMultiplayer
import com.unciv.logic.multiplayer.storage.FileStorageRateLimitReached
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.translations.tr
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.pickerscreens.PickerScreen
import com.unciv.ui.popup.ConfirmPopup
import com.unciv.ui.popup.Popup
import com.unciv.ui.popup.ToastPopup
import com.unciv.ui.saves.LoadGameScreen
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ExpanderTab
import com.unciv.ui.utils.RecreateOnResize
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.addSeparatorVertical
import com.unciv.ui.utils.extensions.disable
import com.unciv.ui.utils.extensions.enable
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.pad
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.unciv.ui.worldscreen.WorldScreen
import com.unciv.utils.Log
import com.unciv.utils.concurrency.Concurrency
import com.unciv.utils.concurrency.launchOnGLThread
import kotlinx.coroutines.coroutineScope
import java.net.URL
import java.util.*
import com.unciv.ui.utils.AutoScrollPane as ScrollPane

class NewGameScreen(
    _gameSetupInfo: GameSetupInfo? = null
): IPreviousScreen, PickerScreen(), RecreateOnResize {

    override val gameSetupInfo = _gameSetupInfo ?: GameSetupInfo.fromSettings()
    override var ruleset = RulesetCache.getComplexRuleset(gameSetupInfo.gameParameters) // needs to be set because the GameOptionsTable etc. depend on this
    private val newGameOptionsTable: GameOptionsTable
    private val playerPickerTable: PlayerPickerTable
    private val mapOptionsTable: MapOptionsTable

    init {
        updateRuleset()
        playerPickerTable = PlayerPickerTable(
            this, gameSetupInfo.gameParameters,
            if (isNarrowerThan4to3()) stage.width - 20f else 0f
        )
        newGameOptionsTable = GameOptionsTable(
            this,
            isNarrowerThan4to3()
        ) { desiredCiv -> playerPickerTable.update(desiredCiv) }
        mapOptionsTable = MapOptionsTable(this)
        updateTables()
        setDefaultCloseAction()
        if (isNarrowerThan4to3()) initPortrait() else initLandscape()
        pickerPane.bottomTable.background = skinStrings.getUiBackground(
            "NewGameScreen/BottomTable",
            tintColor = skinStrings.skinConfig.clearColor
        )
        pickerPane.topTable.background = skinStrings.getUiBackground(
            "NewGameScreen/TopTable",
            tintColor = skinStrings.skinConfig.clearColor
        )
        if (UncivGame.Current.settings.lastGameSetup != null) {
            rightSideGroup.addActorAt(0, VerticalGroup().padBottom(5f))
            val reset = "Reset to defaults".toTextButton()
            rightSideGroup.addActorAt(0, reset)
            reset.onClick {
                ConfirmPopup(
                    this,
                    "Are you sure you want to reset all game options to defaults?",
                    "Reset to defaults"
                ) {
                    game.replaceCurrentScreen(NewGameScreen(GameSetupInfo()))
                }.open(true)
            }
        }
        rightSideButton.enable()
        rightSideButton.setText("Start game!".tr())
        rightSideButton.onClick {
            // Повторно применяем ограничения непосредственно перед стартом, включая смену модов.
            updateTables()
            if (newGameOptionsTable.scenarioVictoryError.isNotEmpty()) {
                Popup(this).apply {
                    addGoodSizedLabel(newGameOptionsTable.scenarioVictoryError).row()
                    addCloseButton()
                    open()
                }
                return@onClick
            }
            if (gameSetupInfo.gameParameters.isOnlineMultiplayer) {
                if (!checkConnectionToMultiplayerServer()) {
                    Popup(this).apply {
                        addGoodSizedLabel((if (OnlineMultiplayer.usesCustomServer()) "Couldn't connect to Multiplayer Server!" else "Couldn't connect to Dropbox!").tr()).row()
                        addCloseButton()
                        open()
                    }
                    return@onClick
                }
                for (player in gameSetupInfo.gameParameters.players.filter { it.playerType == PlayerType.Human }) {
                    try {
                        UUID.fromString(IdChecker.checkAndReturnPlayerUuid(player.playerId))
                    } catch (_: Exception) {
                        Popup(this).apply { addGoodSizedLabel("Invalid player ID!".tr()).row(); addCloseButton(); open() }
                        return@onClick
                    }
                }
                if (!gameSetupInfo.gameParameters.anyoneCanSpectate && gameSetupInfo.gameParameters.players.none {
                            it.playerId == UncivGame.Current.settings.multiplayer.userId
                        }) {
                    Popup(this).apply { addGoodSizedLabel("You are not allowed to spectate!".tr()).row(); addCloseButton(); open() }
                    return@onClick
                }
            }
            if (gameSetupInfo.gameParameters.players.none {
                        it.playerType == PlayerType.Human && !(it.chosenCiv == Constants.spectator &&
                                gameSetupInfo.gameParameters.isOnlineMultiplayer && it.playerId != UncivGame.Current.settings.multiplayer.userId)
                    }) {
                Popup(this).apply { addGoodSizedLabel("No human players selected!".tr()).row(); addCloseButton(); open() }
                return@onClick
            }
            if (gameSetupInfo.gameParameters.victoryTypes.isEmpty()) {
                Popup(this).apply { addGoodSizedLabel("No victory conditions were selected!".tr()).row(); addCloseButton(); open() }
                return@onClick
            }
            Gdx.input.inputProcessor = null
            if (mapOptionsTable.mapTypeSelectBox.selected.value == MapType.custom) {
                val map = try {
                    MapSaver.loadMap(gameSetupInfo.mapFile!!)
                } catch (_: Throwable) {
                    Gdx.input.inputProcessor = stage
                    ToastPopup("Could not load map!", this)
                    return@onClick
                }
                try {
                    com.unciv.pure.application.scenario.ScenarioPlayerBindings.resolve(
                        map.scenarioPlayerSlots,
                        gameSetupInfo.gameParameters.players.map { it.scenarioSlotId to it })
                    require(gameSetupInfo.gameParameters.players.none {
                        it.scenarioSlotId.isNotEmpty() && it.chosenCiv == Constants.spectator
                    }) { "A spectator cannot occupy a scenario player slot" }
                } catch (invalid: IllegalArgumentException) {
                    Popup(this).apply { addGoodSizedLabel(invalid.message.orEmpty()).row(); addCloseButton(); open() }
                    Gdx.input.inputProcessor = stage
                    return@onClick
                }
                val incompatibilities = map.getRulesetIncompatibility(ruleset)
                if (incompatibilities.isNotEmpty()) {
                    Popup(this).apply {
                        addGoodSizedLabel("Map is incompatible with the chosen ruleset!".tr()).row()
                        for (incompatibility in incompatibilities) addGoodSizedLabel(incompatibility).row()
                        addCloseButton()
                        open()
                    }
                    Gdx.input.inputProcessor = stage
                    return@onClick
                }
            } else {
                val mapSize = gameSetupInfo.mapParameters.mapSize
                val message = mapSize.fixUndesiredSizes(gameSetupInfo.mapParameters.worldWrap)
                if (message != null) {
                    ToastPopup(message, UncivGame.Current.screen!!, 4000)
                    with(mapOptionsTable.generatedMapOptionsTable) {
                        customMapSizeRadius.text = mapSize.radius.toString()
                        customMapWidth.text = mapSize.width.toString()
                        customMapHeight.text = mapSize.height.toString()
                    }
                    Gdx.input.inputProcessor = stage
                    return@onClick
                }
            }
            rightSideButton.disable()
            rightSideButton.setText("Working...".tr())
            setSkin()
            Concurrency.runOnNonDaemonThreadPool("NewGame") { startNewGame() }
        }
    }

    private fun initLandscape() {
        scrollPane.setScrollingDisabled(true,true)

        topTable.add("Game Options".toLabel(fontSize = Constants.headingFontSize)).pad(20f, 0f)
        topTable.addSeparatorVertical(Color.BLACK, 1f)
        topTable.add("Map Options".toLabel(fontSize = Constants.headingFontSize)).pad(20f,0f)
        topTable.addSeparatorVertical(Color.BLACK, 1f)
        topTable.add("Civilizations".toLabel(fontSize = Constants.headingFontSize)).pad(20f,0f)
        topTable.addSeparator(Color.CLEAR, height = 1f)

        topTable.add(ScrollPane(newGameOptionsTable)
                .apply { setOverscroll(false, false) })
                .width(stage.width / 3).top()
        topTable.addSeparatorVertical(Color.CLEAR, 1f)
        topTable.add(ScrollPane(mapOptionsTable)
                .apply { setOverscroll(false, false) })
                .width(stage.width / 3).top()
        topTable.addSeparatorVertical(Color.CLEAR, 1f)
        topTable.add(playerPickerTable)  // No ScrollPane, PlayerPickerTable has its own
                .width(stage.width / 3).top()
    }

    private fun initPortrait() {
        scrollPane.setScrollingDisabled(false,false)

        topTable.add(ExpanderTab("Game Options") {
            it.add(newGameOptionsTable).row()
        }).expandX().fillX().row()
        topTable.addSeparator(Color.DARK_GRAY, height = 1f)

        topTable.add(newGameOptionsTable.modCheckboxes).expandX().fillX().row()
        topTable.addSeparator(Color.DARK_GRAY, height = 1f)

        topTable.add(ExpanderTab("Map Options") {
            it.add(mapOptionsTable).row()
        }).expandX().fillX().row()
        topTable.addSeparator(Color.DARK_GRAY, height = 1f)

        (playerPickerTable.playerListTable.parent as ScrollPane).setScrollingDisabled(true,true)
        topTable.add(ExpanderTab("Civilizations") {
            it.add(playerPickerTable).row()
        }).expandX().fillX().row()
    }

    private fun checkConnectionToMultiplayerServer(): Boolean {
        return try {
            val multiplayerServer = UncivGame.Current.settings.multiplayer.server
            val u =  URL(if (OnlineMultiplayer.usesDropbox()) "https://content.dropboxapi.com" else multiplayerServer)
            val con = u.openConnection()
            con.connectTimeout = 3000
            con.connect()

            true
        } catch(ex: Throwable) {
            false
        }
    }

    private suspend fun startNewGame() = coroutineScope {
        val popup = Popup(this@NewGameScreen)
        launchOnGLThread {
            popup.addGoodSizedLabel("Working...").row()
            popup.open()
        }

        val newGame:GameInfo
        try {
            newGame = GameStarter.startNewGame(gameSetupInfo)
        } catch (exception: Exception) {
            exception.printStackTrace()
            launchOnGLThread {
                popup.apply {
                    reuseWith("It looks like we can't make a map with the parameters you requested!")
                    row()
                    addGoodSizedLabel(exception.message ?: exception.javaClass.simpleName).row()
                    addCloseButton()
                }
                Gdx.input.inputProcessor = stage
                rightSideButton.enable()
                rightSideButton.setText("Start game!".tr())
            }
            return@coroutineScope
        }

        if (gameSetupInfo.gameParameters.isOnlineMultiplayer) {
            newGame.isUpToDate = true // So we don't try to download it from dropbox the second after we upload it - the file is not yet ready for loading!
            try {
                game.onlineMultiplayer.createGame(newGame)
                game.files.requestAutoSave(newGame)
            } catch (ex: FileStorageRateLimitReached) {
                launchOnGLThread {
                    popup.reuseWith("Server limit reached! Please wait for [${ex.limitRemainingSeconds}] seconds", true)
                    rightSideButton.enable()
                    rightSideButton.setText("Start game!".tr())
                }
                Gdx.input.inputProcessor = stage
                return@coroutineScope
            } catch (ex: Exception) {
                Log.error("Error while creating game", ex)
                launchOnGLThread {
                    popup.reuseWith("Could not upload game!", true)
                    rightSideButton.enable()
                    rightSideButton.setText("Start game!".tr())
                }
                Gdx.input.inputProcessor = stage
                return@coroutineScope
            }
        }

        val worldScreen = game.loadGame(newGame)

        if (newGame.gameParameters.isOnlineMultiplayer) {
            launchOnGLThread {
                    // Save gameId to clipboard because you have to do it anyway.
                    Gdx.app.clipboard.contents = newGame.gameId
                    // Popup to notify the User that the gameID got copied to the clipboard
                    ToastPopup("Game ID copied to clipboard!".tr(), worldScreen, 2500)
            }
        }
    }

    fun updateRuleset() {
        ruleset.clear()
        ruleset.add(RulesetCache.getComplexRuleset(gameSetupInfo.gameParameters))
        ImageGetter.setNewRuleset(ruleset)
        game.musicController.setModList(gameSetupInfo.gameParameters.getModsAndBaseRuleset())
    }

    fun lockTables() {
        playerPickerTable.locked = true
        newGameOptionsTable.locked = true
    }

    fun unlockTables() {
        playerPickerTable.locked = false
        newGameOptionsTable.locked = false
    }

    /** Обновляет назначения и эффективные победы; defaults не заменяют явный выбор пользователя. */
    fun updateTables() {
        val mapFile =
                if (gameSetupInfo.mapParameters.name.isNotEmpty()) gameSetupInfo.mapFile else null
        val slots = if (mapFile != null) {
            try {
                MapSaver.loadScenarioPlayerSlots(mapFile)
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()
        playerPickerTable.scenarioSlots = slots
        for (player in gameSetupInfo.gameParameters.players)
            if (slots.none { it.id == player.scenarioSlotId }) player.scenarioSlotId = ""
        playerPickerTable.gameParameters = gameSetupInfo.gameParameters
        playerPickerTable.update()
        val parameters = gameSetupInfo.gameParameters
        newGameOptionsTable.gameParameters = parameters
        newGameOptionsTable.scenarioVictoryError = ""
        newGameOptionsTable.scenarioVictorySettings = null
        try {
            val settings = mapFile?.let { MapSaver.loadScenarioVictorySettings(it) }
            newGameOptionsTable.scenarioVictorySettings = settings
            val available = ruleset.victories.keys.filter {
                it != com.unciv.pure.domain.victory.CaptureCityObjective.VICTORY_TYPE || !settings?.captureObjectives.isNullOrEmpty()
            }
            val selected =
                    if (parameters.victorySelectionInitialized || parameters.victoryTypes.isNotEmpty())
                        parameters.victoryTypes else null
            val resolved = com.unciv.pure.application.scenario.ResolveVictoryOptionsUseCase.execute(
                available,
                settings,
                selected
            )
            parameters.victoryTypes = ArrayList(resolved)
            parameters.victorySelectionInitialized = true
        } catch (invalid: Exception) {
            newGameOptionsTable.scenarioVictoryError = invalid.message.orEmpty()
        }
        newGameOptionsTable.update()
    }

    override fun recreate(): BaseScreen = NewGameScreen(gameSetupInfo)
}

class TranslatedSelectBox(values : Collection<String>, default:String, skin: Skin) : SelectBox<TranslatedSelectBox.TranslatedString>(skin) {
    class TranslatedString(val value: String) {
        val translation = value.tr()
        override fun toString() = translation
        // Equality contract needs to be implemented else TranslatedSelectBox.setSelected won't work properly
        override fun equals(other: Any?): Boolean = other is TranslatedString && value == other.value
        override fun hashCode() = value.hashCode()
    }

    init {
        val array = Array<TranslatedString>()
        values.forEach { array.add(TranslatedString(it)) }
        items = array
        selected = array.firstOrNull { it.value == default } ?: array.first()
    }

    fun setSelected(newValue: String) {
        selected = items.firstOrNull { it == TranslatedString(newValue) } ?: return
    }
}
