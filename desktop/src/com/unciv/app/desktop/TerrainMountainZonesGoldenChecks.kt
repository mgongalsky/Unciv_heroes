package com.unciv.app.desktop

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.ScreenUtils
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSizeNew
import com.unciv.logic.map.MapType
import com.unciv.logic.map.TileMap
import com.unciv.ui.mapeditor.MapEditorScreen
import com.unciv.ui.newgamescreen.MapParametersTable
import kotlin.math.roundToInt

/** Проверяет настоящий выбор шаблона, число зон, генерацию и показ карты без изменения baseline. */
object TerrainMountainZonesGoldenChecks {
    private var phase = 0
    private var previousMap: TileMap? = null
    private var previousPixels = byteArrayOf()

    fun reset() {
        phase = 0
        previousMap = null
        TerrainMountainPartialGoldenChecks.reset()
    }

    fun ready(editor: MapEditorScreen): Boolean {
        if (phase == 3) return TerrainMountainPartialGoldenChecks.ready(editor)
        if (phase == 0) {
            val draft = editor.newMapParameters
            draft.apply {
                type = MapType.pangaea
                shape = MapShape.rectangular
                mapSize = MapSizeNew(36, 28)
                worldWrap = false
                noRuins = true
                noNaturalWonders = true
            }
            editor.tabs.generate.restoreGenerationParameters(draft.clone())
            val form = descendants(editor.tabs.generate).filterIsInstance<MapParametersTable>()
                .single()
            check(form.findActor<Actor>("map-mountain-zone-count") == null)
            select(form, MapType.mountainCross)
            count(form).selected = 7
            check(draft.mountainZoneCount == 7 && draft.clone().mountainZoneCount == 7)
            select(form, MapType.pangaea)
            check(form.findActor<Actor>("map-mountain-zone-count") == null)
            select(form, MapType.mountainZones)
            check(count(form).selected == 7)
            select(form, MapType.mountainCross)
            count(form).selected = 5
            start(editor)
            phase = 1
            return false
        }
        if (editor.tabs.generate.isGenerating) return false
        check(editor.tileMap !== previousMap)
        val expectedType = if (phase == 1) MapType.mountainCross else MapType.mountainZones
        val expectedCount = if (phase == 1) 5 else 6
        check(editor.tileMap.mapParameters.type == expectedType)
        check(editor.tileMap.mapParameters.mountainZoneCount == expectedCount)
        check(editor.tileMap.values.any { it.isImpassible() } && editor.tileMap.values.any { it.isLand && !it.isImpassible() })
        check(editor.tabs.activePage == 1 && editor.terrainLayersVisible)
        val form = descendants(editor.tabs.generate).filterIsInstance<MapParametersTable>().single()
        check(form.mapTypeSelectBox.selected.value == expectedType && count(form).selected == expectedCount)
        check(!pixels(editor).contentEquals(previousPixels)) { "Generated mountain map did not change displayed pixels" }
        if (phase == 1) {
            select(form, MapType.mountainZones)
            count(form).selected = 6
            start(editor)
            phase = 2
            return false
        }
        // Полный Reset учитывает новое поле, но оставляет открытую горную карту.
        TerrainEditorDefaultsGoldenChecks.verify(editor)
        check(editor.newMapParameters.mountainZoneCount == 5)
        phase = 3
        return TerrainMountainPartialGoldenChecks.ready(editor)
    }

    private fun select(form: MapParametersTable, type: String) {
        form.mapTypeSelectBox.selected = form.mapTypeSelectBox.items.first { it.value == type }
    }

    private fun count(form: MapParametersTable): SelectBox<Int> = form.findActor("map-mountain-zone-count")

    private fun start(editor: MapEditorScreen) {
        previousMap = editor.tileMap
        previousPixels = pixels(editor)
        val button = editor.tabs.generate.regenerateButton
        val point = button.localToStageCoordinates(Vector2(button.width / 2f, button.height / 2f))
        editor.stage.stageToScreenCoordinates(point)
        editor.stage.touchDown(point.x.roundToInt(), point.y.roundToInt(), 0, Input.Buttons.LEFT)
        editor.stage.touchUp(point.x.roundToInt(), point.y.roundToInt(), 0, Input.Buttons.LEFT)
        check(editor.tabs.generate.isGenerating) { "Mountain template did not start from the UI" }
    }

    /** Изображение слева содержит карту; смена подписи формы не может дать ложный успех. */
    private fun pixels(editor: MapEditorScreen): ByteArray {
        fun validate(actor: Actor) {
            (actor as? Layout)?.validate()
            if (actor is Group) actor.children.forEach { validate(it) }
        }
        validate(editor.stage.root)
        editor.stage.viewport.apply()
        editor.stage.draw()
        return ScreenUtils.getFrameBufferPixels(0, 0,
            com.badlogic.gdx.Gdx.graphics.backBufferWidth / 2, com.badlogic.gdx.Gdx.graphics.backBufferHeight, false)
    }

    private fun descendants(actor: Actor): List<Actor> = listOf(actor) +
        if (actor is Group) actor.children.flatMap { descendants(it) } else emptyList()
}
