package com.unciv.pure.application.pathfinding

import com.unciv.pure.domain.battle.ZoneOfControlTransition
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.pathfinding.ParentTileAndTotalDistance
import com.unciv.pure.domain.pathfinding.PathsToTilesWithinTurn

/** Finds affordable tactical routes, preserving arrivals that allow continued movement. */
object BattleMovementRangeUseCase {
    private data class Route<T>(val tile: T, val cost: Float, val path: List<T>)

    fun <T : INavigableTile> execute(
        startTile: T,
        unitMovement: Float,
        context: BattlePathfindingContext,
        tilesToIgnore: HashSet<T>? = null,
        targetTile: T? = null,
        considerZoneOfControl: Boolean = true
    ): PathsToTilesWithinTurn<T> {
        require(unitMovement.isFinite() && unitMovement >= 0f)
        val result = PathsToTilesWithinTurn<T>()
        if (unitMovement == 0f) return result
        result[startTile] = ParentTileAndTotalDistance(startTile, 0f)
        result.recordPath(startTile, emptyList())
        val expandableCosts = mutableMapOf(startTile to 0f)
        val pending = java.util.ArrayDeque<Route<T>>()
        pending.add(Route(startTile, 0f, emptyList()))

        fun remember(tile: T, parent: T, cost: Float, path: List<T>) {
            if (cost < (result[tile]?.totalDistance ?: Float.POSITIVE_INFINITY)) {
                result[tile] = ParentTileAndTotalDistance(parent, cost)
                result.recordPath(tile, path)
            }
        }

        while (pending.isNotEmpty()) {
            val route = pending.removeFirst()
            if (route.cost != expandableCosts[route.tile]) continue
            for (rawNeighbor in route.tile.neighbors) {
                @Suppress("UNCHECKED_CAST")
                val neighbor = rawNeighbor as T
                if (neighbor == startTile || tilesToIgnore?.contains(neighbor) == true) continue
                if (context.shouldSkipTile(neighbor, targetTile)) continue
                val from = if (considerZoneOfControl) context.controlStrength(route.tile)
                else ZoneOfControlTransition.Strength.NONE
                val to = if (considerZoneOfControl) context.controlStrength(neighbor)
                else ZoneOfControlTransition.Strength.NONE
                val step = ZoneOfControlTransition.execute(
                    ZoneOfControlTransition.Input(
                        from = from,
                        to = to,
                        remainingMovement = unitMovement - route.cost,
                        isFirstStep = route.path.isEmpty(),
                        baseCost = context.getMovementCost(route.tile, neighbor),
                        normalMultiplier = context.normalControlMultiplier
                    )
                )
                if (!step.allowed) continue
                val path = route.path + neighbor
                if (!context.canPassThrough(neighbor)) {
                    // Occupied targets remain candidates for the command's typed occupancy rejection.
                    remember(neighbor, route.tile, unitMovement, path)
                    continue
                }
                val cost = route.cost + step.movementCost
                remember(neighbor, route.tile, cost, path)
                if (!step.endsMovement && cost < unitMovement &&
                    cost < (expandableCosts[neighbor] ?: Float.POSITIVE_INFINITY)
                ) {
                    expandableCosts[neighbor] = cost
                    pending.add(Route(neighbor, cost, path))
                }
            }
        }
        return result
    }
}
