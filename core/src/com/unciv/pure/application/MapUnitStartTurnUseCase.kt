package com.unciv.pure.application

import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.models.ruleset.unique.UniqueType

object MapUnitStartTurnUseCase {
    fun execute(
        unit: MapUnit,
        civInfo: CivilizationInfo,
        currentTile: TileInfo,
        clearPathfindingCache: () -> Unit,
        getMaxMovement: () -> Int,
        teleportToClosestMoveableTile: () -> Unit,
        addMovementMemory: () -> Unit
    ) {
        clearPathfindingCache()
        unit.currentMovement = getMaxMovement().toFloat()
        unit.attacksThisTurn = 0
        unit.due = true

        // Hakkapeliitta movement boost
        if (currentTile.getUnits().count() > 1) {
            for (cohabitant in currentTile.getUnits()) {
                if (cohabitant == unit) continue
                if (cohabitant.getMatchingUniques(UniqueType.TransferMovement)
                            .any { unit.matchesFilter(it.params[0]) }
                )
                    unit.currentMovement = maxOf(
                        getMaxMovement().toFloat(),
                        cohabitant.getMaxMovement().toFloat()
                    )
            }
        }

        // Wake sleeping units if there's an enemy in vision range
        if (unit.isSleeping()
                && (unit.isMilitary() || (currentTile.militaryUnit == null && !currentTile.isCityCenter()))
                && unit.viewableTiles.any {
                    it.militaryUnit != null && it.militaryUnit!!.civInfo.isAtWarWith(civInfo)
                }
        ) unit.action = null

        val tileOwner = currentTile.getOwner()
        if (tileOwner != null
                && !unit.canEnterForeignTerrain
                && !civInfo.canPassThroughTiles(tileOwner)
                && !tileOwner.isCityState()
        ) teleportToClosestMoveableTile()

        addMovementMemory()
        unit.attacksSinceTurnStart.clear()

        // Food warning — only for non-monster units not in cities
        if (!unit.isMonster && !currentTile.isCityCenter()) {
            HeroFoodWarningUseCase.execute(
                currentFood = unit.getCurrentFood(),
                dailyConsumption = unit.army.calculateFoodMaintenance(isInCity = false),
                unitDisplayName = unit.shortDisplayName(),
                unitName = unit.name,
                civInfo = civInfo,
                position = currentTile.position
            )
        }
    }
}
