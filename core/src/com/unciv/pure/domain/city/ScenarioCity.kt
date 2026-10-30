package com.unciv.pure.domain.city

/** Stable map identity, independent of the nation and the multiplayer account. */
data class ScenarioPlayerSlot(
    var id: String = "",
    var name: String = ""
)

enum class ScenarioCityOwner { Neutral, PlayerSlot }
enum class ScenarioCityTypeMode { Fixed, FromStartingPlayer }

/** An initial troop stack, not a live army with runtime dependencies. */
data class ScenarioCityTroop(
    var unitName: String = "",
    var amount: Int = 1
)

/** Editable map data. Reading it never creates a live city or invokes founding effects. */
data class ScenarioCity(
    var id: String = "",
    var position: CityPosition = CityPosition(0f, 0f),
    var name: String = "",
    var owner: ScenarioCityOwner = ScenarioCityOwner.Neutral,
    var ownerSlotId: String = "",
    var population: Int = 1,
    var foodStored: Int = 0,
    var buildings: ArrayList<String> = arrayListOf(),
    var garrison: ArrayList<ScenarioCityTroop> = arrayListOf(),
    /** Empty means the center tile only. Otherwise the center must be included explicitly. */
    var territory: ArrayList<CityPosition> = arrayListOf(),
    var isCapital: Boolean = false,
    var isOriginalCapital: Boolean = false,
    var typeMode: ScenarioCityTypeMode = ScenarioCityTypeMode.FromStartingPlayer,
    /** Stable type key, resolved by the scenario startup boundary. */
    var fixedType: String = ""
) {
    fun deepCopy(): ScenarioCity = copy(
        buildings = ArrayList(buildings),
        garrison = ArrayList(garrison.map { it.copy() }),
        territory = ArrayList(territory)
    )
}
