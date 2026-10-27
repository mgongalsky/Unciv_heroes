package com.unciv.logic.city

import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.RoadStatus
import com.unciv.logic.map.TileInfo

/** Game effects of founding, after availability and diplomatic confirmation have been handled. */
object CityFounding {
    /** Tutorial bookkeeping precedes this operation; presentation updates follow it. */
    fun complete(unit: MapUnit, tile: TileInfo) {
        unit.civInfo.addCity(tile.position, unit)
        if (tile.ruleset.tileImprovements.containsKey("City center")) {
            if (unit.civInfo.isMajorCiv())
                tile.changeImprovement("City center")
            else
                tile.changeImprovement("Village City center")
        }
        if (tile.roadStatus != RoadStatus.None)
            tile.removeRoad()
        unit.destroy()
    }
}
