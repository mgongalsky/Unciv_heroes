package com.unciv.ui.playground

import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.unciv.Constants
import com.unciv.logic.civilization.PlayerType
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.metadata.Player
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.city.ScenarioPlayerSlot
import com.unciv.ui.newgamescreen.IPreviousScreen
import com.unciv.ui.newgamescreen.PlayerPickerTable
import com.unciv.ui.pickerscreens.PickerScreen
import com.unciv.ui.utils.BaseScreen

/** Минимальный родитель настоящего PlayerPickerTable с двумя участниками и обычными правилами. */
private class ScenarioPlayerPickerScreen : PickerScreen(), IPreviousScreen {
    override val ruleset = RulesetCache.getVanillaRuleset()
    override val gameSetupInfo = GameSetupInfo().apply {
        gameParameters.players = arrayListOf(
            Player("Rome").apply { playerType = PlayerType.Human }, Player("Greece"))
    }
}

/** Проверяет реальные селекторы слотов: автоподбор, обмен назначениями, ручное отсутствие слота и наблюдателя. */
fun createScenarioPlayerPickerPreview(): BaseScreen {
    val screen = ScenarioPlayerPickerScreen()
    val players = screen.gameSetupInfo.gameParameters.players
    val picker = PlayerPickerTable(screen, screen.gameSetupInfo.gameParameters)
    picker.scenarioSlots = listOf(ScenarioPlayerSlot("first", "Player 1"), ScenarioPlayerSlot("second", "Player 2"))
    picker.update()
    check(players.map { it.scenarioSlotId } == listOf("first", "second"))
    /** Меняет выбранный элемент виджета, запуская настоящий onChange таблицы участников. */
    fun select(player: Int, choice: Int) {
        picker.findActor<SelectBox<Any>>("scenario-slot-$player").selectedIndex = choice
    }
    select(0, 2)
    check(players.map { it.scenarioSlotId } == listOf("second", "first")) { "Manual selection must swap occupied slots" }
    picker.update()
    check(players.map { it.scenarioSlotId } == listOf("second", "first"))
    select(0, 0)
    picker.update()
    check(players.first().scenarioSlotId.isEmpty()) { "Manual No slot must survive refresh" }
    select(0, 2)
    players.first().chosenCiv = Constants.spectator
    picker.update()
    check(players.first().scenarioSlotId.isEmpty()) { "Spectators must not receive scenario slots" }
    players.first().chosenCiv = "Rome"
    picker.update()
    check(players.map { it.scenarioSlotId } == listOf("second", "first"))
    screen.topTable.add(picker).grow()
    return screen
}
