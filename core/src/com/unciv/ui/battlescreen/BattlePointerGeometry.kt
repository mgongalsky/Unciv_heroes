package com.unciv.ui.battlescreen

import com.unciv.logic.Direction

/** Shared pointer sectors for attack commands, cursors and forecasts on both battle screens. */
object BattlePointerGeometry {
    fun attackDirection(x: Float, y: Float, width: Float): Direction {
        if (width <= 0f || !width.isFinite() || !x.isFinite() || !y.isFinite())
            return Direction.DirError
        val x0 = width / 2f
        val height = width * 1.1547f
        val y0 = x0 * 0.577f
        return when {
            y - (height - y0) + x * (height - y0) / (3f * x0) >= 0 && x <= x0 ->
                Direction.TopLeft
            y - (height - y0) + x * (height - y0) / (3f * x0) < 0 &&
                y - y0 - x * y0 / x0 >= 0 -> Direction.CenterLeft
            y - y0 - x * y0 / x0 < 0 && x <= x0 -> Direction.BottomLeft
            y - (height - y0) + x * (height - y0) / (3f * x0) < 0 && x > x0 ->
                Direction.BottomRight
            y - (height - y0) + x * (height - y0) / (3f * x0) >= 0 &&
                y - y0 - x * y0 / x0 < 0 -> Direction.CenterRight
            y - y0 - x * y0 / x0 >= 0 && x > x0 -> Direction.TopRight
            else -> Direction.DirError
        }
    }
}
