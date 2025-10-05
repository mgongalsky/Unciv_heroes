package com.unciv.clean.application.usecases.battle

import com.unciv.clean.domain.events.DomainEvent

/**
 * Итог боя в Application-слое: минимальный отчёт + события.
 * Вся визуализация/анимации – через Event-подписчиков (Stage 9).
 */
data class BattleResult(
    val attackerRemainingHp: Int,
    val defenderRemainingHp: Int,
    val attackerDefeated: Boolean,
    val defenderDefeated: Boolean,
    val events: List<DomainEvent> = emptyList()
)
