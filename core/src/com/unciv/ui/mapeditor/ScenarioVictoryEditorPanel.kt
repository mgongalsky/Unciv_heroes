package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.map.ScenarioVictoryEditorCommands
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.translations.tr
import com.unciv.pure.domain.scenario.*
import com.unciv.pure.domain.victory.CaptureCityObjective
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton

/** Форма авторских условий: значения виджетов применяются одной проверяемой командой. */
class ScenarioVictoryEditorPanel(
    private val map: TileMap,
    private val ruleset: Ruleset,
    private val panelWidth: Float,
    private val changed: () -> Unit
) : Table(BaseScreen.skin) {
    private val error = "".toLabel(Color.SALMON).apply { setWrap(true) }

    init {
        name = "scenario-victory-panel"
        refresh()
    }

    /** Восстанавливает форму из карты; неприменённые изменения не записываются автоматически. */
    fun refresh() {
        clear()
        top()
        defaults().pad(5f).left()
        val settings = map.scenarioVictorySettings?.deepCopy() ?: ScenarioVictorySettings()
        val policies = settings.rules.associateBy { it.victoryType }
        val modes = linkedMapOf<String, SelectBox<ScenarioVictoryMode>>()
        val defaults = linkedMapOf<String, CheckBox>()
        add("Victory Conditions".toLabel()).colspan(2).row()
        for (type in (ruleset.victories.keys + policies.keys).distinct()) {
            add(type.toLabel()).width(panelWidth * 0.4f)
            val mode = SelectBox<ScenarioVictoryMode>(skin).apply {
                name = "scenario-victory-mode-$type"
                setItems(*ScenarioVictoryMode.values())
                selected = policies[type]?.mode ?: ScenarioVictoryMode.Optional
            }
            modes[type] = mode
            add(mode).growX().row()
            val default = CheckBox("Enabled by default".tr(), skin).apply {
                isChecked = policies[type]?.defaultEnabled ?: (type != CaptureCityObjective.VICTORY_TYPE)
            }
            defaults[type] = default
            add(default).colspan(2).row()
        }
        add("Capture and hold all your objective cities to win!".toLabel().apply { setWrap(true) })
            .width(panelWidth - 30f).colspan(2).row()
        val objectives = settings.captureObjectives.associateBy { it.cityId }
        val selectedCities = linkedMapOf<String, CheckBox>()
        val eligibleSlots = linkedMapOf<String, List<Pair<String, CheckBox>>>()
        for (city in map.scenarioCities) {
            val enabled = CheckBox(city.name, skin).apply {
                name = "scenario-victory-city-${city.id}"
                isChecked = city.id in objectives
            }
            selectedCities[city.id] = enabled
            add(enabled).colspan(2).row()
            val choices = map.scenarioPlayerSlots.map { slot ->
                val choice = CheckBox(slot.name.ifBlank { slot.id }, skin).apply {
                    name = "scenario-victory-slot-${city.id}-${slot.id}"
                    isChecked = slot.id in objectives[city.id]?.eligibleSlotIds.orEmpty()
                }
                add(choice).padLeft(15f).colspan(2).row()
                slot.id to choice
            }
            eligibleSlots[city.id] = choices
        }
        if (map.scenarioCities.isEmpty() || map.scenarioPlayerSlots.isEmpty()) {
            add("Add cities and player slots before assigning capture objectives".toLabel().apply { setWrap(true) })
                .width(panelWidth - 30f).colspan(2).row()
        }
        add("Apply victory settings".toTextButton().apply { name = "scenario-victory-apply" }.onClick {
            attempt {
                val draft = ScenarioVictorySettings(
                    rules = ArrayList(modes.map { (type, mode) ->
                        ScenarioVictoryRule(type, mode.selected, defaults.getValue(type).isChecked)
                    }),
                    captureObjectives = ArrayList(selectedCities.filterValues { it.isChecked }.keys.map { cityId ->
                        CaptureCityObjective(cityId, ArrayList(eligibleSlots.getValue(cityId)
                            .filter { it.second.isChecked }.map { it.first }))
                    })
                )
                if (ScenarioVictoryEditorCommands.apply(map, ruleset, draft)) changed()
                error.setText("")
            }
        }).colspan(2).growX().row()
        add("Use player-selected victory conditions".toTextButton().onClick {
            attempt {
                if (ScenarioVictoryEditorCommands.apply(map, ruleset, null)) changed()
                refresh()
            }
        }).colspan(2).growX().row()
        add(error).width(panelWidth - 30f).colspan(2).row()
    }

    /** Отказ сохраняет черновик в форме и прежнее состояние карты. */
    private fun attempt(action: () -> Unit) {
        try { action() } catch (invalid: IllegalArgumentException) {
            error.setText(invalid.message.orEmpty().tr())
        }
    }
}
