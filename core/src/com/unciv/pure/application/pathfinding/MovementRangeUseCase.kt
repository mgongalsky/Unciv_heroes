package com.unciv.pure.application.pathfinding

import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.pathfinding.IMovementContext
import com.unciv.pure.domain.pathfinding.ParentTileAndTotalDistance
import com.unciv.pure.domain.pathfinding.PathsToTilesWithinTurn

object MovementRangeUseCase {

    fun execute(
        startTile: TileInfo,
        unitMovement: Float,
        context: IMovementContext,
        tilesToIgnore: HashSet<TileInfo>? = null,
        targetTile: TileInfo? = null
    ): PathsToTilesWithinTurn<TileInfo> {
        val distanceToTiles = PathsToTilesWithinTurn<TileInfo>()
        if (unitMovement == 0f) return distanceToTiles

        distanceToTiles[startTile] = ParentTileAndTotalDistance<TileInfo>(startTile, 0f)
        var tilesToCheck = listOf(startTile)

        while (tilesToCheck.isNotEmpty()) {
            val updatedTiles = ArrayList<TileInfo>()
            for (tileToCheck in tilesToCheck)
                for (neighbor in tileToCheck.neighbors) {
                    if (tilesToIgnore?.contains(neighbor) == true) continue
                    if (context.shouldSkipTile(neighbor, targetTile)) continue
                    var totalDistanceToTile: Float = when {
                        !context.hasExplored(neighbor) ->
                            distanceToTiles[tileToCheck]!!.totalDistance + 1f
                        !context.canPassThrough(neighbor) -> unitMovement
                        else -> {
                            val distanceBetweenTiles = context.getMovementCost(tileToCheck, neighbor)
                            distanceToTiles[tileToCheck]!!.totalDistance + distanceBetweenTiles
                        }
                    }

                    if (!distanceToTiles.containsKey(neighbor) || distanceToTiles[neighbor]!!.totalDistance > totalDistanceToTile) {
                        if (totalDistanceToTile < unitMovement)
                            updatedTiles += neighbor
                        distanceToTiles[neighbor] = ParentTileAndTotalDistance(tileToCheck, totalDistanceToTile)
                    }
                }

            tilesToCheck = updatedTiles
        }

        return distanceToTiles
    }
}
