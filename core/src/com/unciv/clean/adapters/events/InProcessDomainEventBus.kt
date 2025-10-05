package com.unciv.clean.adapters.events

import com.unciv.clean.application.ports.events.DomainEventBus
import com.unciv.clean.application.ports.events.DomainEventHandler
import com.unciv.clean.domain.events.DomainEvent
import com.unciv.clean.application.ports.LoggerPort

/**
 * Простая синхронная in-process шина событий.
 * - сортирует хэндлеры по priority (стабильно)
 * - изолирует ошибки каждого хэндлера
 */
class InProcessDomainEventBus(
    handlers: List<DomainEventHandler>,
    private val logger: LoggerPort? = null
) : DomainEventBus {

    private val orderedHandlers = handlers.sortedBy { it.priority }

    override fun publish(events: List<DomainEvent>) {
        if (events.isEmpty()) return
        for (event in events) {
            for (h in orderedHandlers) {
                if (!h.canHandle(event)) continue
                try {
                    h.handle(event)
                } catch (t: Throwable) {
                    logger?.debug(
                        "DomainEvent handler failed: %s for %s: %s",
                        h::class.simpleName,
                        event::class.simpleName,
                        t.message
                    )
                    // Не прерываем цепочку.
                }
            }
        }
    }
}
