package com.unciv.models.ruleset

import com.unciv.logic.city.CityInfo
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.stats.INamed

class CityEvent : INamed {
    override var name = ""
    var description = ""
    var requiredBuilding: String = "" // Здание, активирующее событие
    var effects = ArrayList<Unique>() // Список эффектов события
    var duration: Int = 0 // Длительность в ходах

    fun applyEffects(city: CityInfo) {
        // Логика применения эффектов к городу
        //for (effect in effects) city.addEffect(effect)
    }

    fun removeEffects(city: CityInfo) {
        // Логика снятия эффектов после завершения события
        //for (effect in effects) city.removeEffect(effect)
    }
}
