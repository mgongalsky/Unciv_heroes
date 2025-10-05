package com.unciv.clean.application.ports.events

import com.unciv.clean.domain.events.DomainEvent

/**
 * Подписчик на доменные события.
 * priority: меньше -> раньше. Используйте константы из EventPriority.
 */
interface DomainEventHandler {
    val priority: Int get() = EventPriority.NORMAL
    fun canHandle(event: DomainEvent): Boolean
    fun handle(event: DomainEvent)
}
