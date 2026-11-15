package com.unciv.ui.cityscreen

import com.unciv.logic.city.CityInfo
import com.unciv.pure.application.city.CityBuiltBuilding
import com.unciv.pure.application.city.CityBuildingsSnapshot

/** Читает готовую модель; перевод legacy-описания остаётся на стороне адаптера. */
internal object CityBuildingsSnapshotAdapter {
    fun read(
        city: CityInfo,
        canChangeState: Boolean,
        isPlayersTurn: Boolean
    ): CityBuildingsSnapshot {
        val freeBuildings = city.civInfo.civConstructions.getFreeBuildings(city.id)
        val buildings =
                city.cityConstructions.getBuiltBuildings().sortedBy { it.name }.map { building ->
                    val free = building.name in freeBuildings
                    CityBuiltBuilding(
                        name = building.name,
                        description = building.getDescription(city, true),
                        civilopediaLink = building.makeLink(),
                        isFree = free,
                        baseMaintenance = if (free) 0 else building.maintenance,
                        saleGold = if (building.isSellable()) city.getGoldForSellingBuilding(
                            building.name
                        ) else null,
                        canSell = canSell(city, building.name, canChangeState, isPlayersTurn)
                    )
                }.toList()
        return CityBuildingsSnapshot(
            buildings,
            city.cityConstructions.getMaintenanceCosts(),
            -(city.cityStats.finalStatList["Maintenance"]?.gold ?: 0f)
        )
    }

    /** Повторно проверяется при подтверждении: снимок разрешения мог устареть. */
    fun canSell(city: CityInfo, name: String, canChangeState: Boolean, isPlayersTurn: Boolean): Boolean {
        val building = city.getRuleset().buildings[name] ?: return false
        return canChangeState && isPlayersTurn && !city.isPuppet &&
            city.cityConstructions.isBuilt(name) && building.isSellable() &&
            (!city.hasSoldBuildingThisTurn || city.civInfo.gameInfo.gameParameters.godMode)
    }
}
