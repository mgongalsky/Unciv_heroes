# Шаблон: изоляция игровой механики за интерфейсом

## Контекст

У нас есть игровая механика Supply — герой потребляет еду, армия разбегается если еда кончается. Мы хотим сделать механику модульной: можно включить/выключить одной строчкой, не трогая ничего вокруг.

---

## Принцип: Strategy Pattern + Null Object

Механика — это объект. Есть два варианта: настоящий и пустышка. Код вокруг не знает какой именно подставлен.

---

## Шаг 1: Найти все места где механика используется

Запускаем поиск по ключевым словам:

```powershell
Get-ChildItem -Recurse -Include *.kt |
Select-String -Pattern "currentFood|addFood|foodMaintenance" |
Select-Object Filename, LineNumber, Line
```

Смотрим что нашли и делим на категории:

```
Домен/логика:   MapUnitEndTurnUseCase, CityInfo
UI:             HeroSupplyTable, HeroOverviewScreen, CityStatsTable
Тесты:          MapUnitEndTurnUseCaseCharTest
```

---

## Шаг 2: Определить границы механики

Задаём вопрос: **что делает эта механика?**

Для Supply:
```
- потребляет еду каждый ход
- проверяет голодает ли герой
- распускает армию если голодает
```

Это и будет контракт интерфейса.

---

## Шаг 3: Создать интерфейс в домене

```kotlin
// pure/domain/supply/SupplyMechanic.kt
interface SupplyMechanic {
    val isEnabled: Boolean
    fun consumeFood(hero: HeroWithSupply, army: ArmyInfo)
    fun isStarving(hero: HeroWithSupply): Boolean
    fun onStarving(hero: HeroWithSupply, army: ArmyInfo)
}
```

Правила:
- Интерфейс живёт в **домене** — `pure/domain/`
- Принимает **роли** (`HeroWithSupply`) а не конкретные классы
- `isEnabled` — флаг для UI, чтобы скрывать элементы

---

## Шаг 4: Две реализации

**Настоящая механика:**
```kotlin
// pure/domain/supply/RealSupplyMechanic.kt
class RealSupplyMechanic : SupplyMechanic {
    override val isEnabled = true

    override fun consumeFood(hero: HeroWithSupply, army: ArmyInfo) {
        hero.addFood(-army.calculateFoodMaintenance(isInCity = false))
    }

    override fun isStarving(hero: HeroWithSupply) = hero.currentFood <= 0f

    override fun onStarving(hero: HeroWithSupply, army: ArmyInfo) {
        army.dismissByMostMaintenance()
    }
}
```

**Пустышка — Null Object:**
```kotlin
// pure/domain/supply/NoSupplyMechanic.kt
class NoSupplyMechanic : SupplyMechanic {
    override val isEnabled = false
    override fun consumeFood(hero: HeroWithSupply, army: ArmyInfo) = Unit
    override fun isStarving(hero: HeroWithSupply) = false
    override fun onStarving(hero: HeroWithSupply, army: ArmyInfo) = Unit
}
```

Правила:
- `NoSupplyMechanic` ничего не делает — **не кидает исключений, не возвращает null**
- `isEnabled = false` — UI будет знать что прятать
- Обе реализации живут рядом с интерфейсом в домене

---

## Шаг 5: Зарегистрировать в Koin

```kotlin
// GameInfo.loadRulesetForMap()
loadKoinModules(module {
    single { ruleSet }
    single<SupplyMechanic> { RealSupplyMechanic() }
    // Чтобы отключить — заменить на:
    // single<SupplyMechanic> { NoSupplyMechanic() }
})
```

**Одна строчка меняет всё поведение игры.**

---

## Шаг 6: Подключить к Use Case

Механика приходит как параметр — Use Case не знает какая реализация подставлена:

```kotlin
object MapUnitEndTurnUseCase {
    fun execute(
        unit: MapUnit,
        supplyMechanic: SupplyMechanic, // параметр
        // остальные параметры...
    ) {
        if (!unit.isMonster) {
            supplyMechanic.consumeFood(unit.hero, unit.army)
            if (supplyMechanic.isStarving(unit.hero)) {
                supplyMechanic.onStarving(unit.hero, unit.army)
            }
        }
        // остальная логика...
    }
}
```

**MapUnit** получает механику через Koin и передаёт в Use Case:

```kotlin
class MapUnit : KoinComponent {
    @delegate:Transient
    private val supplyMechanic: SupplyMechanic by inject()

    fun endTurn() {
        MapUnitEndTurnUseCase.execute(
            unit = this,
            supplyMechanic = supplyMechanic,
            // остальное...
        )
    }
}
```

---

## Шаг 7: Скрыть UI когда механика отключена

Классы UI получают механику через Koin и проверяют `isEnabled`:

```kotlin
class CityStatsTable(...) : KoinComponent {
    private val supplyMechanic: SupplyMechanic by inject()

    private fun addHeroSupplyInfo() {
        if (!supplyMechanic.isEnabled) return // скрываем всю таблицу
        val expanderTab = HeroSupplyTable(cityScreen).asExpander { onContentResize() }
        // остальное...
    }
}

class HeroOverviewScreen(...) : KoinComponent {
    private val supplyMechanic: SupplyMechanic by inject()

    // в месте где показывается еда:
    if (supplyMechanic.isEnabled) {
        // показываем блок с едой
    }
}
```

---

## Шаг 8: Починить тесты

Тесты передают механику явно — не через Koin:

```kotlin
// Тест с реальной механикой
MapUnitEndTurnUseCase.execute(
    unit = unit,
    supplyMechanic = RealSupplyMechanic(),
    // остальное...
)

// Тест где механика не важна — пустышка
MapUnitEndTurnUseCase.execute(
    unit = unit,
    supplyMechanic = NoSupplyMechanic(),
    // остальное...
)
```

---

## Итоговая структура

```
pure/domain/supply/
    SupplyMechanic.kt       ← интерфейс
    RealSupplyMechanic.kt   ← настоящая механика
    NoSupplyMechanic.kt     ← пустышка

GameInfo.kt                 ← регистрация в Koin (одна строчка)

MapUnitEndTurnUseCase.kt    ← принимает SupplyMechanic как параметр
MapUnit.kt                  ← инжектит и передаёт в Use Case

CityStatsTable.kt           ← if (!supplyMechanic.isEnabled) return
HeroOverviewScreen.kt       ← if (supplyMechanic.isEnabled) { ... }
```

---

## Как повторить для другой механики

```
1. Найти все места где механика используется (поиск по ключевым словам)
2. Определить что делает механика — это методы интерфейса
3. Создать интерфейс в pure/domain/
4. Написать RealMechanic и NoMechanic
5. Зарегистрировать в Koin
6. Use Case принимает интерфейс как параметр
7. MapUnit инжектит и передаёт
8. UI проверяет isEnabled
9. Тесты передают механику явно
```

Ключевое правило: **домен не знает какая реализация подставлена. Koin знает.**
