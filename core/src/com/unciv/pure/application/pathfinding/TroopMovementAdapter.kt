package com.unciv.pure.application.pathfinding

import com.unciv.logic.MovableUnit
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.troop.Troop

class TroopMovementAdapter(
    val troop: Troop,
    tile: TileInfo,
    val isEnemy: (Troop) -> Boolean = { false },
    civInfo0: CivilizationInfo
) : MovableUnit() {
    init {
        currentTile = tile
        currentMovement = troop.speed.toFloat()
        civInfo = civInfo0
    }
}
