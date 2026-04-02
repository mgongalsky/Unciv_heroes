# Шаблон: изоляция алгоритма из legacy кода

## Контекст

У нас был алгоритм поиска достижимых тайлов `getDistanceToTilesWithinTurn` в классе `UnitMovementAlgorithms`. Он работал только с конкретными классами `TileInfo`, `CivilizationInfo`, `MapUnit` — тестировать без поднятия всей игры было невозможно.

Цель: вынести алгоритм в чистый Use Case, тестируемый без LibGDX, без TileInfo, без Koin.

---

## Принцип: Физерс — маленькими шагами

Каждый шаг компилируется и работает. Никогда не ломаем старый код. Никогда не делаем глобальный рефакторинг за один раз.

---

## Шаг 1: Найти зависимости алгоритма

Смотрим что алгоритм использует от внешнего мира:

```kotlin
// Зависимости которые нашли:
tileToCheck.neighbors          // TileInfo — соседи
unit.civInfo.hasExplored()     // CivilizationInfo — исследован ли тайл
unit.movement.canPassThrough() // UnitMovementAlgorithms — можно ли пройти
unit.movement.getMovementCost() // UnitMovementAlgorithms — стоимость перехода
unit is TroopInfo              // проверка типа — костыль для битвы
unit is MapUnit                // проверка типа — костыль для карты
unit.currentTile               // стартовый тайл
unit.currentTile.tileMap       // карта для поиска тайла по координатам
```

**Правило:** всё что алгоритм использует извне — это зависимость которую надо изолировать.

---

## Шаг 2: Extract Method для сложных зависимостей

Сначала выносим сложные куски в отдельные методы — прямо в том же классе. Это Физерс Extract Method.

```kotlin
// Было прямо в алгоритме:
val unitTile = if (origin == unit.currentTile.position) unit.currentTile
               else unit.currentTile.tileMap[origin]

// Стало:
private fun getStartTile(origin: Vector2): TileInfo {
    val currentUnitTile = unit.currentTile
    return if (origin == currentUnitTile.position) currentUnitTile
           else currentUnitTile.tileMap[origin]
}
```

Алгоритм теперь просто вызывает `getStartTile(origin)`. Логика поиска стартового тайла изолирована и не мешает дальнейшему рефакторингу.

---

## Шаг 3: IMovementContext — вынести правила движения за интерфейс

Две функции (`canPassThrough` и `getMovementCost`) — это **правила движения**. Они зависят от юнита и цивилизации. Выносим их за интерфейс.

```kotlin
// pure/domain/pathfinding/IMovementContext.kt
interface IMovementContext {
    fun canPassThrough(tile: INavigableTile): Boolean
    fun getMovementCost(from: INavigableTile, to: INavigableTile): Float
    fun hasExplored(tile: INavigableTile): Boolean
    fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?): Boolean
}
```

Добавляем параметр с дефолтом — старый код не замечает:

```kotlin
fun getDistanceToTilesWithinTurn(
    ...
    context: IMovementContext = HeroMovementContext(unit) // дефолт!
)
```

**Ключевой приём: дефолтный параметр.** Старый код вызывает функцию без `context` — получает `HeroMovementContext` автоматически. Новый код передаёт любой контекст.

---

## Шаг 4: Убрать костыли через контекст

Были проверки типа прямо в алгоритме:

```kotlin
// Костыли — алгоритм знает про конкретные типы
if (unit is TroopInfo && neighbor.troopUnit != null ...) continue
unit is MapUnit && !unit.civInfo.hasExplored(neighbor)
unit is MapUnit && !canPassThrough(neighbor)
```

Переносим их в реализации контекста:

```kotlin
// Для карты приключений
class HeroMovementContext(private val unit: MovableUnit) : IMovementContext {
    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) = false
    override fun hasExplored(tile: INavigableTile) = unit.civInfo.hasExplored(tile as TileInfo)
}

// Для битвы
class TroopMovementContext(private val unit: TroopInfo) : IMovementContext {
    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) =
        (tile as TileInfo).troopUnit != null && tile.troopUnit != unit && tile != targetTile
    override fun hasExplored(tile: INavigableTile) = true // в битве всё видно
    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = 1f // в битве всё стоит 1
}
```

Алгоритм теперь не знает где он — на карте или в битве.

---

## Шаг 5: INavigableTile — абстракция для тайлов

Алгоритм использует `tileToCheck.neighbors` — это `Sequence<TileInfo>`. Нужна абстракция.

```kotlin
// pure/domain/pathfinding/INavigableTile.kt
interface INavigableTile {
    val neighbors: Sequence<INavigableTile>
    val position: Vector2
}
```

`TileInfo` реализует интерфейс — **минимальные изменения**:

```kotlin
open class TileInfo : IsPartOfGameInfoSerialization, INavigableTile {
    // Существующий код не трогаем
    // neighbors уже есть — просто добавляем override

    // Важно: lazy кеш сохраняем для производительности
    @delegate:Transient
    private val _neighbors by lazy { getTilesAtDistance(1).toList() }
    override val neighbors: Sequence<INavigableTile>
        get() = _neighbors.asSequence()

    override var position: Vector2 = Vector2.Zero // добавляем override
}
```

---

## Шаг 6: Generic типы для PathsToTilesWithinTurn

`PathsToTilesWithinTurn` и `ParentTileAndTotalDistance` были завязаны на `TileInfo`. Делаем generic:

```kotlin
// pure/domain/pathfinding/ParentTileAndTotalDistance.kt
class ParentTileAndTotalDistance<T : INavigableTile>(
    val parentTile: T,
    val totalDistance: Float
)

// pure/domain/pathfinding/PathsToTilesWithinTurn.kt
class PathsToTilesWithinTurn<T : INavigableTile> : LinkedHashMap<T, ParentTileAndTotalDistance<T>>() {
    fun getPathToTile(tile: T): List<T> {
        if (!containsKey(tile)) throw Exception("Can't reach this tile!")
        val reversePathList = ArrayList<T>()
        var currentTile = tile
        while (get(currentTile)!!.parentTile != currentTile) {
            reversePathList.add(currentTile)
            currentTile = get(currentTile)!!.parentTile
        }
        return reversePathList.reversed()
    }
}
```

`<T>` — переменная типа. В проде `T = TileInfo`, в тестах `T = FakeNavigableTile`.

Старый код просто добавляет `<TileInfo>` везде — поведение не меняется:
```kotlin
val distanceToTiles = PathsToTilesWithinTurn<TileInfo>()
```

---

## Шаг 7: Вынести алгоритм в Use Case

Теперь алгоритм зависит только от абстракций — можно выносить:

```kotlin
// pure/application/pathfinding/MovementRangeUseCase.kt
object MovementRangeUseCase {

    fun <T : INavigableTile> execute(
        startTile: T,
        unitMovement: Float,
        context: IMovementContext,
        tilesToIgnore: HashSet<T>? = null,
        targetTile: T? = null
    ): PathsToTilesWithinTurn<T> {
        val distanceToTiles = PathsToTilesWithinTurn<T>()
        if (unitMovement == 0f) return distanceToTiles

        distanceToTiles[startTile] = ParentTileAndTotalDistance(startTile, 0f)
        var tilesToCheck = listOf(startTile)

        while (tilesToCheck.isNotEmpty()) {
            val updatedTiles = ArrayList<T>()
            for (tileToCheck in tilesToCheck)
                for (neighbor in tileToCheck.neighbors) {
                    @Suppress("UNCHECKED_CAST")
                    neighbor as T
                    if (tilesToIgnore?.contains(neighbor) == true) continue
                    if (context.shouldSkipTile(neighbor, targetTile)) continue

                    val totalDistanceToTile: Float = when {
                        !context.hasExplored(neighbor) ->
                            distanceToTiles[tileToCheck]!!.totalDistance + 1f
                        !context.canPassThrough(neighbor) -> unitMovement
                        else -> {
                            val cost = context.getMovementCost(tileToCheck, neighbor)
                            distanceToTiles[tileToCheck]!!.totalDistance + cost
                        }
                    }

                    if (!distanceToTiles.containsKey(neighbor) ||
                        distanceToTiles[neighbor]!!.totalDistance > totalDistanceToTile) {
                        if (totalDistanceToTile < unitMovement)
                            updatedTiles += neighbor
                        distanceToTiles[neighbor] = ParentTileAndTotalDistance(tileToCheck, totalDistanceToTile)
                    }
                }
            tilesToCheck = updatedTiles
        }
        return distanceToTiles
    }
}
```

Старая функция **делегирует** — одна строчка:

```kotlin
fun getDistanceToTilesWithinTurn(...): PathsToTilesWithinTurn<TileInfo> {
    return MovementRangeUseCase.execute(
        startTile = getStartTile(origin),
        unitMovement = unitMovement,
        context = context,
        tilesToIgnore = tilesToIgnore,
        targetTile = targetTile
    )
}
```

---

## Шаг 8: Фейки и тесты

```kotlin
// FakeNavigableTile — легковесный тайл для тестов
class FakeNavigableTile(
    val name: String = "",
    override val position: Vector2 = Vector2.Zero,
    private val neighborList: MutableList<FakeNavigableTile> = mutableListOf()
) : INavigableTile {
    override val neighbors: Sequence<INavigableTile>
        get() = neighborList.asSequence()

    fun addNeighbor(tile: FakeNavigableTile) {
        if (!neighborList.contains(tile)) neighborList.add(tile)
        if (!tile.neighborList.contains(this)) tile.neighborList.add(this)
    }
}

// FakeMovementContext — подменяемые правила движения
open class FakeMovementContext(
    private val cost: Float = 1f,
    private val passable: Boolean = true,
    private val explored: Boolean = true
) : IMovementContext {
    override fun canPassThrough(tile: INavigableTile) = passable
    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = cost
    override fun hasExplored(tile: INavigableTile) = explored
    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) = false
}
```

Тест — никакого LibGDX, никакого TileInfo:

```kotlin
@Test
fun `impassable tile blocks path`() {
    val tiles = FakeNavigableTileFactory.createChain(4)
    val wall = tiles[1]

    val context = object : FakeMovementContext() {
        override fun canPassThrough(tile: INavigableTile) =
            (tile as FakeNavigableTile).name != wall.name
    }

    val result = MovementRangeUseCase.execute(tiles[0], 5f, context)

    assertFalse(result.containsKey(tiles[2]))
}
```

---

## Итоговая структура

```
pure/domain/pathfinding/
    INavigableTile.kt              ← абстракция тайла
    IMovementContext.kt            ← абстракция правил движения
    ParentTileAndTotalDistance.kt  ← generic, результат алгоритма
    PathsToTilesWithinTurn.kt      ← generic, результат алгоритма

pure/application/pathfinding/
    MovementRangeUseCase.kt        ← чистый алгоритм
    HeroMovementContext.kt         ← правила для карты приключений
    TroopMovementContext.kt        ← правила для битвы

logic/map/
    UnitMovementAlgorithms.kt      ← делегирует к Use Case
    TileInfo.kt                    ← реализует INavigableTile

testing/pure/fakes/
    FakeNavigableTile.kt           ← фейк для тестов
    FakeMovementContext.kt         ← фейк контекста
    FakeNavigableTileFactory.kt    ← фабрика тестовых карт
```

---

## Ключевые приёмы

```
1. Дефолтные параметры       — старый код не замечает новых параметров
2. Extract Method            — изолируй сложные куски до выноса алгоритма
3. Interface + default impl  — вынеси правила за интерфейс, дай дефолт
4. Generic типы              — один класс для разных реализаций
5. Делегирование             — старая функция вызывает новый Use Case
6. Фейки без наследования    — реализуй интерфейс, не наследуй TileInfo
7. Чартесты первыми          — зафиксируй поведение до изменений
```
