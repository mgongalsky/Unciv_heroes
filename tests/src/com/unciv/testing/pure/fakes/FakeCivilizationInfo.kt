package com.unciv.testing.pure.fakes

import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.NotificationAction

class FakeCivilizationInfo : CivilizationInfo() {
    override fun initialize() {}

    val capturedNotifications = mutableListOf<String>()

    override fun addNotification(text: String, action: NotificationAction?, vararg notificationIcons: String) {
        capturedNotifications.add(text)
    }
}
