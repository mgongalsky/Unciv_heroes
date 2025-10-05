package com.unciv.clean.application.mappers

import com.unciv.clean.application.dto.GameInfoDto
import com.unciv.logic.GameInfo

/**
 * Stage 8: lossless pass-through mapper.
 * Ничего не «распаковываем» и не пересобираем — сохраняем исходный JSON.
 *
 * Чтобы не тащить сюда зависимость от конкретной JSON-библиотеки,
 * получаем функций-замыкания serialize/deserialize из вызвавшего кода.
 */
object GameInfoMapper {

    /**
     * Преобразует доменную модель -> DTO, сохраняя точный исходный JSON.
     * @param serialize функция, которая превращает GameInfo в JSON-строку (ровно как сейчас в репозитории)
     */
    fun toDto(
        game: GameInfo,
        serialize: (GameInfo) -> String
    ): GameInfoDto {
        val raw = serialize(game)
        // На этапе 8 НЕ лезем в поля домена, чтобы не плодить связности.
        // При желании позже можно аккуратно заполнить проекционные поля.
        return GameInfoDto(
            raw = raw
            // , turn = game.turns, currentPlayer = game.currentPlayer, ... (добавим позже при фиксации контракта)
        )
    }

    /**
     * Преобразует DTO -> доменная модель, используя исходный сырой JSON.
     * @param deserialize функция, которая из JSON-строки строит GameInfo (как раньше)
     */
    fun fromDto(
        dto: GameInfoDto,
        deserialize: (String) -> GameInfo
    ): GameInfo = deserialize(dto.raw)
}
