package com.unciv.app.desktop

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapResources
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSizeNew
import com.unciv.logic.map.MapType
import com.unciv.models.translations.tr
import com.unciv.ui.mapeditor.MapEditorScreen
import com.unciv.ui.popup.ConfirmPopup
import com.unciv.ui.utils.UncivSlider
import kotlin.math.roundToInt

/** Проверяет полный сброс через настоящий диалог, не записывая настройки пользователя. */
object TerrainEditorDefaultsGoldenChecks {
    private var saves = 0
    private var saved: MapParameters? = null

    /** In-memory адаптер сохраняет снимок, чтобы проверить момент и содержание записи. */
    fun remember(parameters: MapParameters) {
        saves++
        saved = parameters.clone()
    }

    fun verify(editor: MapEditorScreen) {
        saves = 0
        saved = null
        val draft = editor.newMapParameters
        val originalMods = LinkedHashSet(draft.mods)
        val originalRuleset = draft.baseRuleset
        val map = editor.tileMap
        val mapParameters = map.mapParameters
        val cells = editor.getMapCloneForSave()
        val dirty = editor.isDirty
        draft.apply {
            type = MapType.archipelago
            shape = MapShape.rectangular
            mapSize = MapSizeNew(32, 20)
            mapResources = MapResources.abundant
            worldWrap = true
            noRuins = true
            noNaturalWonders = true
            seed = 42L
            tilesPerBiomeArea = 12
            vegetationScale = 9.0
            elevationScale = 7.0
            waterThreshold = 0.1f
            useLatitudeRange = true
            topLatitude = 25.0
            bottomLatitude = -15.0
            mods = linkedSetOf("test-reset-context")
        }
        val before = settings(draft)
        click(editor, editor.tabs.generate.resetDefaultsButton)
        val cancelPopup = editor.stage.root.findActor<ConfirmPopup>("map-generation-reset-confirmation")
            ?: error("Reset confirmation was not opened")
        check(settings(draft) == before && saves == 0)
        click(editor, buttons(cancelPopup).single { it.text.toString() == "Cancel".tr() })
        check(settings(draft) == before && saves == 0)
        click(editor, editor.tabs.generate.resetDefaultsButton)
        val confirmPopup = editor.stage.root.findActor<ConfirmPopup>("map-generation-reset-confirmation")
            ?: error("Second reset confirmation was not opened")
        click(editor, buttons(confirmPopup).single { it.text.toString() == "Reset to defaults".tr() })
        check(editor.newMapParameters === draft)
        check(settings(draft) == settings(MapParameters())) { "Full reset did not restore all generation settings" }
        check(draft.mods == linkedSetOf("test-reset-context") && draft.baseRuleset == originalRuleset)
        check(saves == 1 && saved != null && settings(saved!!) == settings(draft))
        check(saved!!.mods == draft.mods && saved!!.baseRuleset == originalRuleset)
        check(editor.tileMap === map && map.mapParameters === mapParameters && editor.isDirty == dirty)
        check(map.values.map { listOf(it.position.cpy(), it.baseTerrain, it.terrainFeatures.toList(), it.resource, it.improvement) } ==
            cells.values.map { listOf(it.position.cpy(), it.baseTerrain, it.terrainFeatures.toList(), it.resource, it.improvement) })
        check(!editor.tabs.generate.isGenerating)
        check(editor.tabs.generate.findActor<UncivSlider>("map-climate-scale").value == 6f)
        check(editor.tabs.generate.findActor<UncivSlider>("map-vegetation-scale").value == 3f)
        check(editor.tabs.generate.findActor<UncivSlider>("map-elevation-scale").value == 2f)
        // Искусственный контекст нужен только для проверки сохранения модов при сбросе.
        draft.mods = originalMods
    }

    /** Seed меняется отдельно; контекст ruleset/mods не относится к сбрасываемым параметрам. */
    private fun settings(p: MapParameters): List<Any> = listOf(
        p.type,
        p.mountainZoneCount,
        p.shape,
        p.mapSize.name,
        p.mapSize.radius,
        p.mapSize.width,
        p.mapSize.height,
        p.mapResources,
        p.noRuins,
        p.noNaturalWonders,
        p.worldWrap,
        p.tilesPerBiomeArea,
        p.vegetationScale,
        p.elevationScale,
        p.maxCoastExtension,
        p.elevationExponent,
        p.temperatureExtremeness,
        p.temperatureShift,
        p.vegetationRichness,
        p.rareFeaturesRichness,
        p.resourceRichness,
        p.waterThreshold,
        p.useLatitudeRange,
        p.topLatitude,
        p.bottomLatitude
    )

    private fun buttons(actor: Actor): List<TextButton> =
        (if (actor is TextButton) listOf(actor) else emptyList()) +
            (if (actor is Group) actor.children.flatMap { buttons(it) } else emptyList())

    /** Клик выполняется через Stage после раскладки и отрисовки окна подтверждения. */
    private fun click(editor: MapEditorScreen, button: TextButton) {
        fun validate(actor: Actor) {
            (actor as? Layout)?.validate()
            if (actor is Group) actor.children.forEach { validate(it) }
        }
        validate(editor.stage.root)
        editor.stage.viewport.apply()
        editor.stage.draw()
        val point = button.localToStageCoordinates(Vector2(button.width / 2f, button.height / 2f))
        editor.stage.stageToScreenCoordinates(point)
        editor.stage.touchDown(point.x.roundToInt(), point.y.roundToInt(), 0, Input.Buttons.LEFT)
        editor.stage.touchUp(point.x.roundToInt(), point.y.roundToInt(), 0, Input.Buttons.LEFT)
    }
}
