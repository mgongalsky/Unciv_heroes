package com.unciv.pure.domain.city

/** Слот участника карты. Постоянный ID не зависит от нации, порядка игроков и сетевого аккаунта. */
data class ScenarioPlayerSlot(
    var id: String = "",
    var name: String = ""
)

/** Начальная принадлежность города: нейтральный либо назначенный слоту участника. */
enum class ScenarioCityOwner { Neutral, PlayerSlot }
/** Способ определить постоянный тип города при старте: явно или по начальной нации владельца. */
enum class ScenarioCityTypeMode { Fixed, FromStartingPlayer }

/** Начальный стек гарнизона в файле карты; не живое войско с состоянием тактического боя. */
data class ScenarioCityTroop(
    var unitName: String = "",
    var amount: Int = 1
)

/**
 * Исходное описание города для редактора и повторного запуска миссии.
 * Чтение этих данных не создаёт CityInfo и не выполняет основание; игровые объекты создаются на старте.
 * Изменения города в партии не должны менять это описание исходной карты.
 */
data class ScenarioCity(
    var id: String = "",
    var position: CityPosition = CityPosition(0f, 0f),
    var name: String = "",
    var owner: ScenarioCityOwner = ScenarioCityOwner.Neutral,
    var ownerSlotId: String = "",
    var population: Int = 1,
    var foodStored: Int = 0,
    var buildings: ArrayList<String> = arrayListOf(),
    var garrison: ArrayList<ScenarioCityTroop> = arrayListOf(),
    /** Пустой список означает только центр; явная территория обязана содержать центр. */
    var territory: ArrayList<CityPosition> = arrayListOf(),
    /** Предпочтение автора карты; единственную столицу выбирает ScenarioCityCapitals. */
    var isCapital: Boolean = false,
    /** Совместимость со старыми описаниями; редактор не требует второй столичной галочки. */
    var isOriginalCapital: Boolean = false,
    var typeMode: ScenarioCityTypeMode = ScenarioCityTypeMode.FromStartingPlayer,
    /** Непрозрачный ключ типа, а не ссылка на текущего владельца; разрешается на границе запуска. */
    var fixedType: String = ""
) {
    /** Отделяет списки и стеки черновика от исходной карты; координаты уже неизменяемы. */
    fun deepCopy(): ScenarioCity = copy(
        buildings = ArrayList(buildings),
        garrison = ArrayList(garrison.map { it.copy() }),
        territory = ArrayList(territory)
    )
}
