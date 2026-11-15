package com.unciv.pure.application.city

import java.util.Collections

/** Данные построенного здания без ссылок на модель игры или графические объекты. */
data class CityBuiltBuilding(
    val name: String,
    val description: String,
    val civilopediaLink: String,
    val isFree: Boolean,
    val baseMaintenance: Int,
    val saleGold: Int?,
    val canSell: Boolean
)

/** Независимый снимок списка; базовое содержание отличается от фактических расходов города. */
class CityBuildingsSnapshot(
    buildings: List<CityBuiltBuilding>,
    val baseMaintenance: Int,
    val effectiveMaintenance: Float
) {
    val buildings: List<CityBuiltBuilding> = Collections.unmodifiableList(ArrayList(buildings))
}
