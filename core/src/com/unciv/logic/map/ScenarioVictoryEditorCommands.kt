package com.unciv.logic.map

import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.scenario.ValidateScenarioUseCase
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.scenario.ScenarioVictorySettings

/** Применяет проверенный черновик побед целиком, не оставляя частично изменённых правил или ссылок. */
object ScenarioVictoryEditorCommands {
    fun apply(map: TileMap, ruleset: Ruleset, draft: ScenarioVictorySettings?): Boolean {
        val copy = draft?.deepCopy()
        ValidateScenarioUseCase.execute(
            map.scenarioCities, map.scenarioPlayerSlots,
            map.values.mapTo(HashSet()) { CityPosition(it.position.x, it.position.y) }, copy
        )
        // Редактор не удаляет настройки неизвестного типа молча: автор должен исправить их явно.
        for (rule in copy?.rules.orEmpty()) require(rule.victoryType in ruleset.victories) {
            "Unknown victory type ${rule.victoryType}"
        }
        if (map.scenarioVictorySettings == copy) return false
        map.scenarioVictorySettings = copy
        return true
    }
}
