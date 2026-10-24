package com.unciv.logic.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.HexMath
import com.unciv.pure.domain.battle.Point

/** Hex line using the battlefield's cube conversion and rounding conventions. */
object BattleShotLine {
    /**
     * Both sides of an exact hex-edge tie can obstruct the shot. Canonical endpoint order
     * ensures reversal symmetry; local coordinates keep rounding independent of translation.
     */
    fun intermediateCells(from: Point, to: Point): Set<Point> {
        if (from == to) return emptySet()
        val forward = from.x < to.x || (from.x == to.x && from.y < to.y)
        val start = if (forward) from else to
        val end = if (forward) to else from
        val dx = end.x.toDouble() - start.x
        val dy = end.y.toDouble() - start.y
        val distance = HexMath.getDistance(Vector2.Zero, Vector2(dx.toFloat(), dy.toFloat()))
        val cells = linkedSetOf<Point>()
        for (step in 1 until distance) {
            val fraction = step.toDouble() / distance
            for (offset in listOf(-0.0001, 0.0001)) {
                val rounded = HexMath.roundHexCoords(
                    Vector2(
                        (dx * fraction + offset).toFloat(),
                        (dy * fraction + offset).toFloat()
                    )
                )
                val cell = Point(start.x + rounded.x.toInt(), start.y + rounded.y.toInt())
                if (cell != from && cell != to) cells.add(cell)
            }
        }
        return cells
    }
}
