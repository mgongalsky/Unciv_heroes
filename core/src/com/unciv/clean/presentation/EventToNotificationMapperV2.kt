package com.unciv.clean.presentation

import com.unciv.clean.presentation.notifications.Notification
import com.unciv.clean.domain.events.DomainEvent

/**
 * Чистый mapper: DomainEvent -> Notification?
 * Без побочных эффектов, не знает про civ.addNotification().
 */
class EventToNotificationMapperV2 {
    fun mapOrNull(event: DomainEvent): Notification? {
        // TODO: перенести сюда when(event) из старого маппера, но вернуть Notification?
        return null
    }
}
