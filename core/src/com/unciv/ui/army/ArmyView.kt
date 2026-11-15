package com.unciv.ui.army

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.ArmyManager
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.cityscreen.CityScreen
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.SimplePopup
import com.unciv.ui.popup.SplitTroopPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick

/**
 * A view for displaying an [ArmyInfo] in the UI.
 * Handles rendering of troops or empty slots and interactions.
 */
class ArmyView(
    private val armyInfo: ArmyInfo?,
    private val armyManager: ArmyManager,
    private val screen: BaseScreen,
    private val slotSize: Float = 64f // Размер слотов по умолчанию
) : Table() {
    /** Позволяет принимающему экрану обновить сведения после штатного обновления армии. */
    var onArmyUpdated: (() -> Unit)? = null
    // !! Note here, ArmyInfo is nullable, which is weird, but unfortunately it's the easiest way to handle the bug
    // of libGDX, which goes crazy if you feed it nullable (but not null!) Table-derived class.
    // So visitingArmyView exists always, but there might be no visiting hero ) Crazy, but it works.
    // Need to be corrected if the bug in libGDX fixed

    // Array to store TroopArmyView or null for empty slots
    private val troopViewsArray: Array<TroopArmyView?> =
            Array(armyInfo?.getAllTroops()?.size ?: 0) { null }

    private var selectedTroop: TroopArmyView? = null // Reference to the selected troop

    // Reference to another ArmyView for potential exchanges
    private var exchangeArmyView: ArmyView? = null

    init {
        // Align the table and set padding
        //right().bottom()

        //bottom().left()
        //pad(10f)
        updateView(initialized = false)
    }

    /** Обновляет слоты без повторного уведомления при синхронизации нескольких представлений. */
    fun updateView(initialized: Boolean = true) {
        val selectedIndex = if (!initialized) getSelectedTroopIndex() else null
        selectedTroop = null
        clear()
        val royalCity = screen is CityScreen && screen.game.settings.skin == "Minimal"
        val columns = if (royalCity) {
            val availableWidth = (screen.stage.width / 3f).coerceAtLeast(300f)
                .coerceAtMost(screen.stage.width - 10f)
            (availableWidth / (slotSize + 10f)).toInt().coerceAtLeast(1)
        } else Int.MAX_VALUE
        // Индексы игровых слотов сохраняются при переносах строк.
        armyInfo?.getAllTroops()?.forEachIndexed { index, troop ->
            val troopView = TroopArmyView(troop, this, slotSize = slotSize)
            troopViewsArray[index] = troopView
            add(troopView).size(slotSize).pad(5f)
            if (royalCity && (index + 1) % columns == 0) row()
            if (index == selectedIndex && !troopView.isEmptySlot()) {
                troopView.select()
                selectedTroop = troopView
            }
        }
        pack()
        if (initialized) {
            if (screen is CityScreen) {
                screen.city.cityStats.updateTileStats()
                screen.update(triggeredByArmyView = true)
            }
            onArmyUpdated?.invoke()
        }
    }

    /**
     * Updates a specific slot in the army view.
     * @param index The index of the slot to update.
     * @param troop The new troop to place in the slot, or null for an empty slot.
     */
    fun updateSlot(index: Int, troop: Troop?) {
        if (index < 0 || index >= troopViewsArray.size) return // Check array bounds

        // Remove the current actor from the table
        troopViewsArray[index]?.remove()
        troopViewsArray[index] = null

        if (troop != null) {
            // Create a new TroopArmyView for the troop
            val troopView = TroopArmyView(troop, this, slotSize = slotSize)
            troopViewsArray[index] = troopView
            add(troopView).size(slotSize).pad(5f)
        } else {
            // Add a placeholder image for the empty slot
            val emptySlotImage = ImageGetter.getImage("OtherIcons/Wait")
            add(emptySlotImage).size(slotSize).pad(5f)
        }

        pack() // Update layout
    }

    /**
     * Returns the index of the selected troop in the army.
     * @return The index of the selected troop, or null if no troop is selected.
     */
    internal fun getSelectedTroopIndex(): Int? {
        // Если selectedTroop равен null, значит ничего не выделено
        if (selectedTroop == null) return null

        // Ищем индекс выделенного отряда в массиве
        return troopViewsArray.indexOf(selectedTroop).takeIf { it != -1 }
    }

    /**
     * Deselects all troops in the army.
     */
    fun deselectAllTroops() {
        troopViewsArray.forEach { troopView ->
            troopView?.deselect() // Call deselect for all non-null TroopArmyView instances
        }
        selectedTroop = null

    }

    /**
     * Sets the reference to another ArmyView for exchanges.
     * @param armyView The other ArmyView.
     */
    fun setExchangeArmyView(armyView: ArmyView?) {
        exchangeArmyView = armyView
    }


    /**
     * Checks if the ArmyInfo is null.
     * @return True if the army is not null,  otherwise, false.
     */
    fun isHero(): Boolean {
        return armyInfo != null
    }

    /** Expose screen for child views that need to open popups */
    fun getScreen(): BaseScreen = screen

    /**
     * Handles selection of a troop by index.
     * Ensures only one troop is selected at a time.
     * @param index The index of the troop to select.
     */
    fun selectTroop(index: Int) {
        deselectAllTroops() // Deselect all troops first
        troopViewsArray[index]?.select() // Select the specified troop

    }


    /** Выполняет прежние операции армии, проверяя актуальность городской сцены перед изменением. */
    fun onTroopClicked(clickedTroopView: TroopArmyView, isTroopSplitting: Boolean) {
        fun allowed(): Boolean = screen !is CityScreen ||
                (screen.city.getVisitingHero() === screen.visitingHero &&
                        screen.context.canChangeState() && screen.canCityBeChanged() && screen.isPlayersTurn)
        if (!allowed()) {
            deselectAllTroops()
            exchangeArmyView?.deselectAllTroops()
            return
        }
        val clickedIndex = troopViewsArray.indexOf(clickedTroopView)
        if (clickedIndex == -1) return
        if (selectedTroop == null && exchangeArmyView?.selectedTroop == null) {
            deselectAllTroops()
            if (!clickedTroopView.isEmptySlot()) {
                clickedTroopView.select()
                selectedTroop = clickedTroopView
            }
            return
        }
        if (clickedIndex == getSelectedTroopIndex()) {
            deselectAllTroops()
            return
        }
        val targetArmy = armyInfo ?: return
        val sameArmy = getSelectedTroopIndex() != null
        val sourceView = if (sameArmy) this else exchangeArmyView ?: return
        val sourceIndex = sourceView.getSelectedTroopIndex() ?: return
        val sourceArmy = sourceView.armyInfo ?: return
        val sourceTroopView = sourceView.troopViewsArray[sourceIndex] ?: return
        val sourceTroop = sourceArmy.getTroopAt(sourceIndex)
        val targetTroop = targetArmy.getTroopAt(clickedIndex)
        fun refresh() {
            deselectAllTroops()
            sourceView.deselectAllTroops()
            updateView()
            if (!sameArmy) sourceView.updateView()
        }
        if (isTroopSplitting) {
            SplitTroopPopup(
                screen = screen,
                sourceTroopView = sourceTroopView,
                targetTroopView = clickedTroopView,
                onSplit = { firstPart, _ ->
                    // Диалог мог пережить уход героя или изменение выбранных слотов.
                    if (allowed() && sourceArmy.getTroopAt(sourceIndex) === sourceTroop &&
                            targetArmy.getTroopAt(clickedIndex) === targetTroop
                    ) {
                        armyManager.splitTroop(
                            sourceArmy = sourceArmy,
                            sourceIndex = sourceIndex,
                            targetArmy = targetArmy,
                            targetIndex = clickedIndex,
                            finalFirstTroopCount = firstPart
                        )
                        refresh()
                    }
                }
            ).open()
        } else {
            val success = armyManager.swapOrCombineTroops(
                firstArmy = sourceArmy,
                firstIndex = sourceIndex,
                secondArmy = targetArmy,
                secondIndex = clickedIndex,
                combine = true
            )
            if (!success) println("Army swapping or combining is unsuccessful")
        }
        refresh()
    }

}
