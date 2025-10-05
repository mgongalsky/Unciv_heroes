package com.unciv.clean.adapters.events

import com.unciv.clean.application.ports.NotificationsPort
import com.unciv.clean.application.ports.events.DomainEventHandler
import com.unciv.clean.application.ports.events.EventPriority
import com.unciv.clean.domain.events.DomainEvent
import com.unciv.clean.presentation.EventToNotificationMapperV2

class UiNotificationsEventHandlerV2(
    private val mapper: EventToNotificationMapperV2,
    private val notifications: NotificationsPort
) : DomainEventHandler {
    override val priority: Int = EventPriority.NORMAL
    override fun canHandle(event: DomainEvent): Boolean = mapper.mapOrNull(event) != null
    override fun handle(event: DomainEvent) {
        mapper.mapOrNull(event)?.let(notifications::push)
    }
}
