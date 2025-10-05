package com.unciv.clean.domain.events

class DomainEventCollector {
    private val events = mutableListOf<DomainEvent>()
    fun emit(e: DomainEvent) { events += e }
    fun drain(): List<DomainEvent> {
        val copy = events.toList()
        events.clear()
        return copy
    }
    fun peek(): List<DomainEvent> = events.toList()
}
