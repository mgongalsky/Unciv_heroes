package com.unciv.pure.application.pathfinding

import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.pathfinding.IMovementContext
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.pathfinding.ParentTileAndTotalDistance
import com.unciv.pure.domain.pathfinding.PathsToTilesWithinTurn

object MovementRangeUseCase {

    fun <T : INavigableTile> execute(
        startTile: T,
        unitMovement: Float,
        context: IMovementContext,
        tilesToIgnore: HashSet<T>? = null,
        targetTile: T? = null,
        considerZoneOfControl: Boolean = true
    ): PathsToTilesWithinTurn<T> {
        if (context is BattlePathfindingContext) return BattleMovementRangeUseCase.execute(
            startTile, unitMovement, context, tilesToIgnore, targetTile, considerZoneOfControl
        )
        val distanceToTiles = PathsToTilesWithinTurn<T>()
        if (unitMovement == 0f) return distanceToTiles
        distanceToTiles[startTile] = ParentTileAndTotalDistance(startTile, 0f)
        var tilesToCheck = listOf(startTile)
        while (tilesToCheck.isNotEmpty()) {
            val updatedTiles = ArrayList<T>()
            for (tileToCheck in tilesToCheck) {
                val enteredZoneOfControl =
                    tileToCheck != startTile && !context.canLeaveTile(tileToCheck)
                if (considerZoneOfControl && enteredZoneOfControl) continue
                for (neighbor in tileToCheck.neighbors) {
                    @Suppress("UNCHECKED_CAST")
                    neighbor as T
                    if (tilesToIgnore?.contains(neighbor) == true) continue
                    if (context.shouldSkipTile(neighbor, targetTile)) continue
                    val totalDistanceToTile = when {
                        !context.hasExplored(neighbor) -> distanceToTiles[tileToCheck]!!.totalDistance + 1f
                        !context.canPassThrough(neighbor) -> unitMovement
                        else -> distanceToTiles[tileToCheck]!!.totalDistance + context.getMovementCost(
                            tileToCheck,
                            neighbor
                        )
                    }
                    if (!distanceToTiles.containsKey(neighbor) || distanceToTiles[neighbor]!!.totalDistance > totalDistanceToTile) {
                        if (totalDistanceToTile < unitMovement) updatedTiles += neighbor
                        distanceToTiles[neighbor] =
                            ParentTileAndTotalDistance(tileToCheck, totalDistanceToTile)
                    }
                }
            }
            tilesToCheck = updatedTiles
        }
        return distanceToTiles
    }
}
