package com.unciv.logic.map

import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.application.scenario.ValidateScenarioUseCase
import com.unciv.models.ruleset.Ruleset

/** Проверки на границе карты: не создают игровые города и не запускают основание или GameInfo. */
object ScenarioMapValidation {
    /** Проверяет координаты и ссылки всех описаний до публикации или записи карты. */
    fun validateStructure(map: TileMap) {
        val positions = map.values.mapTo(HashSet()) { CityPosition(it.position.x, it.position.y) }
        ValidateScenarioUseCase.execute(
            map.scenarioCities, map.scenarioPlayerSlots, positions, map.scenarioVictorySettings
        )
        com.unciv.pure.application.scenario.PrepareScenarioHeroesUseCase.validateDescriptions(
            map.scenarioHeroes, map.scenarioPlayerSlots, positions
        )
        map.story?.let {
            com.unciv.pure.application.story.ValidateStoryScenario.execute(
                it, map.scenarioHeroes.mapTo(HashSet()) { hero -> hero.id },
                map.scenarioPlayerSlots.mapTo(HashSet()) { slot -> slot.id },
                map.scenarioCities.mapTo(HashSet()) { city -> city.id }, positions
            )
        }
    }

    /** Собирает ошибки определений без игровой инициализации и изменения авторских данных. */
    fun definitionErrors(map: TileMap, ruleset: Ruleset): List<String> {
        val errors = arrayListOf<String>()
        for (city in map.scenarioCities) {
            for (building in city.buildings) if (building !in ruleset.buildings)
                errors.add("Scenario city ${city.id}: unknown building [$building]")
            for (troop in city.garrison) if (troop.unitName !in ruleset.units)
                errors.add("Scenario city ${city.id}: unknown garrison unit [${troop.unitName}]")
        }
        fun checkArmy(unitName: String, context: String) {
            val unit = ruleset.units[unitName]
            if (unit == null) errors.add("$context: unknown unit [$unitName]")
            else if (ruleset.unitTypes[unit.unitType]?.isLandUnit() != true || unit.isCivilian())
                errors.add("$context: a land military unit is required")
        }
        for (hero in map.scenarioHeroes) checkArmy(hero.unitName, "Scenario hero ${hero.id}")
        fun checkEffects(effects: List<com.unciv.pure.domain.story.StoryEffect>, context: String) {
            for (effect in effects) {
                if (effect.kind == com.unciv.pure.domain.story.StoryEffectKind.SpawnArmy ||
                        effect.kind == com.unciv.pure.domain.story.StoryEffectKind.SetGarrison
                )
                    checkArmy(effect.unitName, context)
            }
        }
        for (trigger in map.story?.triggers.orEmpty())
            checkEffects(trigger.effects, "Story trigger ${trigger.id}")
        for (dialogue in map.story?.dialogues.orEmpty())
            for (node in dialogue.nodes)
                for (choice in node.choices)
                    checkEffects(
                        choice.effects,
                        "Story choice ${dialogue.id}/${node.id}/${choice.id}"
                    )
        return errors
    }
}
