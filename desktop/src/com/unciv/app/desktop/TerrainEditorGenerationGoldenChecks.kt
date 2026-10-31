package com.unciv.app.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSizeNew
import com.unciv.logic.map.MapType
import com.unciv.logic.map.TileMap
import com.unciv.ui.mapeditor.MapEditorScreen
import com.unciv.ui.utils.ExpanderTab
import kotlin.math.roundToInt
import com.unciv.ui.utils.extensions.isEnabled

/** Проверяет доступность низа формы и реальные асинхронные запуски закреплённой генерации. */
object TerrainEditorGenerationGoldenChecks {
    private var phase = 0
    private var completed = 0
    private var previousMap: TileMap? = null
    private var requestedSeed = 0L
    private lateinit var requested: MapParameters

    fun reset() {
        phase = 0
        completed = 0
        previousMap = null
    }

    /** Проверяет подтверждение сброса, затем ожидает два настоящих запуска между кадрами. */
    fun ready(editor: MapEditorScreen): Boolean {
        if (phase == 2) return true
        if (phase == 0) {
            TerrainEditorDefaultsGoldenChecks.verify(editor)
            verifyScroll(editor)
            editor.newMapParameters.apply {
                type = MapType.pangaea
                shape = MapShape.rectangular
                mapSize = MapSizeNew(20, 12)
                worldWrap = false
                noRuins = true
                noNaturalWonders = true
                tilesPerBiomeArea = 8
                vegetationScale = 4.5
                elevationScale = 3.5
            }
            start(editor)
            phase = 1
            return false
        }
        if (editor.tabs.generate.isGenerating) return false
        check(editor.tileMap !== previousMap) { "Generation did not replace the map" }
        check(editor.tileMap.mapParameters.seed == requestedSeed) { "Requested seed was not applied" }
        val actual = editor.tileMap.mapParameters
        check(actual.type == requested.type && actual.shape == requested.shape)
        check(actual.mapSize.width == requested.mapSize.width && actual.mapSize.height == requested.mapSize.height)
        check(actual.tilesPerBiomeArea == requested.tilesPerBiomeArea)
        check(actual.vegetationScale == requested.vegetationScale && actual.elevationScale == requested.elevationScale)
        check(actual.noRuins && actual.noNaturalWonders && !actual.worldWrap)
        check(editor.isDirty && Gdx.input.inputProcessor === editor.stage)
        check(editor.tabs.activePage == 1 && editor.terrainLayersVisible)
        check(editor.tabs.generate.regenerateButton.isEnabled)
        check(editor.tabs.generate.resetDefaultsButton.isEnabled)
        verifyButton(editor)
        completed++
        if (completed == 1) {
            // Закреплённая кнопка выполняет полную генерацию также из Partial.
            editor.tabs.generate.selectPage(1)
            start(editor)
            return false
        }
        editor.tabs.generate.selectPage(0)
        phase = 2
        return true
    }

    /** Во время генерации повторный ввод и полный сброс настроек заблокированы. */
    private fun start(editor: MapEditorScreen) {
        previousMap = editor.tileMap
        val oldSeed = editor.newMapParameters.seed
        requested = editor.newMapParameters.clone()
        click(editor, editor.tabs.generate.regenerateButton)
        check(editor.tabs.generate.isGenerating) { "Pinned button did not start generation" }
        check(!editor.tabs.generate.regenerateButton.isEnabled) { "Generation button still accepts input" }
        check(!editor.tabs.generate.resetDefaultsButton.isEnabled) { "Reset is available during generation" }
        requestedSeed = editor.newMapParameters.seed
        check(requestedSeed > 0L && requestedSeed != oldSeed)
        click(editor, editor.tabs.generate.regenerateButton)
        check(editor.newMapParameters.seed == requestedSeed)
        check(editor.tileMap === previousMap)
    }

    /** Проверяет весь последний элемент внутри viewport, а не только его наличие в дереве. */
    private fun verifyScroll(editor: MapEditorScreen) {
        editor.tabs.generate.selectPage(0)
        layout(editor)
        val expander = editor.stage.root.findActor<ExpanderTab>("map-advanced-settings")
        if (!expander.isOpen) expander.toggle()
        editor.stage.act(0.5f)
        layout(editor)
        editor.tabs.generate.setPageScrollY(0, 0f)
        layout(editor)
        val button = editor.tabs.generate.regenerateButton
        val pinned = button.localToStageCoordinates(Vector2())
        editor.tabs.generate.setPageScrollY(0, Float.MAX_VALUE)
        layout(editor)
        val reset = editor.stage.root.findActor<TextButton>("map-reset-advanced")
        var ancestor: Actor? = reset.parent
        while (ancestor != null && ancestor !is ScrollPane) ancestor = ancestor.parent
        val pane = ancestor as? ScrollPane ?: error("No form scroll pane")
        pane.updateVisualScroll()
        val bottom = reset.localToStageCoordinates(Vector2())
        val viewport = pane.localToStageCoordinates(Vector2())
        check(pane.maxY > 0f) { "Expanded form should require scrolling" }
        check(bottom.y >= viewport.y - 1f && bottom.y >= 0f) {
            "Last form control is below viewport: control=$bottom viewport=$viewport height=${pane.height}"
        }
        check(bottom.y + reset.height <= viewport.y + pane.height + 1f)
        val centre = reset.localToStageCoordinates(Vector2(reset.width / 2f, reset.height / 2f))
        val hit = editor.stage.hit(centre.x, centre.y, true)
        check(hit === reset || hit?.isDescendantOf(reset) == true) { "Last form control is clipped or covered" }
        check(button.localToStageCoordinates(Vector2()).epsilonEquals(pinned, 0.1f)) {
            "Generation button moved with form scrolling"
        }
        verifyButton(editor)
    }

    private fun verifyButton(editor: MapEditorScreen) {
        layout(editor)
        val button = editor.tabs.generate.regenerateButton
        val origin = button.localToStageCoordinates(Vector2())
        val detail =
                "button=$origin size=${button.width}x${button.height}; parent=${button.parent.width}x${button.parent.height}; stage=${editor.stage.width}x${editor.stage.height}"
        check(origin.y >= 0f && origin.y + button.height <= editor.stage.height + 1f) { detail }
        check(origin.x >= 0f && origin.x + button.width <= editor.stage.width + 1f) { detail }
        val selector = editor.stage.root.findActor<Actor>("map-editor-view-layer")
        check(button.parent === selector.parent && button.y + button.height <= selector.y + 1f) { detail }
    }

    /** Клик проходит через Stage и обычные обработчики ввода кнопки. */
    private fun click(editor: MapEditorScreen, button: TextButton) {
        layout(editor)
        val point = button.localToStageCoordinates(Vector2(button.width / 2f, button.height / 2f))
        editor.stage.stageToScreenCoordinates(point)
        editor.stage.touchDown(point.x.roundToInt(), point.y.roundToInt(), 0, Input.Buttons.LEFT)
        editor.stage.touchUp(point.x.roundToInt(), point.y.roundToInt(), 0, Input.Buttons.LEFT)
    }

    /** ScrollPane обновляет положение содержимого при draw, поэтому проверяем геометрию показанного кадра. */
    private fun layout(editor: MapEditorScreen) {
        fun validate(group: Group) {
            (group as? Layout)?.validate()
            group.children.forEach { if (it is Group) validate(it) else (it as? Layout)?.validate() }
        }
        validate(editor.stage.root)
        editor.stage.viewport.apply()
        editor.stage.draw()
    }
}
