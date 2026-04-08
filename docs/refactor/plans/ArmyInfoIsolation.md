# План дальнейшего рефакторинга — ArmyInfo и BattleManager

---

## Текущее состояние

**Что уже чисто:**
- `Troop` — чистый доменный класс без зависимостей
- `TroopFactory` + `ITroopDefinitionSource` — полиморфизм через Koin
- `IArmy` — интерфейс, `ArmyInfo` реализует
- `troopPositions` в `BattleManager` — позиции отделены от домена
- Юзкейсы: `IsMoraleTriggeredUseCase`, `IsLuckTriggeredUseCase`, `GetBattleResultUseCase`, `CalculateArmyFoodMaintenanceUseCase`

**Что ещё грязно:**
- `BattleManager` — монолит, вся логика внутри одного класса
- `ArmyInfo` — тянет `GameConstants.armySize` через GDX, `hero: MapUnit?` вместо `Hero`
- Очередь ходов разбросана по `BattleManager` — `turnQueue`, `currentTurnIndex`, `removeTroop`, `advanceTurn`
- `performTurn` — огромный метод, всё в одном месте

---

## Шаг 1 — TurnQueue как отдельный доменный класс

### Почему сейчас

Очередь ходов — самостоятельная концепция. Сейчас `turnQueue: MutableList<Troop>` и `currentTurnIndex: Int` разбросаны по `BattleManager`, а методы `advanceTurn`, `removeTroop`, `initializeTurnQueue` манипулируют ими напрямую.

### Что делаем

```kotlin
// com.unciv.pure.domain.battle.TurnQueue
class TurnQueue {
    private val queue: MutableList<Troop> = mutableListOf()
    private var currentIndex: Int = 0

    fun initialize(attackerTroops: List<Troop>, defenderTroops: List<Troop>)
    fun current(): Troop?
    fun advance()
    fun remove(troop: Troop)
    fun isEmpty(): Boolean
    fun getAll(): List<Troop>
    fun size(): Int
}
```

### Как делаем — по Физерсу

1. Characterization тесты на текущее поведение `initializeTurnQueue`, `advanceTurn`, `removeTroop` — до рефакторинга
2. Создаём `TurnQueue` — verbatim перенос логики
3. `BattleManager` заменяет `turnQueue` + `currentTurnIndex` на `private val turnQueue = TurnQueue()`
4. TDD тесты на `TurnQueue` отдельно — без `BattleManager`

### Тесты на TurnQueue

```kotlin
// faster troop goes before slower
// equal speed — attacker before defender
// advance moves to next troop
// remove shifts index correctly when removing before current
// remove shifts index correctly when removing after current
// remove last troop — isEmpty returns true
// wraps around after last troop
```

---

## Шаг 2 — Extract Use Cases из performTurn

`performTurn` сейчас — огромный when с тремя ветками. Каждая ветка — отдельный юзкейс.

### PerformMoveUseCase

```kotlin
object PerformMoveUseCase {
    data class Input(
        val troop: Troop,
        val targetTile: IBattleTile,
        val currentTile: IBattleTile?,
        val isMorale: Boolean
    )
    data class Output(
        val success: Boolean,
        val errorId: ErrorId? = null,
        val movedFrom: IBattleTile? = null,
        val movedTo: IBattleTile? = null,
        val isMorale: Boolean = false
    )
    fun execute(input: Input): Output
}
```

### PerformAttackUseCase

```kotlin
object PerformAttackUseCase {
    data class Input(
        val attacker: Troop,
        val defender: Troop,
        val attackerCurrentTile: IBattleTile?,
        val attackTile: IBattleTile,
        val isMorale: Boolean,
        val isLuck: Boolean
    )
    data class Output(
        val success: Boolean,
        val errorId: ErrorId? = null,
        val movedFrom: IBattleTile? = null,
        val movedTo: IBattleTile? = null,
        val isLuck: Boolean = false,
        val isMorale: Boolean = false,
        val defenderRemainingAmount: Int = 0,
        val defenderDied: Boolean = false
    )
    fun execute(input: Input): Output
}
```

### PerformShootUseCase

Аналогично `PerformAttackUseCase` но без перемещения атакующего.

### Как делаем — по Физерсу

1. Characterization тесты на `performTurn` с каждым `ActionType` — до рефакторинга
2. Verbatim перенос — логику не меняем, просто перемещаем
3. `performTurn` делегирует в юзкейсы
4. TDD тесты на каждый юзкейс отдельно

---

## Шаг 3 — Army как чистый доменный класс

### Проблемы текущего ArmyInfo

- `GameConstants.armySize` — GDX зависимость в конструкторе
- `hero: MapUnit?` — должен быть `Hero`
- `finishBattle()` — координация сверху, не метод армии
- `dismissByMostMaintenance()` — юзкейс, не метод армии
- `clone()`/`copySlots()` — сомнительная ценность

### Что делаем

```kotlin
// com.unciv.pure.domain.army.Army
class Army(
    val maxSlots: Int,
    private val troops: Array<Troop?> = arrayOfNulls(maxSlots)
) {
    fun contains(troop: Troop): Boolean = troops.any { it?.id == troop.id }
    fun getAllTroops(): Array<Troop?> = troops
    fun addTroop(troop: Troop): Boolean
    fun removeTroop(troop: Troop): Boolean
    fun getTroopAt(index: Int): Troop?
    fun isEmpty(): Boolean = troops.all { it == null || it.currentAmount <= 0 }
    fun aliveTroops(): List<Troop> = troops.filterNotNull().filter { it.currentAmount > 0 }
}
```

### IArmyDefinitionSource

```kotlin
interface IArmyDefinitionSource {
    fun getMaxSlots(): Int
}

class HardcodedArmyDefinitionSource(private val slots: Int) : IArmyDefinitionSource {
    override fun getMaxSlots() = slots
}

class GameConstantsArmyDefinitionSource : IArmyDefinitionSource {
    override fun getMaxSlots() = GameConstants.armySize
}
```

`ArmyFactory`:
```kotlin
object ArmyFactory {
    fun create(source: IArmyDefinitionSource): Army = Army(source.getMaxSlots())
}
```

В `testModule` добавляем:
```kotlin
single<IArmyDefinitionSource> { HardcodedArmyDefinitionSource(7) }
```

В `gameModule`:
```kotlin
single<IArmyDefinitionSource> { GameConstantsArmyDefinitionSource() }
```

### ArmyInfo как обёртка

`ArmyInfo` делегирует в `Army`:

```kotlin
val maxSlots: Int get() = army.maxSlots
fun getAllTroops() = army.getAllTroops()
fun contains(troop: Troop) = army.contains(troop)
```

---

## Шаг 4 — Убрать MapUnit из TroopInfo и ArmyInfo

Сейчас:
```kotlin
// TroopInfo
@Transient var hero: MapUnit? = null

// ArmyInfo
@Transient var hero: MapUnit? = null
```

Должно быть `Hero` из нашего доменного класса. Но `MapUnit` сам постепенно превращается в `Hero` — это большой шаг.

### План

1. В `TroopInfo` добавляем `var hero: Hero?` рядом с `var legacyHero: MapUnit?`
2. `getHeroMorale()` читает из `hero?.morale ?: legacyHero?.morale ?: 0`
3. Постепенно переключаем всех кто читает hero на новый `hero: Hero?`
4. `legacyHero` удаляем когда никто не использует

---

## Шаг 5 — BattleManager как координатор

После извлечения юзкейсов `BattleManager` становится тонким координатором:

```kotlin
fun performTurn(request: BattleActionRequest): BattleActionResult {
    val isMorale = IsMoraleTriggeredUseCase.execute(...)

    return when (request.actionType) {
        MOVE -> {
            val output = PerformMoveUseCase.execute(...)
            onEvent?.invoke(BattleEvent.TroopMoved(...))
            output.toBattleActionResult()
        }
        ATTACK -> {
            val output = PerformAttackUseCase.execute(...)
            onEvent?.invoke(BattleEvent.TroopAttacked(...))
            output.toBattleActionResult()
        }
        SHOOT -> { ... }
    }
}
```

---

## Шаг 6 — Система событий (из плана рефакторинга)

После того как юзкейсы изолированы — вводим события:

```kotlin
sealed class BattleEvent {
    data class TroopMoved(val troopId: Int, val from: Vector2, val to: Vector2, val isMorale: Boolean) : BattleEvent()
    data class TroopAttacked(val attackerId: Int, val defenderId: Int, val defenderRemainingAmount: Int, val isLuck: Boolean, val isMorale: Boolean, val defenderDied: Boolean) : BattleEvent()
    data class TroopShot(...) : BattleEvent()
    data class TurnAdvanced(val nextTroopId: Int, val currentPosition: Vector2, val reachablePositions: List<Vector2>) : BattleEvent()
    data class BattleEnded(val winnerIsAttacker: Boolean) : BattleEvent()
    object TurnSkipped : BattleEvent()
}
```

`IBattleManager` добавляем `var onEvent: ((BattleEvent) -> Unit)?`

---

## Порядок выполнения

| Шаг | Что | Риск | Тесты |
|-----|-----|------|-------|
| 1 | `TurnQueue` | низкий | TDD на `TurnQueue` отдельно |
| 2 | `PerformMoveUseCase` | низкий — verbatim | чартест до + после |
| 3 | `PerformAttackUseCase` | низкий — verbatim | чартест до + после |
| 4 | `PerformShootUseCase` | низкий — verbatim | чартест до + после |
| 5 | `Army` доменный класс | средний | TDD на `Army` |
| 6 | `IArmyDefinitionSource` | низкий | тесты без GDX |
| 7 | `hero: Hero?` в `TroopInfo` | средний | чартест до |
| 8 | `BattleEvent` система | средний | `FakeBattleManager` |
| 9 | `BattlePresenter` | средний | `FakeBattleView` |

---

## Главные принципы на следующую сессию

- **Characterization тест перед каждым извлечением** — фиксируем реальность, потом двигаемся
- **Verbatim перенос** — не меняем логику во время экстракции
- **testModule** — все новые зависимости добавляем только туда
- **TurnQueue** начинаем с TDD — это самое изолированное и чистое место для старта
