package com.unciv.pure.application.scenario

import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.city.ScenarioPlayerSlot
import com.unciv.pure.domain.hero.ScenarioHero

/** Проверяет весь набор размещений до создания юнитов; не меняет исходные описания и назначения. */
object PrepareScenarioHeroesUseCase {
    /** Описание отделено от карты; владелец уже разрешён из стабильного слота в участника запуска. */
    data class PreparedHero(val description: ScenarioHero, val owner: String)

    /** Проверка связей пригодна также редактору, которому ещё неизвестны выбранные игроками нации. */
    fun validateDescriptions(
        heroes: List<ScenarioHero>,
        slots: List<ScenarioPlayerSlot>,
        mapPositions: Set<CityPosition>
    ) {
        val slotIds = HashSet<String>()
        for (slot in slots) {
            require(slot.id.isNotBlank() && slotIds.add(slot.id)) {
                "Empty or duplicate scenario player slot ${slot.id}"
            }
        }
        val ids = HashSet<String>()
        val positions = HashSet<CityPosition>()
        for (hero in heroes) {
            hero.validate()
            require(ids.add(hero.id)) { "Duplicate scenario hero ${hero.id}" }
            require(hero.ownerSlotId in slotIds) { "Scenario hero ${hero.id}: unknown player slot ${hero.ownerSlotId}" }
            require(hero.position in mapPositions) { "Scenario hero ${hero.id}: position is outside the map" }
            require(positions.add(hero.position)) { "Scenario heroes share position ${hero.position}" }
        }
    }

    /** Пригодность местности и конфликты с игровыми объектами передаются фактами из адаптера. */
    fun execute(
        heroes: List<ScenarioHero>,
        slots: List<ScenarioPlayerSlot>,
        mapPositions: Set<CityPosition>,
        participants: List<Pair<String, String>>,
        availableUnitNames: Set<String>,
        occupiedPositions: Set<CityPosition> = emptySet()
    ): List<PreparedHero> {
        validateDescriptions(heroes, slots, mapPositions)
        val bindings = ScenarioPlayerBindings.resolve(slots, participants)
        require(bindings.values.all { it.isNotBlank() }) { "Scenario participant has an empty name" }
        require(bindings.values.distinct().size == bindings.size) { "A participant occupies multiple scenario slots" }
        for (hero in heroes) {
            require(hero.unitName in availableUnitNames) { "Scenario hero ${hero.id}: unknown unit ${hero.unitName}" }
            require(hero.position !in occupiedPositions) { "Scenario hero ${hero.id}: position is occupied" }
        }
        // Все проверки завершены до выдачи результата; координаты являются неизменяемыми значениями.
        return heroes.map { PreparedHero(it.copy(), bindings.getValue(it.ownerSlotId)) }
    }
}
