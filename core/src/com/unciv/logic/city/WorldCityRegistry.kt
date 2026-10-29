package com.unciv.logic.city

import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.pure.domain.city.CityId
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.city.CityRegistry

/** Runtime membership with legacy containers used only until their civilization is attached. */
class WorldCityRegistry(private val game: GameInfo) {
    private val registry = CityRegistry<CityInfo> { it.state }
    private val attached = ArrayList<CivilizationInfo>()

    fun attachCivilizations() {
        for (civ in game.civilizations) attach(civ)
    }

    private fun attach(civ: CivilizationInfo) {
        if (attached.any { it === civ }) return
        val incoming = civ.cities.toList()
        val previousOwners = incoming.map { it.state.ownership }
        try {
            for (city in incoming) city.state.ownership = CityOwnership.Player(civ.civName)
            registry.replaceAll(registry.all() + incoming)
        } catch (failure: Exception) {
            incoming.forEachIndexed { index, city -> city.state.ownership = previousOwners[index] }
            throw failure
        }
        for (city in incoming) city.civInfo = civ
        attached.add(civ)
        civ.attachCityRegistry(this)
    }

    /** Validate the complete save before publishing membership or binding any city. */
    internal fun restore(cities: List<CityInfo>) {
        check(attached.isEmpty() && registry.all().isEmpty()) { "City registry is already attached" }
        require(game.civilizations.map { it.civName }.distinct().size == game.civilizations.size) {
            "Duplicate civilization names in city save"
        }
        val owners = game.civilizations.associateBy { it.civName }
        val bindings = cities.map { city ->
            val ownership = city.state.ownership
            val owner = when (ownership) {
                is CityOwnership.Player -> requireNotNull(owners[ownership.playerId]) {
                    "City ${city.id}: unknown owner ${ownership.playerId}"
                }
                CityOwnership.Neutral -> null
                null -> throw IllegalArgumentException("City ${city.id}: missing ownership")
            }
            city to owner
        }
        registry.replaceAll(cities)
        for ((city, owner) in bindings) {
            if (owner == null) city.bindNeutralWorld(game)
            else city.civInfo = owner
        }
        for (civ in game.civilizations) {
            civ.attachCityRegistry(this)
            attached.add(civ)
        }
    }
    fun ownedBy(civ: CivilizationInfo): List<CityInfo> =
        registry.ownedBy(CityOwnership.Player(civ.civName))

    /** Keep the legacy civilization grouping and per-owner order for existing consumers. */
    fun all(): List<CityInfo> {
        val order = game.civilizations.map { CityOwnership.Player(it.civName) }
        return registry.all().sortedBy { city ->
            val index = order.indexOf(city.state.ownership)
            if (index < 0) Int.MAX_VALUE else index
        }
    }

    fun byId(id: CityId): CityInfo? = registry.byId(id)
    fun at(position: CityPosition): CityInfo? = registry.at(position)

    fun register(city: CityInfo) {
        if (city.state.ownership == CityOwnership.Neutral) {
            require(city.gameInfo === game) { "City belongs to another game" }
            registry.register(city)
            return
        }
        val owner = city.civInfo
        require(owner.gameInfo === game) { "City belongs to another game" }
        attach(owner)
        registry.register(city)
    }

    fun remove(city: CityInfo) {
        check(registry.remove(city)) { "City ${city.id} is not registered" }
    }

    fun transfer(city: CityInfo, newOwner: CivilizationInfo) {
        require(newOwner.gameInfo === game) { "New owner belongs to another game" }
        attach(newOwner)
        check(registry.contains(city)) { "City ${city.id} is not registered" }
        registry.remove(city)
        city.civInfo = newOwner
        registry.register(city)
    }

    /** Compatibility setter for callers replacing a civilization's whole list. */
    fun replaceOwned(civ: CivilizationInfo, replacement: List<CityInfo>) {
        val owner = CityOwnership.Player(civ.civName)
        val remaining = registry.all().filter { it.state.ownership != owner }
        val previousOwners = replacement.map { it.state.ownership }
        try {
            for (city in replacement) city.state.ownership = owner
            registry.replaceAll(remaining + replacement)
        } catch (failure: Exception) {
            replacement.forEachIndexed { index, city -> city.state.ownership = previousOwners[index] }
            throw failure
        }
        for (city in replacement) city.civInfo = civ
    }
}
