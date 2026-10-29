package com.unciv.logic.city

import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.pure.domain.city.CityOwnership

/** World effects for acquisition, separate from registry membership and conquest. */
object NeutralCityOwnership {
    fun neutralize(city: CityInfo) {
        val game = city.gameInfo
        val oldOwner = requireNotNull(city.ownerOrNull) { "City ${city.id} has no player owner" }
        require(oldOwner.gameInfo === game && game.civilizations.any { it === oldOwner }) {
            "Previous owner does not belong to the city world"
        }
        check(game.cityRegistry.byId(city.state.id) === city) { "City ${city.id} is not registered" }
        CitySaveValidation.validateDefinitions(listOf(city), game.ruleSet)
        val wasCapital = oldOwner.getCapital() === city

        game.cityRegistry.neutralize(city)
        city.previousOwner = oldOwner.civName
        city.turnAcquired = game.turns
        city.setTransients()
        if (wasCapital && oldOwner.cities.isNotEmpty()) oldOwner.moveCapitalToNextLargest()
        oldOwner.citiesConnectedToCapitalToMediums = emptyMap()
        oldOwner.initialSetCitiesConnectedToCapitalTransients()
        oldOwner.updateSightAndResources()
        for (remainingCity in oldOwner.cities) remainingCity.cityStats.update()
        oldOwner.updateStatsForNextTurn()
        for (other in game.civilizations) {
            if (other === oldOwner || other.isBarbarian() || other.isSpectator()) continue
            oldOwner.updateProximity(other, other.updateProximity(oldOwner))
        }
    }

    fun acquireUndefended(city: CityInfo, newOwner: CivilizationInfo) {
        val game = city.gameInfo
        require(city.state.ownership == CityOwnership.Neutral) { "City ${city.id} is not neutral" }
        require(newOwner.gameInfo === game && game.civilizations.any { it === newOwner }) {
            "New owner does not belong to the city world"
        }
        check(game.cityRegistry.byId(city.state.id) === city) { "City ${city.id} is not registered" }
        require(city.state.garrison?.getAllTroops()?.any { it != null && it.currentAmount > 0 } != true) {
            "City ${city.id} still has a defending garrison"
        }
        CitySaveValidation.validateDefinitions(listOf(city), game.ruleSet)
        val existingCapital = newOwner.getCapital()


        game.cityRegistry.transfer(city, newOwner)
        city.previousOwner = ""
        city.turnAcquired = game.turns
        city.hasJustBeenConquered = false
        city.isPuppet = false
        city.isBeingRazed = false
        city.removeFlag(CityFlags.Resistance)
        city.setTransients()
        if (existingCapital == null && !city.isCapital()) newOwner.moveCapitalTo(city)

        newOwner.updateSightAndResources()
        newOwner.initialSetCitiesConnectedToCapitalTransients()
        city.cityStats.update()
        newOwner.updateStatsForNextTurn()
        for (other in game.civilizations) {
            if (other === newOwner || other.isBarbarian() || other.isSpectator()) continue
            newOwner.updateProximity(other, other.updateProximity(newOwner))
        }
    }
}
