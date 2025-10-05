package com.unciv.clean.application.ports

import com.unciv.clean.presentation.notifications.Notification

/**
 * Порт вывода уведомлений. Адаптер реализует доставку в конкретный UI.
 */
interface NotificationsPort {
    fun push(notification: Notification)
}
