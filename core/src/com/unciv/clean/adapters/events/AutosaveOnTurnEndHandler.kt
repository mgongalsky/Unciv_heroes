package com.unciv.clean.adapters.events

import com.unciv.clean.application.ports.events.DomainEventHandler
import com.unciv.clean.application.ports.events.EventPriority
import com.unciv.clean.domain.events.DomainEvent

/**
 * Пример плагина: автосейв по завершению хода.
 * Здесь слушаем TurnAdvanced, а вызываемый код сохраняет состояние.
 */
class AutosaveOnTurnEndHandler(
    private val onAutosave: (DomainEvent.TurnAdvanced) -> Unit
) : DomainEventHandler {

    override val priority: Int = EventPriority.HIGH

    override fun canHandle(event: DomainEvent): Boolean = event is DomainEvent.TurnAdvanced

    override fun handle(event: DomainEvent) {
        if (event is DomainEvent.TurnAdvanced) onAutosave(event)
    }
}
