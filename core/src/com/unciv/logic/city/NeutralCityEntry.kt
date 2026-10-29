package com.unciv.logic.city

import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.city.NeutralCityEntryRules
import com.unciv.pure.domain.city.NeutralCityEntryRules.Decision

/** Shared world-map entry facts for player and AI movement. */
object NeutralCityEntry {
    fun decision(city: CityInfo, unit: MapUnit): Decision {
        val eligible = !unit.isDestroyed && unit.isMilitary() && unit.baseUnit.isLandUnit() &&
            !unit.civInfo.isBarbarian() && !unit.civInfo.isSpectator() &&
            unit.civInfo.gameInfo === city.gameInfo
        val defenders = city.state.garrison?.getAllTroops()
            ?.count { it != null && it.currentAmount > 0 } ?: 0
        return NeutralCityEntryRules.decide(city.state.ownership, eligible, defenders)
    }

    fun tryAcquireOnEntry(unit: MapUnit, tile: TileInfo): Boolean {
        if (!tile.isCityCenter()) return false
        val city = tile.getCity() ?: return false
        if (city.state.ownership != com.unciv.pure.domain.city.CityOwnership.Neutral) return false
        if (unit.currentTile !== tile || decision(city, unit) != Decision.ACQUIRE) return false
        NeutralCityOwnership.acquireUndefended(city, unit.civInfo)
        return true
    }
}
