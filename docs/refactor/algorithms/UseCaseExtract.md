## Алгоритм: от функции к покрытому UseCase

### Шаг 1 — Выбираем функцию
Берём функцию с логикой, которую хотим покрыть тестами. Критерии:
- Содержит бизнес-логику (не просто геттер)
- Имеет понятные входные параметры и эффекты
- Желательно не слишком большая

### Шаг 2 — Extraction в UseCase
Выносим тело функции в `object UseCase { fun execute(...) }`.
- Все зависимости становятся явными параметрами
- Глобалы (`UncivGame.Current` и т.п.) заворачиваем в лямбды
- Оригинальная функция просто делегирует вызов UseCase
- **Логику не меняем** — один в один

### Шаг 3 — Анализируем зависимости UseCase
Смотрим на каждый параметр:
- Простые поля (`Int`, `String`, `Boolean`) — ставим напрямую
- Сложные объекты — создаём `Fake` классы, переопределяя только проблемные методы (те что лезут в GDX, tileMap, gameInfo и т.п.)
- Глобалы через Koin — `FakeClass : RealClass(), KoinComponent { init { field = get() } }`

### Шаг 4 — Готовим фейки
Для каждого сложного параметра:
- Наследуемся от реального класса (`open class` — это наш шов)
- Переопределяем только проблемные методы
- Добавляем флаги (`var calledCalled = false`) для отслеживания вызовов
- Добавляем конфигурируемые поля (`var isCurrentPlayerOverride = false`)

### Шаг 5 — Намечаем сценарии
Смотрим на логику UseCase и выписываем ветки:
- Ранние выходы (`return`)
- Разные значения условий (`if/when`)
- Граничные случаи

### Шаг 6 — Распечатка реальности
Для каждого сценария пишем `printReality_` тест:
- Настраиваем входные параметры
- Запускаем UseCase
- `println` всего что интересно
- Запускаем и смотрим что реально происходит

### Шаг 7 — Зашиваем в characterization тесты
Берём результаты распечатки и переписываем в `characterize_` тесты:
- Убираем все `println`
- Добавляем `assertEquals`, `assertTrue`, `assertFalse`, `assertNull`
- Значения берём точно из вывода распечатки

---

## Как это выглядело на примере `WorkOnImprovementUseCase`

```
Шаг 1 — workOnImprovement() в MapUnit
         → бизнес-логика, несколько веток, понятные эффекты

Шаг 2 — Вынесли в WorkOnImprovementUseCase.execute()
         → UncivGame.Current завернули в лямбду onImprovementCompleted
         → tryProvideProductionToClosestCity тоже лямбда
         → MapUnit.workOnImprovement() теперь просто вызывает UseCase

Шаг 3 — Зависимости: TileInfo, CivilizationInfo, Ruleset, MapUnit
         → TileInfo: лезет в tileMap, owningCity → нужен Fake
         → CivilizationInfo: нужен isCurrentPlayer() → уже был Fake
         → Ruleset: через Koin → уже умели
         → MapUnit: уже умели создавать

Шаг 4 — FakeTileInfo: переопределили removeRoad, addRoad, setRepaired, removeImprovement
         → ruleset берётся из Koin в init
         → добавили флаги: removeRoadCalled, addRoadCalled и т.д.
         FakeCivilizationInfo: добавили isCurrentPlayerOverride

Шаг 5 — Сценарии:
         → ранний выход (isMarkedForCreatesOneImprovement)
         → декремент без завершения
         → repair завершается
         → Road строится
         → onImprovementCompleted вызывается / не вызывается

Шаг 6 — printReality тесты: запустили, увидели реальные значения

Шаг 7 — characterize тесты: зашили значения в assertEquals/assertTrue/assertFalse
```

---
