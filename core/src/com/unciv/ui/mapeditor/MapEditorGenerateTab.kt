package com.unciv.ui.mapeditor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapType
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.mapgenerator.MapGenerator
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.translations.tr
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.newgamescreen.MapParametersTable
import com.unciv.ui.popup.Popup
import com.unciv.ui.popup.ToastPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.extensions.disable
import com.unciv.ui.utils.extensions.enable
import com.unciv.ui.utils.extensions.isEnabled
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toCheckBox
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.unciv.utils.Log
import kotlin.concurrent.thread

class MapEditorGenerateTab(
    private val editorScreen: MapEditorScreen
): TabbedPager(capacity = 2) {
    /** Полный сброс формы доступен только после явного подтверждения. */
    val resetDefaultsButton = "Reset to defaults".toTextButton().apply {
        name = "map-generation-reset-defaults"
        onClick { requestDefaultParameters() }
    }

    /** Повторный запуск запрещён до публикации результата на UI-потоке. */
    var isGenerating = false
        private set

    /** Общая кнопка полной генерации располагается в закреплённой панели. */
    val regenerateButton = "Generate".toTextButton().apply {
        name = "map-generate-new-seed"
        onClick { generate(MapGeneratorSteps.All, randomizeSeed = true) }
    }

    private var newTab = MapEditorNewMapTab(this)
    private val partialTab = MapEditorGenerateStepsTab(this)

    // Since we allow generation components to be run repeatedly, it might surprise the user that
    // the outcome stays the same when repeated - due to them operating on the same seed.
    // So we change the seed behind the scenes if already used for a certain step...
    private val seedUsedForStep = mutableSetOf<MapGeneratorSteps>()

    init {
        name = "Generate"
        top()
        addPage("New map", newTab,
            ImageGetter.getImage("OtherIcons/New"), 20f,
            shortcutKey = KeyCharAndCode.ctrl('n'))
        addPage("Partial", partialTab,
            ImageGetter.getImage("OtherIcons/Settings"), 20f,
            shortcutKey = KeyCharAndCode.ctrl('g'))
        selectPage(0)
        setButtonsEnabled(true)
        partialTab.generateButton.disable()  // Starts with choice "None"
    }

    private fun setButtonsEnabled(enable: Boolean) {
        isGenerating = !enable
        regenerateButton.isEnabled = enable
        resetDefaultsButton.isEnabled = enable
        regenerateButton.setText((if (enable) "Generate" else "Working...").tr())
        partialTab.generateButton.isEnabled = enable
        partialTab.generateButton.setText((if (enable) "Generate" else "Working...").tr())
    }

    /** Проверяет итоговые параметры и снимает вход на UI-потоке до запуска генерации. */
    private fun generate(step: MapGeneratorSteps, randomizeSeed: Boolean = false) {
        if (isGenerating) return
        val validationParameters = if (step <= MapGeneratorSteps.Landmass)
            editorScreen.newMapParameters.clone()
        else editorScreen.tileMap.mapParameters.clone().also {
            step.copyParameters?.invoke(editorScreen.newMapParameters, it)
        }
        try {
            validationParameters.validatedLatitudeRange()
        } catch (invalid: IllegalArgumentException) {
            ToastPopup(invalid.message ?: "Invalid latitude range", editorScreen, 4000)
            return
        }
        if (!randomizeSeed && step <= MapGeneratorSteps.Landmass && step in seedUsedForStep) {
            newTab.mapParametersTable.reseed()
            seedUsedForStep -= step
        }
        val mapParameters = editorScreen.newMapParameters.clone()
        // Partial сохраняет геометрию карты и не зависит от неприменённого размера новой карты.
        val message = if (step <= MapGeneratorSteps.Landmass)
            mapParameters.mapSize.fixUndesiredSizes(mapParameters.worldWrap) else null
        if (message != null) {
            Gdx.app.postRunnable {
                ToastPopup(message, editorScreen, 4000)
                newTab.mapParametersTable.run {
                    customMapSizeRadius.text = mapParameters.mapSize.radius.toString()
                    customMapWidth.text = mapParameters.mapSize.width.toString()
                    customMapHeight.text = mapParameters.mapSize.height.toString()
                }
            }
            return
        }
        if (step == MapGeneratorSteps.Landmass && mapParameters.type == MapType.empty) {
            ToastPopup(
                "Please don't use step 'Landmass' with map type 'Empty', create a new empty map instead.",
                editorScreen
            )
            return
        }
        if (randomizeSeed) {
            // Новый ненулевой seed выбирается после успешной проверки параметров.
            val previous = editorScreen.newMapParameters.seed
            var next = kotlin.random.Random.nextLong(1L, Long.MAX_VALUE)
            while (next == previous) next = kotlin.random.Random.nextLong(1L, Long.MAX_VALUE)
            newTab.mapParametersTable.reseed(next)
            mapParameters.seed = next
            seedUsedForStep -= step
        }
        if (step in seedUsedForStep) mapParameters.reseed()
        else seedUsedForStep += step
        val targetMap = editorScreen.tileMap
        val targetRuleset = editorScreen.ruleset
        val savedScale = editorScreen.mapHolder.scaleX
        val requestedType = mapParameters.type
        val partialParameters = targetMap.mapParameters.clone().also {
            step.copyParameters?.invoke(mapParameters, it)
            it.seed = mapParameters.seed
        }
        Gdx.input.inputProcessor = null
        setButtonsEnabled(false)

        fun freshMapCompleted(
            generatedMap: TileMap,
            parameters: MapParameters,
            newRuleset: Ruleset,
            selectPage: Int
        ) {
            editorScreen.rememberGenerationParameters(parameters)
            editorScreen.loadMap(generatedMap, newRuleset, selectPage)
            editorScreen.isDirty = true
            setButtonsEnabled(true)
            editorScreen.mapHolder.zoom(savedScale)
        }

        fun stepCompleted() {
            if (step == MapGeneratorSteps.NaturalWonders) editorScreen.naturalWondersNeedRefresh =
                    true
            editorScreen.mapHolder.updateTileGroups()
            editorScreen.isDirty = true
            setButtonsEnabled(true)
            Gdx.input.inputProcessor = editorScreen.stage
        }
        thread(name = "MapGenerator", isDaemon = true) {
            try {
                val (newRuleset, generator) = if (step > MapGeneratorSteps.Landmass) null to null
                else {
                    val loadedRuleset = RulesetCache.getComplexRuleset(mapParameters)
                    loadedRuleset to MapGenerator(loadedRuleset)
                }
                when (step) {
                    MapGeneratorSteps.All -> {
                        val generatedMap = generator!!.generateMap(mapParameters)
                        Gdx.app.postRunnable {
                            freshMapCompleted(
                                generatedMap,
                                mapParameters,
                                newRuleset!!,
                                1
                            )
                        }
                    }

                    MapGeneratorSteps.Landmass -> {
                        mapParameters.type = MapType.empty
                        val generatedMap = generator!!.generateMap(mapParameters)
                        mapParameters.type = requestedType
                        generator.generateSingleStep(generatedMap, step)
                        Gdx.app.postRunnable {
                            freshMapCompleted(
                                generatedMap,
                                mapParameters,
                                newRuleset!!,
                                1
                            )
                        }
                    }

                    else -> {
                        targetMap.mapParameters = partialParameters
                        MapGenerator(targetRuleset).generateSingleStep(targetMap, step)
                        Gdx.app.postRunnable { stepCompleted() }
                    }
                }
            } catch (exception: Exception) {
                Log.error("Exception while generating map", exception)
                Gdx.app.postRunnable {
                    setButtonsEnabled(true)
                    Gdx.input.inputProcessor = editorScreen.stage
                    Popup(editorScreen).apply {
                        addGoodSizedLabel("It looks like we can't make a map with the parameters you requested!".tr())
                        row()
                        addCloseButton()
                    }.open()
                }
            }
        }
    }

    class MapEditorNewMapTab(
        private val parent: MapEditorGenerateTab
    ): Table(BaseScreen.skin) {
        val mapParametersTable = MapParametersTable(parent.editorScreen.newMapParameters, forMapEditor = true) {
            parent.replacePage(0, this)  // A kludge to get the ScrollPanes to recognize changes in vertical layout??
        }

        init {
            top()
            pad(10f)
            add("Map Options".toLabel(fontSize = 24)).row()
            add(mapParametersTable).row()

            mapParametersTable.resourceSelectBox.onChange {
                parent.editorScreen.run {
                    // normally the 'new map' parameters are independent, this needs to be an exception so strategic resource painting will use it
                    tileMap.mapParameters.mapResources = newMapParameters.mapResources
                }
            }
        }
    }

    class MapEditorGenerateStepsTab(
        private val parent: MapEditorGenerateTab
    ): Table(BaseScreen.skin) {
        private val optionGroup = ButtonGroup<CheckBox>()
        val generateButton = "".toTextButton()
        private var choice = MapGeneratorSteps.None
        private val newMapParameters = parent.editorScreen.newMapParameters
        private val tileMap = parent.editorScreen.tileMap
        private val actualMapParameters = tileMap.mapParameters

        init {
            top()
            pad(10f)
            defaults().pad(2.5f)
            add("Generator steps".toLabel(fontSize = 24)).row()
            optionGroup.setMinCheckCount(0)
            for (option in MapGeneratorSteps.values()) {
                if (option <= MapGeneratorSteps.All) continue
                val checkBox = option.label.toCheckBox {
                    choice = option
                    generateButton.enable()
                }
                add(checkBox).row()
                optionGroup.add(checkBox)
            }
            add(generateButton).padTop(15f).row()
            // generate сам снимает параметры до запуска потока; позднее копирование создавало гонку.
            generateButton.onClick { parent.generate(choice) }
        }
    }

    /** Обновляет тот же черновик и форму, сохраняя набор правил, моды и метаданные карты. */
    fun restoreGenerationParameters(source: MapParameters = editorScreen.tileMap.mapParameters) {
        val target = editorScreen.newMapParameters
        target.type = source.type
        target.shape = source.shape
        target.mapSize = source.mapSize.clone()
        target.mapResources = source.mapResources
        target.noRuins = source.noRuins
        target.noNaturalWonders = source.noNaturalWonders
        target.worldWrap = source.worldWrap
        target.seed = source.seed
        target.tilesPerBiomeArea = source.tilesPerBiomeArea
        target.vegetationScale = source.vegetationScale
        target.elevationScale = source.elevationScale
        target.maxCoastExtension = source.maxCoastExtension
        target.elevationExponent = source.elevationExponent
        target.temperatureExtremeness = source.temperatureExtremeness
        target.temperatureShift = source.temperatureShift
        target.vegetationRichness = source.vegetationRichness
        target.rareFeaturesRichness = source.rareFeaturesRichness
        target.resourceRichness = source.resourceRichness
        target.waterThreshold = source.waterThreshold
        target.useLatitudeRange = source.useLatitudeRange
        target.topLatitude = source.topLatitude
        target.bottomLatitude = source.bottomLatitude
        newTab = MapEditorNewMapTab(this)
        replacePage(0, newTab)
    }

    /** Отмена не меняет черновик; подтверждение сохраняет настройки без генерации или мутации карты. */
    private fun requestDefaultParameters() {
        if (isGenerating) return
        com.unciv.ui.popup.ConfirmPopup(
            screen = editorScreen,
            question = "Reset all map generation settings to defaults?",
            confirmText = "Reset to defaults"
        ) {
            if (!isGenerating) {
                restoreGenerationParameters(MapParameters())
                seedUsedForStep.clear()
                editorScreen.rememberGenerationParameters(editorScreen.newMapParameters)
            }
        }.apply { name = "map-generation-reset-confirmation" }.open()
    }
}
