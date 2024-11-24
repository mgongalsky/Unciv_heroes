package com.unciv.ui.army

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.army.ArmyInfo
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.extensions.onClick

/**
 * A view for displaying an [ArmyInfo] in the UI.
 * Handles rendering of troops or empty slots and interactions.
 */
class ArmyView(private val armyInfo: ArmyInfo) : Table() {

    /** Callback for when a troop slot is clicked. Provides the index of the clicked slot. */
    var onTroopClick: ((Int) -> Unit)? = null

    init {
        // From CityContructionsTable
        bottom().left()
        pad(10f)
        // garrisonWidget.height(stageHeight / 8)
        //val tableHeight = stage.height / 8f
        //height = tableHeight
        updateView()
    }

    /**
     * Updates the view to match the current state of the [ArmyInfo].
     */
    fun updateView() {
        clear() // Remove existing actors from the table

        // Access slots directly using `armyInfo.getAllTroops()`
        armyInfo.getAllTroops().forEachIndexed { index, troop ->
            if (troop != null) {
                // Если есть юнит, создаем его представление
                val troopView = TroopArmyView(troop)
                add(troopView).size(64f).pad(5f)
                troopView.updateView()
            } else {
                // Если слота нет, добавляем пустое изображение
                val emptySlotImage =
                        ImageGetter.getImage("OtherIcons/EmptySlot") // Укажите правильный путь
                add(emptySlotImage).size(64f).pad(5f)
            }
            // Ensure layout is updated
            pack()
        }
    }
}
