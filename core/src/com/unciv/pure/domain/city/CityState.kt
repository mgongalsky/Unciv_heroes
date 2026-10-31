package com.unciv.pure.domain.city

import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.army.deepCopy

/** Постоянный идентификатор города: назначается миром или сценарием и не меняется при передаче владельцу. */
data class CityId(val value: String)

/** Неизменяемые координаты клетки без зависимости от LibGDX и объектов игровой карты. */
data class CityPosition(val x: Float = 0f, val y: Float = 0f)

/** Владение городом. Нейтральность — самостоятельное состояние, а не скрытая цивилизация. */
sealed class CityOwnership {
    object Neutral : CityOwnership()
    /** В текущем игровом адаптере playerId содержит civName, а не ID сетевого аккаунта или слота карты. */
    data class Player(val playerId: String) : CityOwnership()
}

/** История основания и последнего приобретения; сохраняется независимо от текущего владельца. */
data class CityHistory(
    val foundingPlayerId: String = "",
    val previousPlayerId: String = "",
    val turnAcquired: Int = 0
)

/**
 * Независимая часть состояния игрового города.
 * CityInfo обращается к этим данным через свойства-адаптеры, не хранит их вторую изменяемую копию.
 * Здесь нет игровых сервисов: население, производство и другие legacy-менеджеры пока остаются снаружи.
 */
data class CityState(
    var id: CityId,
    var name: String = "",
    var history: CityHistory = CityHistory(),
    var health: Int = 200,
    var garrison: Army? = null,
    /** null означает, что привязка ещё не восстановлена; это НЕ нейтральный город. */
    var ownership: CityOwnership? = null,
    var position: CityPosition = CityPosition(0f, 0f),
    /** null встречается в старых сохранениях: статус однократно восстанавливается по зданиям. */
    var isCurrentCapital: Boolean? = null,
    /** Тип определяется при запуске миссии и сохраняется при смене владельца. */
    var resolvedCityType: String = ""
) {
    /** Глубокий снимок изменяемой армии, включая войска и строй; обычный copy() этого не делает. */
    fun deepCopy(): CityState = copy(garrison = garrison?.deepCopy())
}
