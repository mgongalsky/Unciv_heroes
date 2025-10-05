package com.unciv.clean.presentation.notifications

/** Минимальная модель UI-сообщения. Расширяйте по мере надобности. */
data class Notification(
    val text: String,
    val severity: Severity = Severity.Info
) {
    enum class Severity { Info, Warning, Error }
}
