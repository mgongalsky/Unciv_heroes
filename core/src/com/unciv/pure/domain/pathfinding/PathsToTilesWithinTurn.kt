package com.unciv.pure.domain.pathfinding

import com.unciv.pure.domain.pathfinding.INavigableTile

class PathsToTilesWithinTurn<T : INavigableTile> : LinkedHashMap<T, ParentTileAndTotalDistance<T>>() {
    private val explicitPaths = mutableMapOf<T, List<T>>()
    fun getPathToTile(tile: T): List<T> {
        if (!containsKey(tile)) throw Exception("Can't reach this tile!")
        explicitPaths[tile]?.let { return it.toList() }
        val reversePathList = ArrayList<T>()
        var currentTile = tile
        while (get(currentTile)!!.parentTile != currentTile) {
            reversePathList.add(currentTile)
            currentTile = get(currentTile)!!.parentTile
        }
        return reversePathList.reversed()
    }

    /** Records a complete route when different arrivals at a tile have different continuation rights. */
    internal fun recordPath(tile: T, path: List<T>) {
        explicitPaths[tile] = path.toList()
    }
}
