package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.map.ScenarioCityEditorCommands
import com.unciv.models.translations.tr
import com.unciv.pure.domain.city.ScenarioPlayerSlot
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.UncivTextField
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import java.util.UUID

/** Инструмент Edit → Players для слотов карты; нации и аккаунты назначаются участникам позже, при запуске. */
class MapEditorPlayersTab(
    private val editor: MapEditorScreen,
    private val editTab: MapEditorEditTab
) : Table(BaseScreen.skin), TabbedPager.IPageExtensions, IMapEditorEditSubTabs {
    private val error = "".toLabel(Color.SALMON).apply { setWrap(true) }

    init { name = "scenario-players-tab" }
    override fun isDisabled() = false

    /** Отключает кисть карты на время работы с участниками и показывает актуальный список слотов. */
    override fun activated(index: Int, caption: String, pager: TabbedPager) {
        editTab.setCityEditing(true)
        editor.hideSelection()
        refresh()
    }

    /** Возвращает обычный режим редактирования карты. */
    override fun deactivated(index: Int, caption: String, pager: TabbedPager) {
        editTab.setCityEditing(false)
    }

    /** Пересоздаёт форму; добавление, переименование и удаление применяются отдельными командами. */
    private fun refresh() {
        clear()
        top()
        defaults().pad(5f).growX()
        add("Players".toLabel()).colspan(3).row()
        val newName = UncivTextField.create("Player slot name").apply { name = "scenario-player-slot-name" }
        add(newName).width(editor.getToolsWidth() * 0.45f).colspan(2)
        add("Add player slot".toTextButton().apply { name = "scenario-player-add" }.onClick {
            attempt {
                require(newName.text.isNotBlank()) { "Enter a player slot name" }
                // Имя можно менять; UUID остаётся стабильной ссылкой для описаний городов.
                ScenarioCityEditorCommands.putSlot(editor.tileMap,
                    ScenarioPlayerSlot(UUID.randomUUID().toString(), newName.text.trim()))
                changed()
            }
        }).row()
        if (editor.tileMap.scenarioPlayerSlots.isEmpty())
            add("No players. Cities can be neutral.".toLabel().apply { setWrap(true) })
                .width(editor.getToolsWidth() - 30f).colspan(3).row()
        for ((index, slot) in editor.tileMap.scenarioPlayerSlots.withIndex()) {
            val field = UncivTextField.create("Player slot name", slot.name).apply {
                name = "scenario-player-name-$index"
            }
            add(field).width(editor.getToolsWidth() * 0.45f)
            add("Rename".toTextButton().apply { name = "scenario-player-rename-$index" }.onClick {
                attempt {
                    require(field.text.isNotBlank()) { "Enter a player slot name" }
                    if (ScenarioCityEditorCommands.putSlot(editor.tileMap, slot.copy(name = field.text.trim()))) changed()
                }
            })
            add("Delete".toTextButton().apply { name = "scenario-player-delete-$index" }.onClick {
                attempt {
                    // Сначала нужно переназначить города: удаление слота не должно молча менять их владельцев.
                    require(editor.tileMap.scenarioCities.none { it.ownerSlotId == slot.id &&
                        it.owner == com.unciv.pure.domain.city.ScenarioCityOwner.PlayerSlot }) {
                        "Assign this player's cities to Neutral or another player before deleting the player"
                    }
                    if (ScenarioCityEditorCommands.removeSlot(editor.tileMap, slot.id)) changed()
                }
            }).row()
        }
        add(error).width(editor.getToolsWidth() - 30f).colspan(3).row()
    }

    /** Обновляет состояние сохранения карты и подписи городов после действительно применённой команды. */
    private fun changed() {
        editor.isDirty = true
        editor.mapHolder.refreshScenarioCityPreviews()
        error.setText("")
        refresh()
    }

    /** Показывает переводимую ошибку проверки в форме, сохраняя возможность исправить ввод. */
    private fun attempt(action: () -> Unit) {
        try { action() } catch (invalid: IllegalArgumentException) {
            error.setText(invalid.message.orEmpty().tr())
        }
    }
}
