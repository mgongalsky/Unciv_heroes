package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.map.ScenarioCityEditorCommands
import com.unciv.logic.map.ScenarioMapValidation
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.translations.tr
import com.unciv.pure.domain.city.*
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.UncivTextField
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import java.util.UUID

/** Editor-only presentation of scenario data, with no runtime city or civilization. */
class ScenarioCityEditorPanel(
    private val map: TileMap,
    private val ruleset: Ruleset,
    private val panelWidth: Float,
    private val changed: () -> Unit
) : Table(BaseScreen.skin) {
    private var draft: ScenarioCity? = null
    private val form = Table(skin)
    private val error = "".toLabel(Color.SALMON).apply { setWrap(true) }
    private data class OwnerChoice(val id: String, val label: String) {
        override fun toString() = label
    }

    init {
        top()
        defaults().pad(5f).growX()
        add("Click a tile to place or select a city".toLabel().apply { setWrap(true) }).width(panelWidth - 30f).row()
        add(form).row()
        add(error).width(panelWidth - 30f).row()
        refresh()
    }

    fun select(position: CityPosition) {
        draft = map.scenarioCities.firstOrNull { it.position == position }?.deepCopy()
            ?: ScenarioCity(id = UUID.randomUUID().toString(), position = position,
                name = "City", typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Default")
        refresh()
    }

    fun refresh() {
        form.clear()
        form.defaults().pad(4f).left()
        error.setText(ScenarioMapValidation.definitionErrors(map, ruleset).joinToString("\n"))
        val slotName = UncivTextField.create("Player slot name", "")
        form.add(slotName).growX()
        form.add("Add player slot".toTextButton().onClick {
            attempt {
                require(slotName.text.isNotBlank()) { "Enter a player slot name" }
                capture()
                if (ScenarioCityEditorCommands.putSlot(map, ScenarioPlayerSlot(
                        UUID.randomUUID().toString(), slotName.text.trim()))) changed()
                refresh()
            }
        }).row()
        val city = draft ?: return
        form.add("Position: [${city.position.x.toInt()}, ${city.position.y.toInt()}]".toLabel()).colspan(2).row()
        val name = UncivTextField.create("City name", city.name).apply { this.name = "scenario-city-name" }
        val population = UncivTextField.create("Population", city.population.toString())
        val food = UncivTextField.create("Stored food", city.foodStored.toString())
        fun field(label: String, widget: com.badlogic.gdx.scenes.scene2d.Actor) {
            form.add(label.toLabel())
            form.add(widget).width(panelWidth * 0.5f).growX().row()
        }
        field("Name", name)
        val owners = listOf(OwnerChoice("", "Neutral".tr())) + map.scenarioPlayerSlots.map {
            OwnerChoice(it.id, it.name.ifBlank { it.id })
        }
        val owner = SelectBox<OwnerChoice>(skin).apply {
            setItems(*owners.toTypedArray())
            selected = owners.firstOrNull { it.id == city.ownerSlotId } ?: owners.first()
        }
        field("Owner", owner)
        field("Population", population)
        field("Stored food", food)
        val capital = CheckBox("Capital".tr(), skin).apply {
            this.name = "scenario-city-capital"
            isChecked = city.owner == ScenarioCityOwner.PlayerSlot && (city.isCapital || city.isOriginalCapital)
        }
        form.add(capital).colspan(2).row()
        val fromOwner = CheckBox("Type from starting player".tr(), skin).apply {
            isChecked = city.typeMode == ScenarioCityTypeMode.FromStartingPlayer
        }
        form.add(fromOwner).colspan(2).row()
        val fixedType = UncivTextField.create("Fixed city type", city.fixedType)
        field("City type", fixedType)
        form.add("Territory: [${city.territory.size.coerceAtLeast(1)}] tiles".toLabel()).colspan(2).row()
        form.add("Buildings".toLabel()).colspan(2).row()
        for (building in city.buildings.toList()) {
            form.add(building.toLabel()).growX()
            form.add("Remove".toTextButton().onClick { capture(); city.buildings.remove(building); refresh() }).row()
        }
        if (ruleset.buildings.isNotEmpty()) {
            val building = SelectBox<String>(skin).apply { setItems(*ruleset.buildings.keys.sorted().toTypedArray()) }
            form.add(building).width(panelWidth * 0.4f).growX()
            form.add("Add building".toTextButton().onClick {
                capture()
                if (building.selected !in city.buildings) city.buildings.add(building.selected)
                refresh()
            }).row()
        }
        form.add("Garrison".toLabel()).colspan(2).row()
        for ((index, troop) in city.garrison.toList().withIndex()) {
            form.add("[${troop.unitName}] × [${troop.amount}]".toLabel()).growX()
            form.add("Remove".toTextButton().onClick { capture(); city.garrison.removeAt(index); refresh() }).row()
        }
        if (ruleset.units.isNotEmpty()) {
            val unit = SelectBox<String>(skin).apply { setItems(*ruleset.units.keys.sorted().toTypedArray()) }
            field("Unit", unit)
            val amount = UncivTextField.create("Amount", "5")
            form.add(amount).growX()
            form.add("Add troops".toTextButton().onClick {
                attempt {
                    capture()
                    val count = amount.text.toIntOrNull() ?: kotlin.error("Enter a positive troop count")
                    require(count > 0) { "Enter a positive troop count" }
                    city.garrison.add(ScenarioCityTroop(unit.selected, count))
                    refresh()
                }
            }).row()
        }
        val apply = "Apply city".toTextButton().apply { this.name = "scenario-city-apply" }
        form.add(apply).growX()
        form.add("Delete city".toTextButton().onClick {
            if (ScenarioCityEditorCommands.removeCity(map, city.id)) changed()
            draft = null
            refresh()
        }).row()
        form.add("Delete selected player slot".toTextButton().onClick {
            attempt {
                require(owner.selected.id.isNotEmpty()) { "Select a player slot first" }
                if (ScenarioCityEditorCommands.removeSlot(map, owner.selected.id)) changed()
                refresh()
            }
        }).colspan(2).row()

        // Preserve the draft while editing its building and troop lists.
        captureDraft = {
            city.name = name.text.trim()
            population.text.toIntOrNull()?.let { city.population = it }
            food.text.toIntOrNull()?.let { city.foodStored = it }
            city.ownerSlotId = owner.selected.id
            city.owner = if (owner.selected.id.isEmpty()) ScenarioCityOwner.Neutral else ScenarioCityOwner.PlayerSlot
            city.isCapital = city.owner == ScenarioCityOwner.PlayerSlot && capital.isChecked
            city.isOriginalCapital = city.isCapital
            city.typeMode = if (fromOwner.isChecked) ScenarioCityTypeMode.FromStartingPlayer else ScenarioCityTypeMode.Fixed
            city.fixedType = if (fromOwner.isChecked) "" else fixedType.text.trim()
        }
        apply.onClick {
            attempt {
                require(population.text.toIntOrNull() != null && food.text.toIntOrNull() != null) { "Enter whole numbers for population and food" }
                capture()
                if (ScenarioCityEditorCommands.putCity(map, ruleset, city)) changed()
                refresh()
            }
        }
    }

    private var captureDraft: () -> Unit = { }
    private fun capture() = captureDraft()
    private fun attempt(action: () -> Unit) {
        try { action() } catch (invalid: IllegalArgumentException) {
            error.setText(invalid.message.orEmpty().tr())
        } catch (invalid: IllegalStateException) {
            error.setText(invalid.message.orEmpty().tr())
        }
    }
}
