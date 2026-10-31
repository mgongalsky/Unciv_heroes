package com.unciv.pure.domain.scenario

import com.unciv.pure.domain.victory.CaptureCityObjective

/** Ограничение автора карты: обязательная, выбираемая игроком либо запрещённая победа. */
enum class ScenarioVictoryMode { Required, Optional, Forbidden }

/** Настройка одного типа; defaultEnabled применяется только до явного выбора игрока. */
data class ScenarioVictoryRule(
    var victoryType: String = "",
    var mode: ScenarioVictoryMode = ScenarioVictoryMode.Optional,
    var defaultEnabled: Boolean = true
)

/** Данные карты без ссылок на движок; отсутствие контейнера означает прежние правила выбора побед. */
data class ScenarioVictorySettings(
    var rules: ArrayList<ScenarioVictoryRule> = arrayListOf(),
    var captureObjectives: ArrayList<CaptureCityObjective> = arrayListOf()
) {
    /** Черновик, карта и партия не разделяют изменяемые коллекции или их элементы. */
    fun deepCopy(): ScenarioVictorySettings = copy(
        rules = ArrayList(rules.map { it.copy() }),
        captureObjectives = ArrayList(captureObjectives.map { it.deepCopy() })
    )
}
