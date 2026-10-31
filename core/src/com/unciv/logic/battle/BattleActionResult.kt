import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.battle.IBattleTile
import com.unciv.logic.map.TileInfo
import com.unciv.ui.battlescreen.ActionType

enum class ErrorId {
    TOO_FAR,
    OCCUPIED_BY_ALLY,
    NOT_IMPLEMENTED,
    INVALID_TARGET,
    UNKNOWN_ERROR,
    HEX_OCCUPIED,
    AI_NO_VALID_MOVE,
    AI_NO_TARGET,
    AI_NO_ENEMIES,
    AI_NO_CLOSEST_ENEMY,
    SHOOTING_BLOCKED_BY_ENEMY,
}


data class BattleActionResult(
    val actionType: ActionType,
    val success: Boolean,
    val movedFrom: IBattleTile? = null,
    val movedTo: IBattleTile? = null,
    val errorId: ErrorId? = null,
    val isLuck: Boolean = false,
    val isMorale: Boolean = false,
    val battleEnded: Boolean = false // Флаг завершения битвы
)

