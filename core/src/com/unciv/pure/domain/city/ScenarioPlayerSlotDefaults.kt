package com.unciv.pure.domain.city

/** Empty means automatic; null means explicitly unassigned or ineligible (for example a spectator). */
object ScenarioPlayerSlotDefaults {
    fun assign(slots: List<ScenarioPlayerSlot>, assignments: List<String?>): List<String?> {
        val ids = slots.map { it.id }.toSet()
        val reserved = HashSet<String>()
        val result = assignments.map { id ->
            when {
                id == null -> null
                id in ids && reserved.add(id) -> id
                else -> ""
            }
        }.toMutableList()
        val free = slots.map { it.id }.filter { it !in reserved }.iterator()
        for (index in result.indices) {
            if (result[index] == "" && free.hasNext()) result[index] = free.next()
        }
        return result
    }
}
