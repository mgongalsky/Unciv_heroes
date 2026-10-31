package com.unciv.app.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.ScreenUtils
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSizeNew
import com.unciv.logic.map.TileMap
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.mapeditor.MapEditorLayerOverlay
import com.unciv.ui.mapeditor.MapEditorScreen
import com.unciv.ui.mapeditor.MapEditorTerrainLayer

/** Проверяет реальное изображение карты, область Generate и отсутствие изменений данных от просмотра. */
object TerrainEditorLayersGoldenChecks {
    private var checked = false

    fun screen(): MapEditorScreen {
        checked = false
        TerrainEditorGenerationGoldenChecks.reset()
        return MapEditorScreen(map(false)) { TerrainEditorDefaultsGoldenChecks.remember(it) }
            .also { verifyGeometry(it) }
    }
    private fun map(wrap: Boolean): TileMap {
        val rules = ImageGetter.ruleset
        return TileMap(if (wrap) 32 else 14, 10, rules, wrap).apply {
            mapParameters = MapParameters().apply {
                seed = 42L
                shape = MapShape.rectangular
                mapSize = MapSizeNew(if (wrap) 32 else 14, 10)
                worldWrap = wrap
            }
            for ((index, tile) in values.withIndex()) {
                tile.baseTerrain = if (index % 11 == 0) "Ocean" else "Grassland"
                tile.setTerrainTransients()
                if (tile.isLand && index % 5 == 0) tile.addTerrainFeature("Forest")
                if (tile.isLand && index % 7 == 0) tile.addTerrainFeature("Hill")
                tile.temperature = (index % 21 - 10) / 10.0
                tile.humidity = (index % 11) / 10.0
            }
        }
    }
    private fun verifyGeometry(editor: MapEditorScreen) {
        for (groups in editor.mapHolder.tileGroups.values) for (group in groups) {
            val overlay = group.highlightFogCrosshairLayerGroup.findActor<MapEditorLayerOverlay>("map-editor-layer-overlay")
            check(overlay.width.isFinite() && overlay.height.isFinite() && overlay.width > 0f && overlay.height > 0f) {
                "Invalid layer overlay dimensions: ${overlay.width} x ${overlay.height}"
            }
            check(overlay.touchable == Touchable.disabled)
        }
    }
    private fun layout(editor: MapEditorScreen) {
        editor.stage.root.children.forEach { (it as? Layout)?.validate() }
    }
    /** Сравнивается только область карты слева: смена легенды не может дать ложный успех. */
    private fun mapPixels(editor: MapEditorScreen): ByteArray {
        layout(editor)
        editor.stage.viewport.apply()
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        editor.stage.draw()
        return ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.backBufferWidth / 2,
            Gdx.graphics.backBufferHeight, false)
    }
    private fun changedBytes(first: ByteArray, second: ByteArray): Int {
        check(first.size == second.size)
        return first.indices.count { first[it] != second[it] }
    }
    private fun selector(editor: MapEditorScreen): SelectBox<String> =
        editor.stage.root.findActor("map-editor-view-layer")

    fun ready(editor: MapEditorScreen): Boolean {
        if (checked) return TerrainEditorGenerationGoldenChecks.ready(editor)
        check(!editor.terrainLayersVisible)
        check(editor.stage.root.findActor<Actor>("map-editor-layer-controls") == null)
        editor.tabs.selectPage(1)
        editor.tabs.generate.selectPage(0)
        layout(editor)
        check(editor.terrainLayersVisible)
        val select = selector(editor)
        val before = snapshot(editor)
        val dirty = editor.isDirty
        val scale = editor.mapHolder.scaleX
        val scroll = editor.mapHolder.scrollX to editor.mapHolder.scrollY
        select.selected = MapEditorTerrainLayer.GAME.caption
        val game = mapPixels(editor)
        for (layer in MapEditorTerrainLayer.values()) {
            select.selected = layer.caption
            layout(editor)
            check(editor.terrainLayer == layer)
            verifyLayoutAndInput(editor)
            val pixels = mapPixels(editor)
            if (layer != MapEditorTerrainLayer.GAME) {
                check(changedBytes(game, pixels) > 400) { "Layer did not visibly change map pixels: $layer" }
            }
            check(snapshot(editor) == before)
            check(editor.isDirty == dirty)
            check(editor.mapHolder.scaleX == scale)
            check((editor.mapHolder.scrollX to editor.mapHolder.scrollY) == scroll)
        }
        select.selected = MapEditorTerrainLayer.GAME.caption
        check(mapPixels(editor).contentEquals(game)) { "Game terrain was not restored" }
        select.selected = MapEditorTerrainLayer.TEMPERATURE.caption
        editor.tabs.generate.selectPage(1)
        check(selector(editor) === select && editor.terrainLayersVisible)
        for (page in listOf(0, 2, 3, 4, 5, 6)) {
            editor.tabs.selectPage(page)
            check(!editor.terrainLayersVisible)
            check(editor.stage.root.findActor<Actor>("map-editor-layer-controls") == null)
            check(editor.mapHolder.tileGroups.values.all { groups -> groups.all {
                it.highlightFogCrosshairLayerGroup.findActor<MapEditorLayerOverlay>("map-editor-layer-overlay").currentColor() == null
            } })
        }
        editor.tabs.selectPage(1)
        editor.tabs.generate.selectPage(0)
        check(selector(editor).selected == MapEditorTerrainLayer.TEMPERATURE.caption)
        check(editor.terrainLayersVisible)
        val tile = editor.tileMap.values.first { it.isLand && it.terrainFeatures.isEmpty() }
        editor.tabs.selectPage(2)
        editor.tabs.edit.setBrush("Forest", "OtherIcons/Terrains") { it.addTerrainFeature("Forest") }
        val paint = editor.tabs.edit.javaClass.declaredMethods.single { it.name.startsWith("paintTilesWithBrush") }
            .apply { isAccessible = true }
        paint.invoke(editor.tabs.edit, tile)
        check("Forest" in tile.terrainFeatures && editor.isDirty)
        editor.tabs.selectPage(1)
        selector(editor).selected = MapEditorTerrainLayer.VEGETATION.caption
        val group = editor.mapHolder.tileGroups.getValue(tile).first()
        val overlay = group.highlightFogCrosshairLayerGroup.findActor<MapEditorLayerOverlay>("map-editor-layer-overlay")
        check(overlay.currentColor() == MapEditorTerrainLayer.VEGETATION.colorFor(tile))
        val wrapped = map(true)
        wrapped.values.forEach { it.temperature = null; it.humidity = null }
        editor.loadMap(wrapped, ImageGetter.ruleset, selectPage = 1)
        verifyGeometry(editor)
        check(editor.mapHolder.tileGroups.values.all { it.size == 3 })
        for (groups in editor.mapHolder.tileGroups.values) {
            check(groups.map {
                it.highlightFogCrosshairLayerGroup.findActor<MapEditorLayerOverlay>("map-editor-layer-overlay").currentColor()
            }.distinct().size == 1)
        }
        selector(editor).selected = MapEditorTerrainLayer.TEMPERATURE.caption
        check(MapEditorTerrainLayer.TEMPERATURE.colorFor(wrapped.values.first()) == com.badlogic.gdx.graphics.Color.valueOf("707070"))
        val recreated = editor.recreate() as MapEditorScreen
        try {
            check(recreated.terrainLayer == editor.terrainLayer && recreated.terrainLayersVisible)
            check(recreated.tileMap === editor.tileMap)
        } finally { recreated.dispose() }
        editor.tabs.generate.selectPage(0)
        selector(editor).selected = MapEditorTerrainLayer.ELEVATION.caption
        verifyLayoutAndInput(editor)
        checked = true
        return TerrainEditorGenerationGoldenChecks.ready(editor)
    }
    private fun snapshot(editor: MapEditorScreen) = listOf(editor.tileMap.mapParameters.seed,
        editor.tileMap.values.map { listOf(it.position.cpy(), it.baseTerrain, it.terrainFeatures.toList(),
            it.temperature, it.humidity, it.resource, it.improvement, it.naturalWonder) })

    /** Проверяет границы текста и прохождение ввода; режим экрана после проверки восстанавливается. */
    private fun verifyLayoutAndInput(editor: MapEditorScreen) {
        layout(editor)
        val select = selector(editor)
        val origin = select.localToStageCoordinates(Vector2())
        check(origin.x >= 0f && origin.y >= 0f)
        check(origin.x + select.width <= editor.stage.width + 1f)
        check(origin.y + select.height <= editor.stage.height + 1f)
        val legend = editor.stage.root.findActor<Label>("map-editor-layer-legend")
        check(legend.height + 1f >= legend.prefHeight) { "Legend text is clipped" }
        check(legend.y + legend.height <= select.y + 1f) { "Legend overlaps selector" }
        val point = editor.mapHolder.tileGroups.values.asSequence().map { it.first() }
            .map { it.localToStageCoordinates(Vector2(it.width / 2f, it.height / 2f)) }
            .first { it.x > 20f && it.x < editor.stage.width - editor.getToolsWidth() - 20f &&
                it.y > 20f && it.y < editor.stage.height - 20f }
        val layer = editor.terrainLayer
        try {
            editor.terrainLayer = MapEditorTerrainLayer.GAME
            val hit = editor.stage.hit(point.x, point.y, true)
            check(hit != null)
            for (mode in MapEditorTerrainLayer.values()) {
                editor.terrainLayer = mode
                check(editor.stage.hit(point.x, point.y, true) === hit)
            }
        } finally { editor.terrainLayer = layer }
    }
}
