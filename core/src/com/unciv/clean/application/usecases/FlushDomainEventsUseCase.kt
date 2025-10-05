package com.unciv.clean.application.usecases

import com.unciv.clean.application.ports.events.DomainEventBus
import com.unciv.clean.domain.events.DomainEventCollector

/**
 * Единая точка публикации событий после выполнения доменной логики.
 */
class FlushDomainEventsUseCase(
    private val collector: DomainEventCollector,
    private val bus: DomainEventBus
) {
    /** Публикуем и очищаем буфер. Возвращаем количество опубликованных событий. */
    fun flush(): Int {
        val events = collector.drain() // забираем и очищаем
        if (events.isNotEmpty()) bus.publish(events)
        return events.size
    }
}
