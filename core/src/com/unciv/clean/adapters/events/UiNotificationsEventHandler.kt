package com.unciv.clean.adapters.events

import com.unciv.clean.application.ports.events.DomainEventHandler
import com.unciv.clean.application.ports.events.EventPriority
import com.unciv.clean.domain.events.DomainEvent
import com.unciv.clean.presentation.EventToNotificationMapper
import com.unciv.logic.civilization.CivilizationInfo

/**
 * Подписчик: маппит DomainEvent -> UI-нотификации через существующий EventToNotificationMapper.
 * Нулевой риск: используем текущую реализацию mapper.apply(events, getCiv) с побочными эффектами addNotification.
 */
class UiNotificationsEventHandler(
    private val mapper: EventToNotificationMapper,
    private val getCiv: (String) -> CivilizationInfo
) : DomainEventHandler {

    override val priority: Int = EventPriority.NORMAL

    override fun canHandle(event: DomainEvent): Boolean = true // делегируем проверку самому мапперу

    override fun handle(event: DomainEvent) {
        mapper.apply(listOf(event), getCiv)
    }
}
