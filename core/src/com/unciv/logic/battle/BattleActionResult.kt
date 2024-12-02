import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.battlescreen.ActionType

enum class ErrorId {
    TOO_FAR,             // Целевая клетка слишком далеко
    OCCUPIED_BY_ALLY,    // Целевая клетка занята союзником
    NOT_IMPLEMENTED,     // Функция ещё не реализована
    INVALID_TARGET,      // Цель недействительна
    UNKNOWN_ERROR        // Неизвестная ошибка
}


data class BattleActionResult(
    val actionType: ActionType,
    val success: Boolean,
    val errorId: ErrorId? = null, // Используем enum для ошибок
    val movedFrom: Vector2? = null,
    val movedTo: Vector2? = null
)

