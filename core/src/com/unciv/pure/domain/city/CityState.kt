package com.unciv.pure.domain.city

import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.army.deepCopy

/** Stable identity supplied by the world or scenario boundary. */
data class CityId(val value: String)

/** Immutable map coordinate without a graphics or map dependency. */
data class CityPosition(val x: Float, val y: Float)

sealed class CityOwnership {
    object Neutral : CityOwnership()
    data class Player(val playerId: String) : CityOwnership()
}

data class CityHistory(
    val foundingPlayerId: String = "",
    val previousPlayerId: String = "",
    val turnAcquired: Int = 0
)

/** Local state. Legacy systems adapt to this object rather than keeping parallel field copies. */
data class CityState(
    var id: CityId,
    var name: String = "",
    var history: CityHistory = CityHistory(),
    var health: Int = 200,
    var garrison: Army? = null,
    /** Null means not yet bound, for example while reading the legacy city container. */
    var ownership: CityOwnership? = null,
    var position: CityPosition = CityPosition(0f, 0f)
) {
    /** Independent snapshot, including mutable troops and formations. */
    fun deepCopy(): CityState = copy(garrison = garrison?.deepCopy())
}
