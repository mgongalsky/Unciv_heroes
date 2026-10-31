package com.unciv.pure.domain.city

/** Выбирает одну стартовую столицу на слот, не изменяя исходные описания карты. */
object ScenarioCityCapitals {
    fun resolve(cities: List<ScenarioCity>): List<ScenarioCity> {
        val result = cities.map { it.deepCopy() }
        // У нейтрального города нет столицы цивилизации, даже если флаги остались от старого редактора.
        for (city in result.filter { it.owner == ScenarioCityOwner.Neutral }) {
            city.isCapital = false
            city.isOriginalCapital = false
        }
        for (owned in result.filter { it.owner == ScenarioCityOwner.PlayerSlot }.groupBy { it.ownerSlotId }.values) {
            // Приоритет: отметка автора, исходная столица старой карты, первый город слота.
            // Повторные отметки разрешаются порядком списка, а не блокируют запуск миссии.
            val capital = owned.firstOrNull { it.isCapital }
                ?: owned.firstOrNull { it.isOriginalCapital }
                ?: owned.first()
            for (city in owned) {
                city.isCapital = city === capital
                city.isOriginalCapital = city === capital
            }
        }
        return result
    }
}
