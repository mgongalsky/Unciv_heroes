package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.map.ScenarioStoryEditorCommands
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.translations.tr
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.hero.ScenarioHero
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import java.util.UUID

/** Форма исходного героя; выбор клетки не создаёт игровой юнит и не публикует события. */
class ScenarioHeroEditorPanel(
    private val map: TileMap,
    private val ruleset: Ruleset,
    private val panelWidth: Float,
    private val changed: () -> Unit
) : Table(BaseScreen.skin) {
    private var position: CityPosition? = null
    private var originalId: String? = null
    private val error = "".toLabel(Color.SALMON).apply { wrap = true }
    private data class Slot(val id: String, val label: String) {
        override fun toString() = label
    }

    init { refresh() }

    /** Неприменённый ввод отбрасывается при выборе другого размещения. */
    fun select(value: CityPosition) {
        position = value
        refresh()
    }

    private fun refresh() {
        clear()
        top()
        defaults().pad(5f).growX()
        add("Click a tile to place or select a hero".toLabel().apply { wrap = true })
            .width(panelWidth - 30f).row()
        for (hero in map.scenarioHeroes) {
            add("${hero.name} (${hero.id})".toTextButton().onClick { select(hero.position) }).row()
        }
        val pickedPosition = position ?: return
        val existing = map.scenarioHeroes.firstOrNull { it.position == pickedPosition }
        originalId = existing?.id
        val draft = existing?.copy() ?: ScenarioHero(
            id = UUID.randomUUID().toString(), position = pickedPosition,
            ownerSlotId = map.scenarioPlayerSlots.firstOrNull()?.id.orEmpty()
        )
        add("[${pickedPosition.x}, ${pickedPosition.y}]".toLabel()).row()
        fun field(caption: String, value: String): TextField {
            add(caption.toLabel()).row()
            val input = TextField(value, skin)
            add(input).width(panelWidth - 30f).row()
            return input
        }

        val id = field("Hero ID", draft.id)
        val heroName = field("Name", draft.name)
        add("Unit type".toLabel()).row()
        val units = SelectBox<String>(skin).apply {
            val available = ruleset.units.values.filter {
                ruleset.unitTypes[it.unitType]?.isLandUnit() == true && !it.isCivilian()
            }.map { it.name }
            setItems(*(available + draft.unitName).distinct().sorted().toTypedArray())
            selected = draft.unitName
        }
        add(units).row()
        add("Player slot".toLabel()).row()
        val slots = SelectBox<Slot>(skin).apply {
            val values = map.scenarioPlayerSlots.map {
                Slot(
                    it.id,
                    "${it.name.ifBlank { it.id }} (${it.id})"
                )
            }
            setItems(*values.toTypedArray())
            values.firstOrNull { it.id == draft.ownerSlotId }?.let { selected = it }
        }
        add(slots).row()
        add("Apply hero".toTextButton().apply { name = "scenario-hero-apply" }.onClick {
            attempt {
                draft.id = id.text.trim()
                draft.name = heroName.text.trim()
                draft.unitName = units.selected.orEmpty()
                draft.ownerSlotId = slots.selected?.id.orEmpty()
                val heroes =
                        map.scenarioHeroes.filter { it.id != originalId }.map { it.copy() } + draft
                if (ScenarioStoryEditorCommands.apply(map, ruleset, heroes, map.story)) changed()
                error.setText("")
                refresh()
            }
        }).row()
        if (existing != null) add("Delete hero".toTextButton().onClick {
            attempt {
                if (ScenarioStoryEditorCommands.apply(
                            map, ruleset,
                            map.scenarioHeroes.filter { it.id != originalId }, map.story
                        )
                ) changed()
                error.setText("")
                refresh()
            }
        }).row()
        add(error).width(panelWidth - 30f).row()
    }

    private fun attempt(action: () -> Unit) {
        try { action() } catch (invalid: IllegalArgumentException) {
            error.setText(invalid.message.orEmpty().tr())
        }
    }
}
