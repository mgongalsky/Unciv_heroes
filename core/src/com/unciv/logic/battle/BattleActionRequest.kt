package com.unciv.ui.battlescreen

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.Direction
import com.unciv.logic.army.TroopInfo

enum class ActionType {
    MOVE,
    ATTACK,
    SHOOT // Новый тип действия для стрельбы
}

data class BattleActionRequest(
    val troop: TroopInfo,
    val targetPosition: Vector2,
    val actionType: ActionType,
    val direction: Direction? = null // Направление атаки
)


