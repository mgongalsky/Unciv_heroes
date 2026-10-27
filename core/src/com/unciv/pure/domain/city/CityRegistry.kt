package com.unciv.pure.domain.city

/** Owns city membership; state remains on the registered objects, without parallel copies. */
class CityRegistry<T : Any>(private val stateOf: (T) -> CityState) {
    private val cities = ArrayList<T>()

    /** Snapshot membership allows registration and removal during a caller's traversal. */
    fun all(): List<T> = cities.toList()

    fun byId(id: CityId): T? = cities.firstOrNull { stateOf(it).id == id }

    fun at(position: CityPosition): T? = cities.firstOrNull { stateOf(it).position == position }

    fun ownedBy(owner: CityOwnership): List<T> = cities.filter { stateOf(it).ownership == owner }

    fun contains(city: T): Boolean = cities.any { it === city }

    fun register(city: T) {
        val state = stateOf(city)
        require(state.ownership != null) { "City ${state.id.value} has no bound owner" }
        require(byId(state.id) == null) { "Duplicate city ID: ${state.id.value}" }
        require(at(state.position) == null) { "Duplicate city position: ${state.position}" }
        cities.add(city)
    }

    /** Replaces membership only after the entire incoming collection has been validated. */
    fun replaceAll(replacement: Collection<T>) {
        val validated = CityRegistry(stateOf)
        for (city in replacement) validated.register(city)
        cities.clear()
        cities.addAll(validated.cities)
    }

    fun remove(city: T): Boolean {
        val index = cities.indexOfFirst { it === city }
        if (index < 0) return false
        cities.removeAt(index)
        return true
    }
}
