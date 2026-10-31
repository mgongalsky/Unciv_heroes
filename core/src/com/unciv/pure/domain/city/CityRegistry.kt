package com.unciv.pure.domain.city

/**
 * Учёт существования городов без зависимости от GameInfo и CivilizationInfo.
 * Хранит сами объекты; ID, позицию и владельца читает из их текущего CityState.
 * Поиск линейный: отдельных индексов, которые могли бы разойтись с состоянием города, нет.
 */
class CityRegistry<T : Any>(private val stateOf: (T) -> CityState) {
    private val cities = ArrayList<T>()

    /** Снимок состава: вызывающий код может регистрировать и удалять города во время своего обхода. */
    fun all(): List<T> = cities.toList()

    /** Находит канонический объект по постоянному ID. */
    fun byId(id: CityId): T? = cities.firstOrNull { stateOf(it).id == id }

    /** Ищет город по центру, а не по любой клетке его территории. */
    fun at(position: CityPosition): T? = cities.firstOrNull { stateOf(it).position == position }

    /** Производная выборка владельца; не создаёт второй список-источник истины. */
    fun ownedBy(owner: CityOwnership): List<T> = cities.filter { stateOf(it).ownership == owner }

    /** Проверяет присутствие именно этого экземпляра, а не равного ему объекта. */
    fun contains(city: T): Boolean = cities.any { it === city }

    /** Добавляет уже привязанный город, запрещая повторные ID и центры. Основание не выполняет. */
    fun register(city: T) {
        val state = stateOf(city)
        require(state.ownership != null) { "City ${state.id.value} has no bound owner" }
        require(byId(state.id) == null) { "Duplicate city ID: ${state.id.value}" }
        require(at(state.position) == null) { "Duplicate city position: ${state.position}" }
        cities.add(city)
    }

    /** Сначала проверяет весь новый состав; при ошибке прежний реестр остаётся нетронутым. */
    fun replaceAll(replacement: Collection<T>) {
        val validated = CityRegistry(stateOf)
        for (city in replacement) validated.register(city)
        cities.clear()
        cities.addAll(validated.cities)
    }

    /** Убирает конкретный экземпляр из учёта; освобождение территории выполняет игровой адаптер. */
    fun remove(city: T): Boolean {
        val index = cities.indexOfFirst { it === city }
        if (index < 0) return false
        cities.removeAt(index)
        return true
    }
}
