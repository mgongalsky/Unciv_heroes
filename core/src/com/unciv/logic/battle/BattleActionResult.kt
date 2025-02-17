import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.map.TileInfo
import com.unciv.ui.battlescreen.ActionType

enum class ErrorId {
    TOO_FAR,             // Целевая клетка слишком далеко
    OCCUPIED_BY_ALLY,    // Целевая клетка занята союзником
    NOT_IMPLEMENTED,     // Функция ещё не реализована
    INVALID_TARGET,      // Цель недействительна
    UNKNOWN_ERROR,        // Неизвестная ошибка
    HEX_OCCUPIED,
    AI_NO_VALID_MOVE,
    AI_NO_TARGET,
    AI_NO_ENEMIES
}


data class BattleActionResult(
    val actionType: ActionType,
    val success: Boolean,
    val movedFrom: TileInfo? = null,
    val movedTo: TileInfo? = null,
    val errorId: ErrorId? = null,
    val isLuck: Boolean = false,
    val isMorale: Boolean = false,
    val battleEnded: Boolean = false // Флаг завершения битвы
)

