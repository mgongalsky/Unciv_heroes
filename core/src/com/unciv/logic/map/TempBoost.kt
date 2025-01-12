package com.unciv.logic.map

import com.unciv.models.ruleset.unique.IHasUniques
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTarget
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.CivilizationInfo

class TempBoost(
    val source: String,                   // Источник эффекта (например, здание, событие и т.п.)
    val stackable: Boolean = false,        // Можно ли накладывать одинаковые эффекты
    var duration: Int = 0,                // Длительность эффекта (например, в ходах)
    var battleCount: Int = 0              // Количество битв, на которые эффект распространяется
) : IHasUniques {

    // Список строковых уникальностей
    override var uniques = ArrayList<String>()

    // Список объектов уникальностей, инициализируемый лениво
    @delegate:Transient
    override val uniqueObjects: List<Unique> by lazy {
        if (uniques.isEmpty()) emptyList()
        else uniques.map { Unique(it, getUniqueTarget(), source) }
    }

    // Карта для быстрого поиска уникальностей по тексту
    @delegate:Transient
    override val uniqueMap: Map<String, List<Unique>> by lazy {
        if (uniques.isEmpty()) emptyMap()
        else uniqueObjects.groupBy { it.placeholderText }
    }

    /**
     * Конструктор для копирования уникальностей из другого объекта, реализующего IHasUniques.
     * Это удобно, если эффект создаётся на основе здания, события или героя.
     */
    constructor(
        source: String,
        stackable: Boolean = false,
        duration: Int = 0,
        battleCount: Int = 0,
        sourceObject: IHasUniques
    ) : this(source, stackable, duration, battleCount) {
        this.uniques.addAll(sourceObject.uniques)
    }

    /**
     * Реализация getUniqueTarget, чтобы указать, что это триггерный объект.
     */
    override fun getUniqueTarget(): UniqueTarget {
        return UniqueTarget.Triggerable
    }

    /**
     * Проверяет, истёк ли эффект.
     * @return true, если эффект должен быть удалён.
     */
    fun isExpired(): Boolean {
        return duration <= 0 && battleCount <= 0
    }

    /**
     * Уменьшает длительность эффекта на 1 ход.
     */
    fun reduceDuration() {
        if (duration > 0) duration--
    }

    /**
     * Уменьшает количество оставшихся битв на 1.
     */
    fun reduceBattleCount() {
        if (battleCount > 0) battleCount--
    }
}
