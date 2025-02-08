package com.unciv.ui.battlescreen

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.Direction
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.map.TileInfo

/**
 * Enumeration of possible action types in battle.
 */
enum class ActionType {
    MOVE,
    ATTACK,
    SHOOT, // New action type for ranged attacks
    SKIP   // New action type for skipping the turn
}

/**
 * Represents a request for a battle action.
 *
 * @property troop The troop performing the action.
 * @property targetPosition The target position for the action.
 * @property actionType The type of action (MOVE, ATTACK, SHOOT, SKIP).
 * @property direction The attack direction, if applicable.
 */
data class BattleActionRequest(
    val troop: TroopInfo,
    val targetPosition: TileInfo,
    val actionType: ActionType,
    val direction: Direction? = null // Attack direction (if applicable)
)
