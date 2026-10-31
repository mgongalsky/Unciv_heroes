package com.unciv.pure.application.scenario

import com.unciv.pure.domain.city.ScenarioPlayerSlot

/**
 * Автоназначение слотов в меню: пустая строка разрешает подбор, null запрещает его
 * для ручного «Без слота» или наблюдателя. Возвращаемый список соответствует порядку участников.
 */
object ScenarioPlayerSlotDefaults {
    fun assign(slots: List<ScenarioPlayerSlot>, assignments: List<String?>): List<String?> {
        val ids = slots.map { it.id }.toSet()
        val reserved = HashSet<String>()
        // Сначала сохраняем все действующие назначения, чтобы ранняя пустая строка не заняла чужой слот.
        val result = assignments.map { id ->
            when {
                id == null -> null
                id in ids && reserved.add(id) -> id
                else -> ""
            }
        }.toMutableList()
        val free = slots.map { it.id }.filter { it !in reserved }.iterator()
        // Неизвестные и повторные назначения также попадают в подбор свободных слотов.
        for (index in result.indices) {
            if (result[index] == "" && free.hasNext()) result[index] = free.next()
        }
        return result
    }
}
