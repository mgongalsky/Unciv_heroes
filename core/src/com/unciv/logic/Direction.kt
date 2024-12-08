package com.unciv.logic

/** Specifies one of 6 directions of a hex (from center of an edge to center of the hex). [DirError] is handle for error */
enum class Direction(val num: Int) {
    TopRight(0), CenterRight(1), BottomRight(2), BottomLeft(3), CenterLeft(4), TopLeft(5), DirError(6)
}
