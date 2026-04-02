package com.unciv.pure.domain.pathfinding

import com.unciv.pure.domain.pathfinding.INavigableTile

class PathsToTilesWithinTurn<T : INavigableTile> : LinkedHashMap<T, ParentTileAndTotalDistance<T>>() {
    fun getPathToTile(tile: T): List<T> {
        if (!containsKey(tile))
            throw Exception("Can't reach this tile!")
        val reversePathList = ArrayList<T>()
        var currentTile = tile
        while (get(currentTile)!!.parentTile != currentTile) {
            reversePathList.add(currentTile)
            currentTile = get(currentTile)!!.parentTile
        }
        return reversePathList.reversed()
    }
}
