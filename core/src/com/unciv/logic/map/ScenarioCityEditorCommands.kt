package com.unciv.logic.map

import com.badlogic.gdx.math.Vector2
import com.unciv.models.GameConstants
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.domain.city.*

/** Одиночные команды редактора: меняют описания карты без кисти рельефа и создания игровых CityInfo. */
object ScenarioCityEditorCommands {
    /** Проверяет копию черновика и добавляет либо заменяет описание по ID; false означает отсутствие изменений. */
    fun putCity(map: TileMap, ruleset: Ruleset, description: ScenarioCity): Boolean {
        val copy = description.deepCopy()
        val index = map.scenarioCities.indexOfFirst { it.id == copy.id }
        val cities = ArrayList(map.scenarioCities)
        if (index < 0) cities.add(copy) else cities[index] = copy
        // Ссылки на слоты и пересечения территории проверяются для всего предполагаемого результата.
        ScenarioCityValidation.validate(cities, map.scenarioPlayerSlots,
            map.values.mapTo(HashSet()) { CityPosition(it.position.x, it.position.y) })
        // После смены правил или рельефа другие города могут требовать исправления.
        // Их данные сохраняем, а определения и пригодность центра проверяем только у применяемого города.
        val center = map[Vector2(copy.position.x, copy.position.y)]
        require(center.isLand && !center.isImpassible() && !center.isNaturalWonder()) {
            "A city requires a passable land tile"
        }
        require(copy.garrison.size <= GameConstants.armySize) { "Too many garrison troops" }
        for (building in copy.buildings) require(building in ruleset.buildings) { "Unknown building $building" }
        for (troop in copy.garrison) require(troop.unitName in ruleset.units) { "Unknown unit ${troop.unitName}" }
        if (index >= 0 && map.scenarioCities[index] == copy) return false
        // Публикуем результат только после всех проверок; отказ не меняет прежний список карты.
        map.scenarioCities = cities
        return true
    }

    /** Удаляет описание по стабильному ID; повторное удаление возвращает false. */
    fun removeCity(map: TileMap, id: String): Boolean {
        if (map.scenarioCities.none { it.id == id }) return false
        map.scenarioCities = ArrayList(map.scenarioCities.filterNot { it.id == id })
        return true
    }

    /** Сохраняет независимую копию слота, добавляя его или заменяя запись с тем же ID. */
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

    /** Запрещает удаление слота, на который ссылается город; отсутствие неиспользуемого слота даёт false. */
    fun removeSlot(map: TileMap, id: String): Boolean {
        require(map.scenarioCities.none { it.owner == ScenarioCityOwner.PlayerSlot && it.ownerSlotId == id }) {
            "This player slot is still used by a city"
        }
        if (map.scenarioPlayerSlots.none { it.id == id }) return false
        map.scenarioPlayerSlots = ArrayList(map.scenarioPlayerSlots.filterNot { it.id == id })
        return true
    }
}
