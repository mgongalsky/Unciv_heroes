package com.unciv.clean.application.ports.events

import com.unciv.clean.domain.events.DomainEvent

/**
 * Порт шины событий (Application boundary).
 */
interface DomainEventBus {
    /** Отправить пачку событий всем подписчикам. Порядок внутри bus детерминирован. */
    fun publish(events: List<DomainEvent>)
}
