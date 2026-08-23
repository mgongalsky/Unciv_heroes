package com.unciv.pure.domain.troop

/** A troop's shared capacity to keep fighting as an organized formation. */
data class Formation(
    var current: Int = 0,
    var maximum: Int = 0
) {
    val isBroken: Boolean get() = current == 0
    val fraction: Float get() = if (maximum == 0) 0f else current.toFloat() / maximum

    fun normalize() {
        maximum = maximum.coerceAtLeast(0)
        current = current.coerceIn(0, maximum)
    }

    companion object {
        const val POINTS_PER_SOLDIER = 10

        fun forSoldiers(amount: Int): Formation {
            val maximum = amount.coerceAtLeast(0) * POINTS_PER_SOLDIER
            return Formation(current = maximum, maximum = maximum)
        }
    }
}
