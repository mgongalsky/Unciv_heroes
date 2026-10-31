package com.unciv.ui.playground

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.unciv.logic.MapSaver
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.GameParameters
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.city.*
import com.unciv.pure.domain.scenario.*
import com.unciv.pure.domain.victory.CaptureCityObjective
import com.unciv.ui.newgamescreen.NewGameScreen
import com.unciv.ui.utils.BaseScreen
import java.io.File

/** Проверяет реальные галочки меню и пересоздание экрана без запуска игры и записи пользовательских настроек. */
fun createScenarioVictorySelectionPreview(): BaseScreen {
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = TileMap(3, ruleset)
    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one", "First player"))
    map.scenarioCities.add(ScenarioCity(id = "fort", name = "Fort", typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle"))
    map.scenarioVictorySettings = ScenarioVictorySettings(
        rules = arrayListOf(
            ScenarioVictoryRule("CaptureCity", ScenarioVictoryMode.Required),
            ScenarioVictoryRule("Domination", ScenarioVictoryMode.Forbidden),
            ScenarioVictoryRule("Scientific", ScenarioVictoryMode.Optional, false)
        ),
        captureObjectives = arrayListOf(CaptureCityObjective("fort", arrayListOf("one")))
    )
    val file = File.createTempFile("unciv-victory-menu-", ".json")
    val screen = NewGameScreen(GameSetupInfo())
    try {
        file.writeText(MapSaver.mapToSavedString(map))
        screen.gameSetupInfo.mapFile = FileHandle(file)
        screen.gameSetupInfo.mapParameters.name = "Victory test"
        screen.gameSetupInfo.gameParameters.apply {
            victorySelectionInitialized = false
            victoryTypes.clear()
        }
        screen.updateTables()
        val capture = screen.stage.root.findActor<CheckBox>("CaptureCity")
        val domination = screen.stage.root.findActor<CheckBox>("Domination")
        val science = screen.stage.root.findActor<CheckBox>("Scientific")
        check(capture.isChecked && capture.isDisabled)
        check(!domination.isChecked && domination.isDisabled)
        check(!science.isChecked && !science.isDisabled) { "Optional default was ignored" }
        science.isChecked = true
        screen.updateTables()
        check("Scientific" in screen.gameSetupInfo.gameParameters.victoryTypes) { "Optional choice was lost on refresh" }
        screen.gameSetupInfo.mapParameters.name = ""
        screen.gameSetupInfo.mapFile = null
        screen.gameSetupInfo.gameParameters.victoryTypes.clear()
        screen.gameSetupInfo.gameParameters.victorySelectionInitialized = true
        screen.updateTables()
        check(screen.gameSetupInfo.gameParameters.victoryTypes.isEmpty()) { "Empty choice was replaced by defaults" }
        val recreated = screen.recreate() as NewGameScreen
        screen.dispose()
        check(recreated.gameSetupInfo.gameParameters.victoryTypes.isEmpty()) { "Recreation enabled all victories" }
        return recreated
    } catch (failure: Throwable) {
        screen.dispose()
        throw failure
    } finally {
        file.delete()
    }
}
