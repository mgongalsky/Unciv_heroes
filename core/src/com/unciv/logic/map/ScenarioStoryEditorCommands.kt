package com.unciv.logic.map

import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.scenario.PrepareScenarioHeroesUseCase
import com.unciv.pure.application.story.ValidateStoryScenario
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.hero.ScenarioHero
import com.unciv.pure.domain.story.StoryEffectKind
import com.unciv.pure.domain.story.StoryScenario

/** Проверяет общий черновик до публикации: удаление героя не оставляет висячих сюжетных ссылок. */
object ScenarioStoryEditorCommands {
    fun apply(
        map: TileMap,
        ruleset: Ruleset,
        heroes: List<ScenarioHero>,
        story: StoryScenario?
    ): Boolean {
        val heroCopies = heroes.map { it.copy() }
        val storyCopy = story?.deepCopy()
        val tiles = map.values.associateBy { CityPosition(it.position.x, it.position.y) }
        PrepareScenarioHeroesUseCase.validateDescriptions(
            heroCopies,
            map.scenarioPlayerSlots,
            tiles.keys
        )
        for (hero in heroCopies) {
            val unit = requireNotNull(ruleset.units[hero.unitName]) {
                "Scenario hero ${hero.id}: unknown unit ${hero.unitName}"
            }
            // В редакторе BaseUnit ещё может не иметь игровой обратной ссылки на ruleset.
            require(ruleset.unitTypes[unit.unitType]?.isLandUnit() == true && !unit.isCivilian()) {
                "Scenario hero ${hero.id}: a land military unit is required"
            }
            val tile = tiles.getValue(hero.position)
            require(tile.isLand && tile.militaryUnit == null) {
                "Scenario hero ${hero.id}: choose an unoccupied land tile"
            }
        }
        if (storyCopy != null) {
            ValidateStoryScenario.execute(
                storyCopy,
                heroCopies.mapTo(HashSet()) { it.id },
                map.scenarioPlayerSlots.mapTo(HashSet()) { it.id },
                map.scenarioCities.mapTo(HashSet()) { it.id }, tiles.keys
            )
            val effects = storyCopy.triggers.flatMap { it.effects } +
                    storyCopy.dialogues.flatMap { it.nodes }.flatMap { it.choices }
                        .flatMap { it.effects }
            for (effect in effects) {
                if (effect.kind == StoryEffectKind.SpawnArmy || effect.kind == StoryEffectKind.SetGarrison) {
                    val unit = requireNotNull(ruleset.units[effect.unitName]) {
                        "Unknown story unit ${effect.unitName}"
                    }
                    require(ruleset.unitTypes[unit.unitType]?.isLandUnit() == true && !unit.isCivilian()) {
                        "Story armies require a land military unit"
                    }
                }
            }
        }
        if (map.scenarioHeroes == heroCopies && map.story == storyCopy) return false
        // Все проверки завершены; опубликованные коллекции не разделяются с формой редактора.
        map.scenarioHeroes.clear()
        map.scenarioHeroes.addAll(heroCopies)
        map.story = storyCopy
        return true
    }
}
