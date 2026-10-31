package com.unciv.pure.domain.city

/**
 * Проверяет явное сопоставление слотов карты участникам запуска.
 * Не угадывает назначение по нации, аккаунту или позиции строки: удобные значения задаёт меню заранее.
 */
object ScenarioPlayerBindings {
    fun <T> resolve(slots: List<ScenarioPlayerSlot>, assignments: List<Pair<String, T>>): Map<String, T> {
        val ids = slots.map { it.id }
        require(ids.none { it.isBlank() } && ids.distinct().size == ids.size) {
            "Scenario player slots contain empty or duplicate IDs"
        }
        val result = LinkedHashMap<String, T>()
        for ((slotId, participant) in assignments) {
            if (slotId.isEmpty()) continue // Участник вне сценарных слотов тоже может участвовать в игре.
            require(slotId in ids) { "Unknown scenario player slot $slotId" }
            require(!result.containsKey(slotId)) { "Scenario player slot $slotId is assigned more than once" }
            result[slotId] = participant
        }
        // Пропущенный участник — ошибка настройки, а не просьба сделать его города нейтральными.
        for (id in ids) require(result.containsKey(id)) { "Scenario player slot $id has no participant" }
        return result
    }
}
