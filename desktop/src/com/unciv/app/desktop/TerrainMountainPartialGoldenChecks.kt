package com.unciv.app.desktop

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.unciv.logic.map.TileMap
import com.unciv.models.translations.tr
import com.unciv.ui.mapeditor.MapEditorGenerateTab
import com.unciv.ui.mapeditor.MapEditorScreen
import kotlin.math.roundToInt

/** Проверяет, что UI-reseed повторного Partial не передвигает стены горного шаблона. */
object TerrainMountainPartialGoldenChecks {
    private var phase = 0
    private var map: TileMap? = null
    private var seed = 0L
    private var walls = emptySet<Pair<Float, Float>>()

    fun reset() { phase = 0; map = null }

    fun ready(editor: MapEditorScreen): Boolean {
        if (phase == 3) return true
        if (phase == 0) {
            map = editor.tileMap
            seed = editor.tileMap.mapParameters.seed
            walls = wallPositions(editor.tileMap)
            // Черновик после Reset имеет другой тип и seed; Partial обязан использовать структуру карты.
            editor.newMapParameters.seed = seed xor 1L
            editor.tabs.generate.selectPage(1)
            val page = descendants(editor.tabs.generate).filterIsInstance<MapEditorGenerateTab.MapEditorGenerateStepsTab>().single()
            descendants(page).filterIsInstance<CheckBox>().single { it.text.toString() == "Raise mountains and hills".tr() }.isChecked = true
            click(editor, page.generateButton)
            check(editor.tabs.generate.isGenerating)
            phase = 1
            return false
        }
        if (editor.tabs.generate.isGenerating) return false
        check(editor.tileMap === map)
        check(editor.tileMap.mapParameters.seed == seed) { "Partial changed mountain structure seed" }
        check(wallPositions(editor.tileMap) == walls) { "Partial moved mountain zone boundaries" }
        if (phase == 1) {
            val page = descendants(editor.tabs.generate).filterIsInstance<MapEditorGenerateTab.MapEditorGenerateStepsTab>().single()
            click(editor, page.generateButton)
            check(editor.tabs.generate.isGenerating)
            phase = 2
            return false
        }
        editor.tabs.generate.selectPage(0)
        phase = 3
        return true
    }

    private fun wallPositions(map: TileMap) = map.values.filter { it.isImpassible() }
        .map { it.position.x to it.position.y }.toSet()

    private fun descendants(actor: Actor): List<Actor> = listOf(actor) +
        if (actor is Group) actor.children.flatMap { descendants(it) } else emptyList()

    /** Кнопка шагов находится внизу списка; перед реальным кликом раскрываем её прокруткой. */
    private fun click(editor: MapEditorScreen, button: TextButton) {
        fun validate(actor: Actor) {
            (actor as? Layout)?.validate()
            if (actor is Group) actor.children.forEach { validate(it) }
        }
        validate(editor.stage.root)
        editor.tabs.generate.setPageScrollY(1, Float.MAX_VALUE)
        validate(editor.stage.root)
        editor.stage.viewport.apply()
        editor.stage.draw()
        val point = button.localToStageCoordinates(Vector2(button.width / 2f, button.height / 2f))
        check(point.y >= 0f && point.y <= editor.stage.height) { "Partial Generate is outside the viewport" }
        editor.stage.stageToScreenCoordinates(point)
        editor.stage.touchDown(point.x.roundToInt(), point.y.roundToInt(), 0, Input.Buttons.LEFT)
        editor.stage.touchUp(point.x.roundToInt(), point.y.roundToInt(), 0, Input.Buttons.LEFT)
    }
}
