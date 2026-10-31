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
        val sliders = descendants(expander.innerTable).filterIsInstance<UncivSlider>()
        val scales = sliders.filter { it.minValue == 0.5f && it.maxValue == 30f }
        check(scales.size == 2) { "Expected elevation and vegetation scale controls" }
        val elevation = scales[0]
        val vegetation = scales[1]
        val climate = sliders.single { it.minValue == 1f && it.maxValue == 15f }
        elevation.value = 8f
        check(parameters.elevationScale == 8.0 && parameters.vegetationScale == 3.0 && parameters.tilesPerBiomeArea == 6) { "Elevation control changed other scales" }
        vegetation.value = 12f
        check(parameters.elevationScale == 8.0 && parameters.vegetationScale == 12.0 && parameters.tilesPerBiomeArea == 6) { "Vegetation control changed other scales" }
        climate.value = 10f
        check(parameters.elevationScale == 8.0 && parameters.vegetationScale == 12.0 && parameters.tilesPerBiomeArea == 10) { "Climate control changed other scales" }
        val reset = descendants(expander.innerTable).filterIsInstance<TextButton>().single()
        val event = InputEvent().apply { target = reset }
        val listeners = reset.listeners.filterIsInstance<ClickListener>()
        check(listeners.isNotEmpty()) { "Reset has no click listener" }
        listeners.forEach { it.clicked(event, 0f, 0f) }
        check(parameters.elevationScale == 2.0 && parameters.vegetationScale == 3.0 && parameters.tilesPerBiomeArea == 6) { "Reset did not restore scale defaults" }
        check(elevation.value == 2f && vegetation.value == 3f && climate.value == 6f) { "Reset left stale sliders" }
        // Обычная форма сохраняет исторический reseed при Reset.
        val seedField = descendants(expander.innerTable)
            .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.TextField>().single()
        check(parameters.seed != 42L) { "Reset did not reseed ordinary map parameters" }
        check(seedField.text == parameters.seed.toString()) { "Seed field is stale after Reset" }
        seedField.text = "42"
        seedField.fire(com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent())
        check(parameters.seed == 42L) { "Seed input listener did not update parameters" }

        val content = expander.innerTable
        content.remove()
        val root = Table(BaseScreen.skin).apply { setFillParent(true); pad(16f) }
        root.add(ScrollPane(content, BaseScreen.skin).apply {
            setScrollingDisabled(true, false)
            setFadeScrollBars(false)
        }).width(650f).growY()
        screen.stage.addActor(root)
        root.validate()
        check(content.prefWidth > 0f && content.prefHeight > 0f) { "Advanced settings have no layout size" }
    }

    private fun descendants(actor: Actor): List<Actor> = listOf(actor) +
        if (actor is Group) actor.children.flatMap { descendants(it) } else emptyList()
}
