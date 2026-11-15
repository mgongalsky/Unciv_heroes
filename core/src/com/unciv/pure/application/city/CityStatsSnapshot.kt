package com.unciv.pure.application.city

import java.util.Collections

/** Идентификаторы показателей без зависимости от игрового Stat, графики и локализации. */
enum class CityStatResource {
    Production, Food, Gold, Science, Culture, Happiness, Faith
}

/** Копия числовых значений; наружу не передаётся изменяемая карта источника. */
class CityStatValues(values: Map<CityStatResource, Float>) {
    private val values = Collections.unmodifiableMap(LinkedHashMap(values))

    operator fun get(resource: CityStatResource): Float = values[resource] ?: 0f
}

/** Источник с дочерними пояснениями; родительское значение уже включает дочерние. */
class CityStatSource(
    val name: String,
    val values: CityStatValues,
    children: List<CityStatSource> = emptyList()
) {
    val children: List<CityStatSource> = Collections.unmodifiableList(ArrayList(children))
}

/** Снимок готовых расчётов города. Проценты не являются слагаемыми итоговых доходов. */
class CityStatsSnapshot(
    val population: Int,
    val foodStored: Int,
    val civilizationGold: Int,
    resources: List<CityStatResource>,
    baseSources: List<CityStatSource>,
    percentageSources: List<CityStatSource>,
    finalSources: List<CityStatSource>,
    val totals: CityStatValues
) {
    val resources: List<CityStatResource> = Collections.unmodifiableList(ArrayList(resources))
    val baseSources: List<CityStatSource> = Collections.unmodifiableList(ArrayList(baseSources))
    val percentageSources: List<CityStatSource> = Collections.unmodifiableList(ArrayList(percentageSources))
    val finalSources: List<CityStatSource> = Collections.unmodifiableList(ArrayList(finalSources))
}
