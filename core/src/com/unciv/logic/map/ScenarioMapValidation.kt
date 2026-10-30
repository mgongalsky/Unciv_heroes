package com.unciv.logic.map

import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.city.ScenarioCityValidation
import com.unciv.models.ruleset.Ruleset

/** Validation at the map boundary; never creates runtime cities or initializes a game. */
object ScenarioMapValidation {
    fun validateStructure(map: TileMap) {
        ScenarioCityValidation.validate(map.scenarioCities, map.scenarioPlayerSlots,
            map.values.mapTo(HashSet()) { CityPosition(it.position.x, it.position.y) })
    }

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
