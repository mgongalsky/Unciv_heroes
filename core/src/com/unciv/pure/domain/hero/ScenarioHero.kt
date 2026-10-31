package com.unciv.pure.domain.hero

import com.unciv.pure.domain.city.CityPosition

/** Исходное размещение именованного героя; изменяемое состояние партии хранится отдельно в MapUnit. */
data class ScenarioHero(
    var id: String = "",
    var name: String = "",
    var unitName: String = "Warrior",
    var ownerSlotId: String = "",
    var position: CityPosition = CityPosition(0f, 0f)
) {
    /** Проверяет только локальные инварианты; существование слота, клетки и типа проверяет application. */
    fun validate() {
        require(id.isNotBlank()) { "Scenario hero has an empty ID" }
        require(name.isNotBlank()) { "Scenario hero $id has an empty name" }
        require(unitName.isNotBlank()) { "Scenario hero $id has an empty unit type" }
        require(ownerSlotId.isNotBlank()) { "Scenario hero $id has no owner slot" }
        require(position.x.isFinite() && position.y.isFinite()) {
            "Scenario hero $id has invalid coordinates"
        }
    }
}
