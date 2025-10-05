package com.unciv.clean.application.ports.events

object EventPriority {
    const val CRITICAL = 0      // миграции, коррекции состояния
    const val HIGH = 10         // аналитика/логирование, реактивные эффекты
    const val NORMAL = 50       // UI-уведомления
    const val LOW = 100         // «косметика», телеметрия с низким приоритетом
}
