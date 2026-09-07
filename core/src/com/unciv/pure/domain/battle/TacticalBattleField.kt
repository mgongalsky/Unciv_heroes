package com.unciv.pure.domain.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.Direction
import com.unciv.logic.HexMath
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop

/** Battlefield data only. Movement, combat and AI are provided by the game's BattleManager. */
class TacticalBattleField(cells: List<Cell>) : IBattleField {
    data class Cell(val position: Point, val impassable: Boolean)

    private val tiles: Map<Point, Tile>
    val values: Collection<IBattleTile> get() = tiles.values

    init {
        require(cells.isNotEmpty()) { "Battlefield must contain cells" }
        require(cells.map { it.position }
            .distinct().size == cells.size) { "Duplicate battlefield coordinates" }
        tiles = cells.associate { it.position to Tile(it) }
    }

    private inner class Tile(private val cell: Cell) : IBattleTile {
        override val position = Vector2(cell.position.x.toFloat(), cell.position.y.toFloat())
        private var troop: Troop? = null
        override val neighbors: Sequence<IBattleTile>
            get() = HexMath.getVectorsAtDistance(position, 1, Int.MAX_VALUE, false)
                .asSequence().mapNotNull { getTileAt(it) }

        override fun isImpassible(): Boolean = cell.impassable
        override fun getTroop(): Troop? = troop
        override fun receiveTroop(troop: Troop) {
            this.troop = troop
        }

        override fun clearTroop() {
            troop = null
        }
    }

    override fun contains(tile: INavigableTile): Boolean = getTileAt(tile.position) === tile
    override fun getTileAt(position: Vector2): IBattleTile? {
        if (position.x != position.x.toInt().toFloat() || position.y != position.y.toInt()
                .toFloat()
        ) return null
        return tiles[Point(position.x.toInt(), position.y.toInt())]
    }

    override fun getNeighborTile(tile: INavigableTile, direction: Direction): IBattleTile? {
        if (!contains(tile) || direction == Direction.DirError) return null
        return getTileAt(HexMath.oneStepTowards(tile.position, direction))
    }

    companion object {
        /** Inputs supply terrain facts explicitly; no world map or application is constructed. */
        fun rectangular(
            width: Int,
            height: Int,
            impassable: (Vector2) -> Boolean
        ): TacticalBattleField {
            require(width > 0 && height > 0)
            val cells = mutableListOf<Cell>()
            for (x in -width / 2..(width - 1) / 2) for (y in -height / 2..(height - 1) / 2) {
                val position = HexMath.evenQ2HexCoords(Vector2(x.toFloat(), y.toFloat()))
                cells.add(Cell(Point(position.x.toInt(), position.y.toInt()), impassable(position)))
            }
            return TacticalBattleField(cells)
        }
    }
}
