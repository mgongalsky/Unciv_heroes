package com.unciv.pure.application.scenario

import com.unciv.pure.domain.scenario.ScenarioVictoryMode
import com.unciv.pure.domain.scenario.ScenarioVictorySettings

/** Применяет ограничения карты к каталогу и выбору игрока без изменения входных коллекций. */
object ResolveVictoryOptionsUseCase {
    /** null означает первичную настройку; пустая коллекция — явное отключение всех Optional. */
    fun execute(
        availableVictoryTypes: Collection<String>,
        settings: ScenarioVictorySettings?,
        selectedVictoryTypes: Collection<String>?
    ): List<String> {
        val available = availableVictoryTypes.toSet()
        val rules = settings?.rules.orEmpty()
        require(rules.all { it.victoryType.isNotBlank() }) { "Victory type must not be empty" }
        require(rules.map { it.victoryType }.distinct().size == rules.size) { "Duplicate scenario victory type" }
        for (rule in rules) {
            require(rule.mode != ScenarioVictoryMode.Required || rule.victoryType in available) {
                "Required victory type is unavailable: ${rule.victoryType}"
            }
        }
        val byType = rules.associateBy { it.victoryType }
        val selected = selectedVictoryTypes?.toSet()
        val result = availableVictoryTypes.distinct().filter { type ->
            val rule = byType[type]
            when (rule?.mode) {
                ScenarioVictoryMode.Required -> true
                ScenarioVictoryMode.Forbidden -> false
                else -> selected?.contains(type) ?: (rule?.defaultEnabled ?: true)
            }
        }
        require(result.isNotEmpty()) { "At least one victory type must be enabled" }
        return result
    }
}
