package com.unciv.logic.map

import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.application.scenario.ValidateScenarioUseCase
import com.unciv.models.ruleset.Ruleset

/** Проверки на границе карты: не создают игровые города и не запускают основание или GameInfo. */
object ScenarioMapValidation {
    /** Передаёт чистым правилам реальные координаты карты для проверки ссылок и территорий. */
    fun validateStructure(map: TileMap) {
        ValidateScenarioUseCase.execute(map.scenarioCities, map.scenarioPlayerSlots,
            map.values.mapTo(HashSet()) { CityPosition(it.position.x, it.position.y) })
    }

    /** Собирает все отсутствующие определения; данные карты не удаляются даже при смене ruleset. */
    fun definitionErrors(map: TileMap, ruleset: Ruleset): List<String> {
        val errors = arrayListOf<String>()
        for (city in map.scenarioCities) {
            for (building in city.buildings) if (building !in ruleset.buildings)
                errors.add("Scenario city ${city.id}: unknown building [$building]")
            for (troop in city.garrison) if (troop.unitName !in ruleset.units)
                errors.add("Scenario city ${city.id}: unknown garrison unit [${troop.unitName}]")
        }
        return errors
    }
}
