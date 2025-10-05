package com.unciv.clean.application.dto

/**
 * Нулевой-риск DTO-обёртка для сохранений.
 *
 * На этом этапе мы сохраняем оригинальный JSON в raw (байт-в-байт),
 * чтобы поведение и совместимость были идентичны текущему.
 * Поля ниже (turn, currentPlayer, ...) — опциональные «проекции»,
 * которые пригодятся позже для валидаций/миграций/логики.
 */
data class GameInfoDto(
    /** Исходный JSON сейва. Должен писаться/читаться без изменений. */
    val raw: String,

    // --- Проекционные поля (опционально, не участвуют в сериализации на этапе 8)
    val turn: Int? = null,
    val currentPlayer: String? = null,
    val difficulty: String? = null,
    val rulesetName: String? = null
)
