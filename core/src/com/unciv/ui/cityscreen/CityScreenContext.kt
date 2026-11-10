package com.unciv.ui.cityscreen

import com.unciv.UncivGame

/** Контекст доступа к городскому UI; не хранит копию города или игровые правила. */
class CityScreenContext(
    val canChangeState: () -> Boolean,
    val isPlayersTurn: () -> Boolean,
    val recordTutorialTasks: Boolean = true
) {
    companion object {
        /** Обычный вход сохраняет проверки текущего мира, включая сюжетные и сетевые ограничения. */
        fun fromWorld(): CityScreenContext {
            val world = checkNotNull(UncivGame.Current.worldScreen) {
                "CityScreen requires a world or an explicit standalone context"
            }
            return CityScreenContext(
                canChangeState = { world.canChangeState },
                isPlayersTurn = { world.isPlayersTurn }
            )
        }
    }
}
