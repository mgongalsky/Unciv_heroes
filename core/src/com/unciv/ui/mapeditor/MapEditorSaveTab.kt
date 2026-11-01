package com.unciv.ui.mapeditor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.MapSaver
import com.unciv.logic.map.MapType
import com.unciv.logic.map.TileMap
import com.unciv.models.translations.tr
import com.unciv.ui.popup.ConfirmPopup
import com.unciv.ui.popup.Popup
import com.unciv.ui.popup.ToastPopup
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.UncivTextField
import com.unciv.ui.utils.extensions.isEnabled
import com.unciv.ui.utils.extensions.keyShortcuts
import com.unciv.ui.utils.extensions.onActivation
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toTextButton
import kotlin.concurrent.thread

class MapEditorSaveTab(
    private val editorScreen: MapEditorScreen,
    headerHeight: Float
): Table(BaseScreen.skin), TabbedPager.IPageExtensions {
    private val mapFiles = MapEditorFilesTable(
        initWidth = editorScreen.getToolsWidth() - 40f,
        includeMods = false,
        this::selectFile)

    private val saveButton = "Save map".toTextButton()
    private val deleteButton = "Delete map".toTextButton()
    private val quitButton = "Exit map editor".toTextButton()

    private val mapNameTextField = UncivTextField.create("Map Name")

    private var chosenMap: FileHandle? = null

    init {
        mapNameTextField.maxLength = 100
        mapNameTextField.textFieldFilter = TextField.TextFieldFilter { _, char -> char != '\\' && char != '/' }
        mapNameTextField.selectAll()
        // do NOT take the keyboard focus here! We're not even visible.
        add(mapNameTextField).pad(10f).fillX().row()

        val buttonTable = Table(skin)
        buttonTable.defaults().pad(10f).fillX()
        saveButton.onActivation { saveHandler() }
        saveButton.keyShortcuts.add(KeyCharAndCode.RETURN)
        mapNameTextField.onChange {
            saveButton.isEnabled = mapNameTextField.text.isNotBlank()
        }
        buttonTable.add(saveButton)

        deleteButton.onActivation { deleteHandler() }
        deleteButton.keyShortcuts.add(KeyCharAndCode.DEL)
        buttonTable.add(deleteButton)

        quitButton.onClick(editorScreen::closeEditor)
        buttonTable.add(quitButton)
        buttonTable.pack()

        val fileTableHeight = editorScreen.stage.height - headerHeight - mapNameTextField.prefHeight - buttonTable.height - 22f
        val scrollPane = AutoScrollPane(mapFiles, skin)
        scrollPane.setOverscroll(false, true)
        add(scrollPane).height(fileTableHeight).fillX().row()
        add(buttonTable).row()
    }

    /** Снимает копию и версию на UI-потоке; файловая запись не читает изменяемую форму редактора. */
    private fun saveHandler(playAfterSave: Boolean = false) {
        val mapName = mapNameTextField.text
        if (mapName.isBlank() || saving) return
        try {
            editorScreen.tileMap.mapParameters.name = mapName
            editorScreen.tileMap.mapParameters.type = MapType.custom
            val sourceMap = editorScreen.tileMap
            val snapshot = editorScreen.getMapCloneForSave()
            val revision = editorScreen.editRevision
            saving = true
            thread(name = "MapSaver") {
                saverThread(
                    mapName,
                    snapshot,
                    sourceMap,
                    revision,
                    playAfterSave
                )
            }
        } catch (failure: Exception) {
            saving = false
            Popup(editorScreen).apply {
                addGoodSizedLabel(failure.message ?: "Could not prepare map for saving").row()
                addCloseButton()
            }.open()
        }
    }

    private fun deleteHandler() {
        if (chosenMap == null) return
        ConfirmPopup(
            editorScreen,
            "Are you sure you want to delete this map?",
            "Delete map",
        ) {
            chosenMap!!.delete()
            mapFiles.update()
        }.open()
    }

    override fun activated(index: Int, caption: String, pager: TabbedPager) {
        pager.setScrollDisabled(true)
        if (findActor<com.badlogic.gdx.scenes.scene2d.Actor>("scenario-save-and-play") == null) {
            val fileScroll = children.toList().filterIsInstance<AutoScrollPane>().single()
            val cell = getCell(fileScroll)
            cell.height((cell.prefHeight - 60f).coerceAtLeast(80f))
            add("Save and play".toTextButton().apply {
                name = "scenario-save-and-play"
            }.onClick { saveHandler(playAfterSave = true) }).minHeight(48f).growX().pad(6f).row()
        }
        mapFiles.update()
        selectFile(null)
    }

    override fun deactivated(index: Int, caption: String, pager: TabbedPager) {
        pager.setScrollDisabled(false)
        stage.keyboardFocus = null
    }

    fun selectFile(file: FileHandle?) {
        chosenMap = file
        mapNameTextField.text = file?.name() ?: editorScreen.tileMap.mapParameters.name
        if (mapNameTextField.text.isBlank()) mapNameTextField.text = "My new map".tr()
        mapNameTextField.setSelection(Int.MAX_VALUE, Int.MAX_VALUE)  // sets caret to end of text
        stage.keyboardFocus = mapNameTextField
        saveButton.isEnabled = true
        deleteButton.isEnabled = (file != null)
        deleteButton.color = if (file != null) Color.SCARLET else Color.BROWN
    }

    /** Настройки запуска открываются только после успешной записи проверенного снимка. */
    private fun saverThread(
        mapName: String,
        snapshot: TileMap,
        sourceMap: TileMap,
        revision: Long,
        playAfterSave: Boolean
    ) {
        try {
            MapSaver.saveMap(mapName, snapshot)
            Gdx.app.postRunnable {
                saving = false
                // Новая карта или более свежие правки не относятся к завершившейся записи.
                if (editorScreen.tileMap === sourceMap && editorScreen.editRevision == revision)
                    editorScreen.isDirty = false
                if (editorScreen.game.screen !== editorScreen) return@postRunnable
                if (playAfterSave) {
                    val parameters = com.unciv.models.metadata.GameParameters().apply {
                        baseRuleset = snapshot.mapParameters.baseRuleset
                        mods = LinkedHashSet(snapshot.mapParameters.mods)
                        if (snapshot.scenarioPlayerSlots.isNotEmpty()) {
                            numberOfCityStates = 0
                            players =
                                    ArrayList(snapshot.scenarioPlayerSlots.mapIndexed { index, slot ->
                                        com.unciv.models.metadata.Player().apply {
                                            scenarioSlotId = slot.id
                                            playerType =
                                                    if (index == 0) com.unciv.logic.civilization.PlayerType.Human
                                                    else com.unciv.logic.civilization.PlayerType.AI
                                        }
                                    })
                        }
                    }
                    val setup = com.unciv.models.metadata.GameSetupInfo(
                        parameters,
                        snapshot.mapParameters.clone()
                    ).apply {
                        mapFile = Gdx.files.local("${MapSaver.mapsFolder}/$mapName")
                    }
                    editorScreen.game.pushScreen(com.unciv.ui.newgamescreen.NewGameScreen(setup))
                } else ToastPopup("Map saved successfully!", editorScreen)
            }
        } catch (failure: Exception) {
            failure.printStackTrace()
            Gdx.app.postRunnable {
                saving = false
                Popup(editorScreen).apply {
                    addGoodSizedLabel("It looks like your map can't be saved!").row()
                    addGoodSizedLabel(failure.message ?: failure.javaClass.simpleName).row()
                    addCloseButton()
                }.open(force = true)
            }
        }
    }

    /** Не допускает конкурирующих записей и повторного открытия экрана запуска. */
    private var saving = false
}
