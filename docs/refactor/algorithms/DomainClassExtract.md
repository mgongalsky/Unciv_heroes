# Шаблон рефакторинга: Sprout Class по Физерсу + Clean Architecture

## Контекст

У нас есть монструозный legacy класс `MapUnit` (~1500 строк). Он делает всё сразу: хранит состояние героя, управляет армией, содержит логику механик, сериализуется, работает с UI. Менять его страшно — всё завязано на всё.

Задача: **выделить чистый доменный класс Hero, неломая ничего снаружи.**

---

## Принцип: Sprout Class (Физерс)

Вместо того чтобы рефакторить `MapUnit` изнутри — **сажаем росток рядом**. Новый чистый класс растёт параллельно. `MapUnit` постепенно делегирует к нему. Внешний код не замечает подмены.

Правило одно: **на этапе посадки ростка — никакой новой логики. Только перенос существующей.**

---

## Шаг 1: Определяем границы домена

Смотрим на `MapUnit` и задаём вопрос: **что из этого принадлежит герою как концепции, а не инфраструктуре?**

```
✅ Домен героя:
   heroAttackSkill, heroDefenseSkill  — характеристики
   currentFood, basicFoodCapacity     — состояние снабжения
   morale, luck                       — характеристики

❌ Не домен:
   civInfo                            — инфраструктура цивилизации
   baseUnit                           — привязка к ruleset
   army                               — отдельная сущность
   heal(), fortify()                  — механики Civ, не наши
```

**Ключевой вопрос:** "Если бы я описывал героя в книге правил настольной игры — что бы я написал?" Это и есть домен.

---

## Шаг 2: Создаём чистый доменный класс

Требования к доменному классу:
- **Ноль внешних зависимостей** — никакого LibGDX, Koin, Ruleset
- **Только Kotlin** — компилируется и тестируется без поднятия игры
- **Инкапсуляция** — состояние меняется только через методы

```kotlin
// pure/domain/hero/Hero.kt
class Hero(
    val baseAttackSkill: Int,
    val baseDefenseSkill: Int,
    override val baseFoodCapacity: Float,
    currentFood: Float,
    morale: Int,
    luck: Int
) : HeroWithSupply, HeroWithSettle {

    override var currentFood: Float = currentFood
        private set  // снаружи только чтение, внутри через методы

    var morale: Int = morale
    var luck: Int = luck

    override fun addFood(amount: Float) { currentFood += amount }
    override fun setFood(amount: Float) { currentFood = amount }

    override fun canSettle(): Boolean {
        TODO("Not yet implemented")
    }
}
```

`private set` — важная деталь. Говорит: "прочитать можно, изменить — только через `addFood` или `setFood`". Это защищает инварианты объекта.

---

## Шаг 3: Role Interfaces — модульность механик

Проблема: если добавить все поля механик в один класс — Hero становится монолитом. Если разбить на отдельные классы — взрыв комбинаций при наследовании.

Решение: **один класс Hero, несколько интерфейсов-ролей**.

```kotlin
// pure/domain/hero/HeroWithSupply.kt
interface HeroWithSupply {
    val currentFood: Float
    val baseFoodCapacity: Float
    fun addFood(amount: Float)
    fun setFood(amount: Float)
}

// pure/domain/hero/HeroWithSettle.kt
interface HeroWithSettle {
    fun canSettle(): Boolean
}
```

Use Case принимает **только нужную роль**, а не весь Hero:

```kotlin
class HeroEndTurnUseCase(private val supply: SupplyMechanic) {
    fun execute(hero: HeroWithSupply, army: ArmyInfo) {
        supply.consumeFood(hero, army)
    }
}
```

Завтра добавим механику магии — пишем `HeroWithMagic`, Hero реализует его. Use Cases не трогаем.

---

## Шаг 4: Port — отделяем домен от инфраструктуры

Проблема: Hero нужны начальные значения из Ruleset (JSON файл). Но Ruleset — это инфраструктура. Домен не должен знать про JSON.

Решение: **Port (интерфейс в домене) + Adapter (реализация в инфраструктуре)**.

```kotlin
// pure/domain/hero/IHeroDefinitionSource.kt
// Домен объявляет ЧТО ему нужно — не знает ОТКУДА
interface IHeroDefinitionSource {
    fun getAttackSkill(unitName: String): Int
    fun getDefenseSkill(unitName: String): Int
    fun getFoodCapacity(unitName: String): Float
}
```

Две реализации в инфраструктуре:

```kotlin
// infrastructure/hero/RulesetHeroDefinitionSource.kt
// Знает про JSON и Ruleset
class RulesetHeroDefinitionSource(
    private val ruleset: Ruleset
) : IHeroDefinitionSource {
    override fun getAttackSkill(unitName: String) =
        ruleset.units[unitName]?.strength ?: 5
    override fun getDefenseSkill(unitName: String) =
        ruleset.units[unitName]?.rangedStrength ?: 5
    override fun getFoodCapacity(unitName: String) = 15f
}

// infrastructure/hero/HardcodedHeroDefinitionSource.kt
// Для тестов и простой инициализации
class HardcodedHeroDefinitionSource(
    private val attackSkill: Int,
    private val defenseSkill: Int,
    private val foodCapacity: Float = 15f
) : IHeroDefinitionSource {
    override fun getAttackSkill(unitName: String) = attackSkill
    override fun getDefenseSkill(unitName: String) = defenseSkill
    override fun getFoodCapacity(unitName: String) = foodCapacity
}
```

Завтра захочешь MongoDB — пишешь `MongoHeroDefinitionSource`. Домен не трогаешь.

---

## Шаг 5: Factory — изолируем логику создания

Создание Hero — нетривиальная операция: нужно взять данные из source, проставить дефолты. Это отдельная ответственность.

```kotlin
// pure/domain/hero/HeroFactory.kt
object HeroFactory {
    fun create(
        source: IHeroDefinitionSource,
        unitName: String,
        currentFood: Float = 3f,
        morale: Int = 3,
        luck: Int = 3
    ): Hero = Hero(
        baseAttackSkill = source.getAttackSkill(unitName),
        baseDefenseSkill = source.getDefenseSkill(unitName),
        baseFoodCapacity = source.getFoodCapacity(unitName),
        currentFood = currentFood,
        morale = morale,
        luck = luck
    )
}
```

Дефолты (`morale = 3`, `luck = 3`) живут в одном месте. Захочешь взять из ruleset — меняешь только фабрику.

---

## Шаг 6: Встраиваем Hero в MapUnit — шов

Это самое важное место. `MapUnit` остаётся как был — внешний код не замечает ничего.

```kotlin
class MapUnit {
    // Hero живёт внутри — внешний код не знает
    var hero: Hero = HeroFactory.create(
        source = HardcodedHeroDefinitionSource(5, 5),
        unitName = "default"
    )

    // setTransients — точка восстановления после десериализации
    // Hero пересоздаётся из ruleset, сохраняя текущее состояние
    fun setTransients(ruleset: Ruleset) {
        baseUnit = ruleset.units[name]
            ?: throw Exception("Unit $name is not found!")

        hero = HeroFactory.create(
            source = RulesetHeroDefinitionSource(ruleset),
            unitName = name,
            currentFood = hero.currentFood, // сохраняем состояние
            morale = hero.morale,
            luck = hero.luck
        )
        // остальное как было...
    }

    // Методы делегируют к Hero
    fun addFood(addAmount: Float) {
        hero.addFood(addAmount)
    }
}
```

---

## Итоговая структура слоёв

```
pure/domain/hero/           ← чистый домен, ноль зависимостей
    Hero.kt                 ← сущность
    HeroWithSupply.kt       ← роль в механике Supply
    HeroWithSettle.kt       ← роль в механике Settle
    IHeroDefinitionSource.kt ← port (контракт к инфраструктуре)
    HeroFactory.kt          ← создание Hero

infrastructure/hero/        ← знает про JSON, Ruleset, LibGDX
    RulesetHeroDefinitionSource.kt  ← читает из JSON
    HardcodedHeroDefinitionSource.kt ← для тестов

logic/map/
    MapUnit.kt              ← legacy, держит Hero внутри
                               делегирует методы
                               внешний код не тронут
```

Стрелки зависимостей:
```
MapUnit           → Hero (владеет)
RulesetHeroDefinitionSource → IHeroDefinitionSource (реализует)
HeroFactory       → IHeroDefinitionSource (использует)
MapUnit           → HeroFactory (создаёт через)
```

Домен никуда не смотрит. Всё смотрит в домен.

---

## Что этот шаблон даёт

- **Безопасность:** legacy код не тронут, тесты не упали
- **Тестируемость:** Hero тестируется без MapUnit, без LibGDX, без Koin
- **Расширяемость:** новая механика — новый интерфейс-роль, Hero его реализует
- **Заменяемость:** новый источник данных — новая реализация `IHeroDefinitionSource`
- **Постепенность:** росток посажен, можно растить дальше не ломая игру
