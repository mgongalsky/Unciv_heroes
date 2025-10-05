package com.unciv.clean.application.usecases.battle

import com.unciv.clean.application.ports.battle.CombatResolver
import com.unciv.clean.application.ports.events.DomainEventBus
import com.unciv.clean.domain.events.DomainEventCollector

/**
 * Чистый апп-юзкейс: валидирует команду, делегирует расчёт в CombatResolver,
 * публикует события и возвращает компактный результат.
 */
class ResolveAttackUseCase(
    private val combat: CombatResolver,
    private val events: DomainEventCollector,
    private val bus: DomainEventBus
) {
    fun execute(command: AttackCommand): BattleResult {
        val result = combat.resolve(command)
        val drained = events.drain()
        val toPublish = if (drained.isNotEmpty()) drained else result.events
        if (toPublish.isNotEmpty()) bus.publish(toPublish)
        return result.copy(events = toPublish)
    }
}
