package com.unciv.logic.map

import com.badlogic.gdx.math.Vector2
import com.unciv.models.GameConstants
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.domain.city.*

/** Single editing commands. They never run the terrain brush or create runtime city objects. */
object ScenarioCityEditorCommands {
    fun putCity(map: TileMap, ruleset: Ruleset, description: ScenarioCity): Boolean {
        val copy = description.deepCopy()
        val index = map.scenarioCities.indexOfFirst { it.id == copy.id }
        val cities = ArrayList(map.scenarioCities)
        if (index < 0) cities.add(copy) else cities[index] = copy
        ScenarioCityValidation.validate(cities, map.scenarioPlayerSlots,
            map.values.mapTo(HashSet()) { CityPosition(it.position.x, it.position.y) })
        // Other descriptions may require repair after changing the ruleset or terrain.
        // Keep their data intact while validating the city being applied.
        val center = map[Vector2(copy.position.x, copy.position.y)]
        require(center.isLand && !center.isImpassible() && !center.isNaturalWonder()) {
            "A city requires a passable land tile"
        }
        require(copy.garrison.size <= GameConstants.armySize) { "Too many garrison troops" }
        for (building in copy.buildings) require(building in ruleset.buildings) { "Unknown building $building" }
        for (troop in copy.garrison) require(troop.unitName in ruleset.units) { "Unknown unit ${troop.unitName}" }
        if (index >= 0 && map.scenarioCities[index] == copy) return false
        map.scenarioCities = cities
        return true
    }

    fun removeCity(map: TileMap, id: String): Boolean {
        if (map.scenarioCities.none { it.id == id }) return false
        map.scenarioCities = ArrayList(map.scenarioCities.filterNot { it.id == id })
        return true
    }

    fun putSlot(map: TileMap, slot: ScenarioPlayerSlot): Boolean {
        require(slot.id.isNotBlank()) { "Scenario player slot has an empty ID" }
        val copy = slot.copy()
        val index = map.scenarioPlayerSlots.indexOfFirst { it.id == slot.id }
        if (index >= 0 && map.scenarioPlayerSlots[index] == copy) return false
        val slots = ArrayList(map.scenarioPlayerSlots)
        if (index < 0) slots.add(copy) else slots[index] = copy
        map.scenarioPlayerSlots = slots
        return true
    }

    fun removeSlot(map: TileMap, id: String): Boolean {
        require(map.scenarioCities.none { it.owner == ScenarioCityOwner.PlayerSlot && it.ownerSlotId == id }) {
            "This player slot is still used by a city"
        }
        if (map.scenarioPlayerSlots.none { it.id == id }) return false
        map.scenarioPlayerSlots = ArrayList(map.scenarioPlayerSlots.filterNot { it.id == id })
        return true
    }
}
