package com.unciv.pure.application

import com.unciv.Constants
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.RoadStatus
import com.unciv.logic.map.TileInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.UniqueType

object WorkOnImprovementUseCase {
    fun execute(
        tile: TileInfo,
        civInfo: CivilizationInfo,
        ruleset: Ruleset,
        mapUnit: MapUnit,
        onImprovementCompleted: () -> Unit,
        tryProvideProductionToClosestCity: (String) -> Unit
    ) {
        if (tile.isMarkedForCreatesOneImprovement()) return
        tile.turnsToImprovement -= 1
        if (tile.turnsToImprovement != 0) return

        if (civInfo.isCurrentPlayer())
            onImprovementCompleted()

        when {
            tile.improvementInProgress!!.startsWith(Constants.remove) -> {
                val removedFeatureName = tile.improvementInProgress!!.removePrefix(Constants.remove)
                val tileImprovement = tile.getTileImprovement()
                if (tileImprovement != null
                        && tile.terrainFeatures.any {
                            tileImprovement.terrainsCanBeBuiltOn.contains(it) && it == removedFeatureName
                        }
                        && !tileImprovement.terrainsCanBeBuiltOn.contains(tile.baseTerrain)
                ) {
                    tile.removeImprovement()
                    if (tile.resource != null) civInfo.updateDetailedCivResources()
                }
                if (RoadStatus.values().any { tile.improvementInProgress == it.removeAction }) {
                    tile.removeRoad()
                } else {
                    val removedFeatureObject = ruleset.terrains[removedFeatureName]
                    if (removedFeatureObject != null && removedFeatureObject.hasUnique(UniqueType.ProductionBonusWhenRemoved)) {
                        tryProvideProductionToClosestCity(removedFeatureName)
                    }
                    tile.removeTerrainFeature(removedFeatureName)
                }
            }
            tile.improvementInProgress == RoadStatus.Road.name -> tile.addRoad(RoadStatus.Road, civInfo)
            tile.improvementInProgress == RoadStatus.Railroad.name -> tile.addRoad(RoadStatus.Railroad, civInfo)
            tile.improvementInProgress == Constants.repair -> tile.setRepaired()
            else -> {
                val improvement = ruleset.tileImprovements[tile.improvementInProgress]!!
                improvement.handleImprovementCompletion(mapUnit)
                tile.changeImprovement(tile.improvementInProgress)
            }
        }

        tile.improvementInProgress = null
        tile.getCity()?.updateCitizens = true
    }
}
