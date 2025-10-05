package com.unciv.clean.application.usecases

import com.unciv.clean.application.ports.RulesetProvider
import com.unciv.logic.GameInfo
import com.unciv.logic.BackwardCompatibility
import com.unciv.models.ruleset.unique.UniqueType

class InitializeSessionUseCase(
    private val rulesets: RulesetProvider
) {
    fun execute(game: GameInfo) {
        val missing = rulesets.missingMods(game.gameParameters)
        if (missing.isNotEmpty()) {
            throw MissingModsException(missing.joinToString())
        }
        game.ruleSet = rulesets.loadFor(game.gameParameters)

        with(BackwardCompatibility) {
            game.barbarians.migrateBarbarianCamps()
            game.removeMissingModReferences()
            game.convertOldGameSpeed()
        }

        for (civ in game.civilizations) civ.gameInfo = game
        for (civ in game.civilizations) civ.setNationTransient()
        for (civ in game.civilizations) {
            for (dip in civ.diplomacy.values) {
                dip.civInfo = civ
                dip.updateHasOpenBorders()
            }
        }
        game.tileMap.gameInfo = game
        game.tileMap.setTransients(game.ruleSet)

        if (game.currentPlayer == "") game.currentPlayer = game.civilizations.first { it.isPlayerCivilization() }.civName
        game.currentPlayerCiv = game.getCivilization(game.currentPlayer)

        game.difficultyObject = game.ruleSet.difficulties[game.difficulty]!!
        game.speed = game.ruleSet.speeds[game.gameParameters.speed]!!

        for (r in game.religions.values) r.setTransients(game)

        for (civ in game.civilizations) civ.setTransients()
        for (civ in game.civilizations) {
            civ.thingsToFocusOnForVictory = civ.getPreferredVictoryTypeObject()?.getThingsToFocus(civ) ?: setOf()
        }
        game.tileMap.setNeutralTransients()

        with(BackwardCompatibility) { game.convertFortify() }

        val seq = sequence {
            yieldAll(game.civilizations.filter { it.isCityState() })
            yieldAll(game.civilizations.filter { !it.isCityState() })
        }
        for (civ in seq) {
            for (unit in civ.getCivUnits()) unit.updateVisibleTiles(false)
            civ.updateSightAndResources()
            civ.initialSetCitiesConnectedToCapitalTransients()
            for (city in civ.cities) city.cityStats.updateCityHappiness(city.cityConstructions.getStats())
            for (city in civ.cities) {
                if (city.cityConstructions.constructionQueue.isEmpty())
                    city.cityConstructions.chooseNextConstruction()
                if (!game.ruleSet.tileResources.containsKey(city.demandedResource))
                    city.demandedResource = ""
                city.cityStats.update()
            }
            if (civ.hasEverOwnedOriginalCapital == null) {
                civ.hasEverOwnedOriginalCapital = civ.cities.any { it.isOriginalCapital }
            }
        }

        game.spaceResources.clear()
        game.spaceResources.addAll(game.ruleSet.buildings.values.filter { it.hasUnique(UniqueType.SpaceshipPart) }
            .flatMap { it.getResourceRequirements().keys })
        game.spaceResources.addAll(game.ruleSet.victories.values.flatMap { it.requiredSpaceshipParts })

        game.barbarians.setTransients(game)
        with(BackwardCompatibility) { game.guaranteeUnitPromotions() }
    }
}

class MissingModsException(msg: String) : RuntimeException(msg)
