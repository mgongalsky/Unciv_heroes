# Отчёт о рефакторинге Heroic Civs — BattleManager и доменные классы

---

## Контекст и цель

Стартовая точка — монолитный `BattleManager` работающий напрямую с `TroopInfo`, который тянул за собой LibGDX, Koin, `CivilizationInfo`, `TileInfo` и рулсет. Цель — извлечь чистые доменные классы, изолировать логику за юзкейсами, наладить тестирование без графического контекста.

---

## Извлечение юзкейсов из BattleManager

### IsMoraleTriggeredUseCase / IsLuckTriggeredUseCase

Вынесли из `BattleManager.isMoraleTriggered` и `isLuckTriggered`. Ключевое решение — вместо `GameConstants.moraleProbability` (тянул GDX через lazy init) юзкейс принимает вероятность явно:

```kotlin
object IsMoraleTriggeredUseCase {
    fun execute(moraleValue: Int, random: IBattleRandom, moraleProbability: Double): Boolean
}
```

`BattleManager` передаёт константу сам — в тестах `TestableBattleManager` передаёт `0.0`.

Приём Физерса: **Parameterize Method** — убрали зависимость от GDX через параметризацию.

### GetBattleResultUseCase

Вынесли из `BattleManager.getBattleResult`. Вместо `BattleTotalResult(ArmyInfo)` возвращает чистый `Result(winnerIsAttacker: Boolean)` — юзкейс не знает об `ArmyInfo`:

```kotlin
object GetBattleResultUseCase {
    data class Result(val winnerIsAttacker: Boolean)
    fun execute(attackerArmy: IArmy, defenderArmy: IArmy): Result?
}
```

Для тестирования ввели `IArmy` интерфейс и `FakeArmy`.

### CalculateArmyFoodMaintenanceUseCase

Вынесли из `ArmyInfo.calculateFoodMaintenance`. Зависимость на `baseUnit.hasUnique(UniqueType.SelfFeeding)` убрали — добавили `isSelfFeeding: Boolean` в доменный `Troop`.

### TroopEntersBattleUseCase → initializeBattle

`TroopEntersBattleUseCase` был костылём. Удалили — логику расстановки войск поглотил новый `BattleManager.initializeBattle()` который стал единственной точкой входа в бой.

---

## Доменный класс Troop

Главный архитектурный шаг сессии — извлечение чистого доменного класса:

```kotlin
class Troop(
    val id: Int,
    val unitName: String,
    val amount: Int,
    val speed: Int,
    val damage: Int,
    val maxHealth: Int,
    val rangedStrength: Int,
    val isSelfFeeding: Boolean,
    currentAmount: Int,
    currentHealth: Int
) {
    var currentAmount: Int = currentAmount
    var currentHealth: Int = currentHealth
    val isRanged: Boolean get() = rangedStrength > 0
}
```

Никаких зависимостей на LibGDX, Koin, рулсет, цивилизацию, тайлы.

### ITroopDefinitionSource

Паттерн по аналогии с `IHeroDefinitionSource`:

```kotlin
interface ITroopDefinitionSource {
    fun getSpeed(unitName: String): Int
    fun getDamage(unitName: String): Int
    fun getMaxHealth(unitName: String): Int
    fun getRangedStrength(unitName: String): Int
    fun isSelfFeeding(unitName: String): Boolean
}
```

Две реализации:
- `RulesetTroopDefinitionSource` — продакшн, берёт из рулсета
- `HardcodedTroopDefinitionSource` — тесты, хардкод

### TroopFactory

```kotlin
object TroopFactory {
    private var nextId = 1
    fun resetIdCounter() { nextId = 1 } // Seam for testing
    fun create(unitName: String, amount: Int, source: ITroopDefinitionSource): Troop
}
```

Глобальный счётчик id — войска существуют вне боя, id уникален на всю сессию.

### TroopInfo как обёртка

`TroopInfo` остался — он реализует `MovableUnit`, сериализуется через LibGDX Json, хранит `civInfo`, `currentTile`. Все доменные поля делегируются в `Troop` через геттеры/сеттеры:

```kotlin
var currentAmount: Int
    get() = troop.currentAmount
    set(value) { troop.currentAmount = value }

val speed: Int get() = troop.speed
val rangedStrength: Int get() = troop.rangedStrength
```

Приём Физерса: **Sprout Class + Wrap Method** — внешний код не заметил изменений.

---

## Доменный класс Army / IArmy

### IArmy

```kotlin
interface IArmy {
    fun getAllTroops(): Array<Troop?>
    fun contains(troop: Troop): Boolean
    fun removeTroop(troop: Troop): Boolean
}
```

### ArmyInfo → Array<Troop?>

Хранилище переключили с `Array<TroopInfo?>` на `Array<Troop?>`. Это потребовало:
- `ArmyInfo : KoinComponent` — инжектирует `ITroopDefinitionSource` через Koin
- `ITroopDefinitionSource` зарегистрирован в `gameModule` и `testModule`
- `contains(Troop)` и `removeTroop(Troop)` по `troop.id` вместо ссылочного сравнения
- `perish()` при удалении вынесен в `BattleManager` — `ArmyInfo` не знает о тайлах

Побочный эффект — нашли баг: `rangedStrength` всегда был 0 потому что `ArmyInfo` использовал дефолтный `HardcodedTroopDefinitionSource`. Тесты выявили это немедленно.

---

## Позиции войск в BattleManager

Ключевое архитектурное решение — `Troop` не знает где он находится. Позиции хранит `BattleManager`:

```kotlin
protected val troopPositions = mutableMapOf<Troop, IBattleTile>()

fun getTroopTile(troop: Troop): IBattleTile? = troopPositions[troop]

fun moveTroop(troop: Troop, tile: IBattleTile) {
    troopPositions[troop]?.clearTroop()
    troopPositions[troop] = tile
    tile.receiveTroop(troop)
}
```

`initializeBattle()` расставляет войска по начальным позициям и заполняет `troopPositions`. `BattleScreen.init` вызывает `manager.initializeBattle()` вместо старого `initializeTurnQueue()`.

`protected` на `troopPositions` — Seam для `TestableBattleManager`.

---

## TroopMovementAdapter

Проблема: `UnitMovementAlgorithms` требует `MovableUnit`, а `Troop` им не является. Решение — минимальный адаптер:

```kotlin
class TroopMovementAdapter(
    val troop: Troop,
    tile: TileInfo,
    val isEnemy: (Troop) -> Boolean = { false },
    civInfo0: CivilizationInfo
) : MovableUnit() {
    init {
        currentTile = tile
        currentMovement = troop.speed.toFloat()
        civInfo = civInfo0
    }
}
```

`BattleManager` создаёт адаптер через `makeAdapter(troop, tile)` — внешний код не знает об адаптере.

---

## IBattleTile

```kotlin
interface IBattleTile : INavigableTile {
    fun getTroop(): Troop?
    fun receiveTroop(troop: Troop)
    fun clearTroop()
}
```

`TileInfo` реализует `IBattleTile` — `troopUnit: Troop?` вместо старого `TroopInfo?`. `receiveTroop` просто присваивает `troopUnit = troop`.

---

## Инфраструктура тестирования

### FakeBattleTile

```kotlin
class FakeBattleTile(
    override val position: Vector2,
    private var troop: Troop? = null
) : IBattleTile {
    override fun getTroop(): Troop? = troop
    override fun receiveTroop(troop: Troop) { this.troop = troop }
    override fun clearTroop() { troop = null }
    fun setTroop(t: Troop?) { troop = t }
}
```

### FakeArmy

```kotlin
class FakeArmy(private val troops: List<Troop?>) : IArmy {
    override fun getAllTroops() = troops.toTypedArray()
    override fun contains(troop: Troop) = troops.any { it?.id == troop.id }
    override fun removeTroop(troop: Troop): Boolean { ... }
}
```

### TestableBattleManager

Наследует `BattleManager`, пишет напрямую в `protected troopPositions`:

```kotlin
class TestableBattleManager(...) : BattleManager(
    ..., moraleProbability = 0.0, luckProbability = 0.0
) {
    fun placeTroop(troop: Troop, tile: FakeBattleTile) {
        tile.setTroop(troop)
        troopPositions[troop] = tile
    }

    override fun isReachableInCurrentTurn(troop: Troop, targetTile: INavigableTile): Boolean =
        if (useRealMovement) { /* real pathfinding */ } else allTilesReachable
}
```

Приём Физерса: **Subclass and Override Method**.

### testModule

Единый файл зависимостей для всех тестов:

```kotlin
val testModule = module {
    single<ITroopDefinitionSource> { RulesetTroopDefinitionSource(get()) }
    // завтра: single<IArtifactDefinitionSource> { ... }
}
```

В тестах:
```kotlin
startKoin {
    allowOverride(true)
    modules(module { single { fakeRuleset } }, testModule)
}
```

### TDD и чартесты

- `IsMoraleTriggeredUseCase`, `IsLuckTriggeredUseCase` — TDD, три теста до реализации
- `GetBattleResultUseCase` — TDD
- `CalculateArmyFoodMaintenanceUseCase` — чартесты (10 сценариев), потом зашили в assertEquals
- `ArcherRangedStrengthTest` — послойное тестирование: ruleset → source → factory → army
- `ArcherAttackTest` — регрессионный тест на баг AI (стрелял в себя)

---

## Баги найденные тестами

1. **Лучники не стреляли** — `ArmyInfo` использовал дефолтный `HardcodedTroopDefinitionSource(rangedStrength=0)` вместо рулсета. Нашли послойными тестами — упал Layer 4 (ArmyInfo), Layer 3 (TroopFactory) был зелёным.

2. **AI стрелял в себя** — `performRangedAction` передавал `currentTile` вместо `targetTile` в `BattleActionRequest`. Регрессионный тест зафиксировал исправление.

3. **troopPositions не обновлялся** — `moveTroop` не синхронизировал `troopPositions`. Войска визуально не двигались.

4. **Food maintenance считал неверно** — `addUnits` обновлял `currentAmount` но юзкейс читал `amount`. Починили используя `currentAmount`.

---

## Приёмы Физерса применённые в сессии

| Приём | Где применён |
|-------|-------------|
| Sprout Class | `Troop` из `TroopInfo` |
| Wrap Method | геттеры/сеттеры в `TroopInfo` делегируют в `Troop` |
| Extract Interface | `IArmy`, `IBattleTile`, `ITroopDefinitionSource` |
| Parameterize Method | `moraleProbability`/`luckProbability` в конструктор `BattleManager` |
| Subclass and Override | `TestableBattleManager` переопределяет `isReachableInCurrentTurn` |
| Extract and Override Factory Method | `TroopFactory`, `HardcodedTroopDefinitionSource` |
| Static Setter Seam | `TroopFactory.resetIdCounter()` |
