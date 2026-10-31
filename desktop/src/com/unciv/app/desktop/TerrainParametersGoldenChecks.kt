package com.unciv.app.desktop

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.unciv.logic.map.MapParameters
import com.unciv.ui.newgamescreen.MapParametersTable
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ExpanderTab
import com.unciv.ui.utils.UncivSlider

/** Проверяет ползунки настоящей общей формы без изменения настроек обычной игры. */
object TerrainParametersGoldenChecks {
    fun open(screen: BaseScreen) {
        val parameters = MapParameters().apply { seed = 42L }
        val form = MapParametersTable(parameters, forMapEditor = true)
        val expander = descendants(form).filterIsInstance<ExpanderTab>().single()
        val elevation = form.findActor<UncivSlider>("map-elevation-scale")
        val vegetation = form.findActor<UncivSlider>("map-vegetation-scale")
        val climate = form.findActor<UncivSlider>("map-climate-scale")
        check(
            !elevation.isDescendantOf(expander) && !vegetation.isDescendantOf(expander) && !climate.isDescendantOf(
                expander
            )
        ) {
            "Editor scales are still hidden in Advanced"
        }
        check(descendants(form).filterIsInstance<UncivSlider>().count { it.maxValue == 30f } == 3) {
            "Editor scale controls are duplicated"
        }
        elevation.value = 8f
        check(parameters.elevationScale == 8.0 && parameters.vegetationScale == 3.0 && parameters.tilesPerBiomeArea == 6)
        vegetation.value = 12f
        check(parameters.elevationScale == 8.0 && parameters.vegetationScale == 12.0 && parameters.tilesPerBiomeArea == 6)
        climate.value = 10f
        check(parameters.elevationScale == 8.0 && parameters.vegetationScale == 12.0 && parameters.tilesPerBiomeArea == 10)
        check(form.findActor<com.badlogic.gdx.scenes.scene2d.ui.Label>("map-climate-scale-value").text.toString() == "10")
        check(form.findActor<com.badlogic.gdx.scenes.scene2d.ui.Label>("map-vegetation-scale-value").text.toString() == "12.0")
        check(form.findActor<com.badlogic.gdx.scenes.scene2d.ui.Label>("map-elevation-scale-value").text.toString() == "8.0")
        verifyLatitudeControls(form, parameters)
        val reset = descendants(expander.innerTable).filterIsInstance<TextButton>().single()
        val event = InputEvent().apply { target = reset }
        val listeners = reset.listeners.filterIsInstance<ClickListener>()
        check(listeners.isNotEmpty())
        listeners.forEach { it.clicked(event, 0f, 0f) }
        verifyLatitudeReset(form, parameters)
        check(parameters.elevationScale == 2.0 && parameters.vegetationScale == 3.0 && parameters.tilesPerBiomeArea == 6)
        check(elevation.value == 2f && vegetation.value == 3f && climate.value == 6f)
        check(form.findActor<com.badlogic.gdx.scenes.scene2d.ui.Label>("map-climate-scale-value").text.toString() == "6")
        check(form.findActor<com.badlogic.gdx.scenes.scene2d.ui.Label>("map-vegetation-scale-value").text.toString() == "3.0")
        val seedField =
                descendants(expander.innerTable).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.TextField>()
                    .single()
        check(parameters.seed != 42L && seedField.text == parameters.seed.toString())
        seedField.text = "42"
        seedField.fire(com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent())
        check(parameters.seed == 42L)
        // Обычная форма сохраняет прежнее размещение и диапазоны настроек.
        val ordinary = MapParametersTable(MapParameters(), forMapEditor = false)
        val ordinaryAdvanced = descendants(ordinary).filterIsInstance<ExpanderTab>().single()
        val ordinarySliders =
                descendants(ordinaryAdvanced.innerTable).filterIsInstance<UncivSlider>()
        check(ordinarySliders.count { it.minValue == 0.5f && it.maxValue == 30f } == 2)
        check(ordinarySliders.count { it.minValue == 1f && it.maxValue == 15f } == 1)
        val root = Table(BaseScreen.skin).apply { setFillParent(true); pad(16f) }
        root.add(ScrollPane(form, BaseScreen.skin).apply {
            setScrollingDisabled(true, false)
            setFadeScrollBars(false)
        }).width(650f).growY()
        screen.stage.addActor(root)
        root.validate()
        check(form.prefWidth > 0f && form.prefHeight > 0f)
    }

    private fun descendants(actor: Actor): List<Actor> = listOf(actor) +
        if (actor is Group) actor.children.flatMap { descendants(it) } else emptyList()

    /** Проверяет реальные события полей диапазона, не вызывая генерацию для ошибочного черновика. */
    private fun verifyLatitudeControls(form: MapParametersTable, parameters: MapParameters) {
        val enabled =
                form.findActor<com.badlogic.gdx.scenes.scene2d.ui.CheckBox>("map-latitude-enabled")
        val top = form.findActor<com.badlogic.gdx.scenes.scene2d.ui.TextField>("map-top-latitude")
        val bottom =
                form.findActor<com.badlogic.gdx.scenes.scene2d.ui.TextField>("map-bottom-latitude")
        val error = form.findActor<com.badlogic.gdx.scenes.scene2d.ui.Label>("map-latitude-error")
        fun input(field: com.badlogic.gdx.scenes.scene2d.ui.TextField, text: String) {
            field.text = text
            field.fire(com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent())
        }
        check(!enabled.isChecked && top.isDisabled && bottom.isDisabled)
        enabled.isChecked = true
        check(parameters.useLatitudeRange && !top.isDisabled && !bottom.isDisabled)
        input(top, "20")
        input(bottom, "-20")
        check(
            parameters.validatedLatitudeRange() == com.unciv.pure.domain.mapgeneration.LatitudeRange(
                20.0,
                -20.0
            )
        )
        check(error.text.isEmpty())
        input(top, "100")
        check(parameters.topLatitude == 100.0 && error.text.isNotEmpty())
        input(top, "")
        check(parameters.topLatitude.isNaN() && error.text.isNotEmpty())
        input(top, "60")
        input(bottom, "20")
        check(error.text.isEmpty())
        val controls =
                descendants(form).filterIsInstance<com.unciv.ui.newgamescreen.MapLatitudeControls>()
                    .single()
        parameters.shape = com.unciv.logic.map.MapShape.flatEarth
        controls.updateState()
        check(error.text.isNotEmpty())
        parameters.shape = com.unciv.logic.map.MapShape.hexagonal
        controls.updateState()
        check(error.text.isEmpty())
        // Обычная форма новой игры не получает редакторский блок.
        val ordinary = MapParametersTable(MapParameters(), forMapEditor = false)
        check(ordinary.findActor<Actor>("map-latitude-enabled") == null)
    }

    /** Reset синхронизирует модель и поля; затем проверяется загрузка карты в настоящем редакторе. */
    private fun verifyLatitudeReset(form: MapParametersTable, parameters: MapParameters) {
        check(!parameters.useLatitudeRange)
        check(parameters.topLatitude == 60.0 && parameters.bottomLatitude == 20.0)
        val enabled =
                form.findActor<com.badlogic.gdx.scenes.scene2d.ui.CheckBox>("map-latitude-enabled")
        val top = form.findActor<com.badlogic.gdx.scenes.scene2d.ui.TextField>("map-top-latitude")
        val bottom =
                form.findActor<com.badlogic.gdx.scenes.scene2d.ui.TextField>("map-bottom-latitude")
        check(!enabled.isChecked && top.isDisabled && bottom.isDisabled)
        check(top.text == "60.0" && bottom.text == "20.0")
        verifyEditorLatitudeRestore()
    }

    /** Проверяет восстановление редактора и ранний отказ Partial без фоновой мутации. */
    private fun verifyEditorLatitudeRestore() {
        val rules = com.unciv.ui.images.ImageGetter.ruleset
        fun map(top: Double, bottom: Double) =
                com.unciv.logic.map.TileMap(7, 5, rules, false).apply {
                    mapParameters = MapParameters().apply {
                        shape = com.unciv.logic.map.MapShape.rectangular
                        mapSize = com.unciv.logic.map.MapSizeNew(7, 5)
                        seed = 42L
                        useLatitudeRange = true
                        topLatitude = top
                        bottomLatitude = bottom
                    }
                }

        val originalProcessor = com.badlogic.gdx.Gdx.input.inputProcessor
        val first = map(60.0, 20.0)
        val editor = com.unciv.ui.mapeditor.MapEditorScreen(first)
        try {
            check(editor.newMapParameters !== first.mapParameters)
            check(editor.newMapParameters.validatedLatitudeRange() == first.mapParameters.validatedLatitudeRange())
            val draft = editor.newMapParameters
            draft.topLatitude = 70.0
            check(first.mapParameters.topLatitude == 60.0)
            val second = map(20.0, -20.0)
            editor.loadMap(second, rules, selectPage = 1)
            editor.tabs.generate.selectPage(0)
            check(editor.newMapParameters === draft)
            check(draft.validatedLatitudeRange() == second.mapParameters.validatedLatitudeRange())
            check(draft.mapSize !== second.mapParameters.mapSize)
            val topField =
                    editor.tabs.generate.findActor<com.badlogic.gdx.scenes.scene2d.ui.TextField>("map-top-latitude")
            val bottomField =
                    editor.tabs.generate.findActor<com.badlogic.gdx.scenes.scene2d.ui.TextField>("map-bottom-latitude")
            check(topField.text == "20.0" && bottomField.text == "-20.0")
            val before = second.values.map { it.baseTerrain to it.terrainFeatures.toList() }
            val actualParameters = second.mapParameters
            draft.topLatitude = 100.0
            val generate =
                    com.unciv.ui.mapeditor.MapEditorGenerateTab::class.java.getDeclaredMethod(
                        "generate",
                        com.unciv.ui.mapeditor.MapGeneratorSteps::class.java,
                        Boolean::class.javaPrimitiveType!!
                    ).apply { isAccessible = true }
            // Partial сохраняет прежний режим seed; ошибочный диапазон отклоняется до запуска.
            generate.invoke(
                editor.tabs.generate,
                com.unciv.ui.mapeditor.MapGeneratorSteps.HumidityAndTemperature,
                false
            )
            check(second.mapParameters === actualParameters)
            check(second.mapParameters.seed == 42L)
            check(second.mapParameters.topLatitude == 20.0)
            check(before == second.values.map { it.baseTerrain to it.terrainFeatures.toList() })
            check(!editor.isDirty)
            check(!editor.tabs.generate.isGenerating)
        } finally {
            editor.dispose()
            com.badlogic.gdx.Gdx.input.inputProcessor = originalProcessor
        }
    }
}
