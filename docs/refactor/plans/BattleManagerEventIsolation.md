# План изоляции BattleManager по Clean Architecture и Физерсу

## 1. Контекст

Боевой модуль уже частично отделён от legacy-кода:

- существуют доменные `Troop`, `Army`, `TurnQueue`;
- выделены use case’ы MOVE, ATTACK, SHOOT, расчёта урона, морали и удачи;
- введены `BattleEvent`, `IBattleField`, `IBattleTile`, `IBattleRandom`;
- `BattleScreen` частично переведён на события;
- `AIBattle` работает через боевые интерфейсы;
- есть unit-, characterization- и simulation-тесты;
- `DesktopLauncherBattle` запускает бой отдельно от основной игры.

При этом имя каталога `pure` пока не гарантирует чистоту слоя:

- `Perform*UseCase` используют legacy `ErrorId` и `IBattleTile`;
- `IBattleField` зависит от LibGDX `Vector2` и legacy `Direction`;
- `INavigableTile` зависит от LibGDX `Vector2`;
- каталоги и package у некоторых battle-классов не совпадают;
- `BattleActionResult` находится в default package и зависит от UI и боевых тайлов.

Задача — определить устойчивые границы слоёв и постепенно привести зависимости к ним без изменения
игрового поведения.

## 2. Архитектурная цель

```text
Presentation/UI ───────→ Application ───────→ Domain
       │                      ↑                  ↑
       │                      │                  │
       └──── mappers ─────────┘                  │
                                                │
Infrastructure/Legacy ── реализует ports внутренних слоёв

Composition Root ─────── создаёт реализации и связывает слои
```

Главное правило: внутренний слой ничего не знает о внешнем.

- Domain не знает об Application, UI, LibGDX, Koin, Ruleset, `TileInfo`, `MapUnit`, `ArmyInfo`.
- Application знает Domain и объявляет необходимые входные/выходные порты.
- Presentation знает Application DTO/API и LibGDX UI.
- Infrastructure/Legacy Integration знает внешние технологии и реализует внутренние порты.
- Composition Root знает конкретные реализации и связывает их.

## 3. Карта целевых слоёв

### 3.1 Domain

Каталоги:

```text
core/src/com/unciv/pure/domain/battle/
core/src/com/unciv/pure/domain/army/
core/src/com/unciv/pure/domain/troop/
```

| Класс | Роль |
|---|---|
| `Troop` | Entity |
| `Army` | Entity/Aggregate |
| `TurnQueue` | Domain object |
| `Point` | Неизменяемый value object координаты |
| `BattleDirection` | Value object/enum, только если направление является правилом Domain |
| `IBattleRandom` | Domain service port |

Ограничения:

- только Kotlin/JDK и собственный Domain;
- никаких `Vector2`, `TileInfo`, Scene2D, Koin и UI-текстов;
- entity не используется как DTO, когда достаточно id/value object;
- package совпадает с физическим каталогом.

`Vector2` остаётся допустимым во внешних слоях. На границе используется преобразование
`Vector2 ↔ Point`.

### 3.2 Application

Каталоги:

```text
core/src/com/unciv/pure/application/battle/
core/src/com/unciv/pure/application/battle/port/
```

| Класс | Роль |
|---|---|
| `BattleCommand` | Input DTO |
| `BattleCommandResult` | Синхронный output DTO |
| `BattleRejection` | Машиночитаемая причина отказа |
| `BattleEvent` | Асинхронный output DTO |
| `BattleOutcome` | Output DTO завершённого боя, если нужен отдельно |
| `PerformMoveUseCase` | Use case |
| `PerformAttackUseCase` | Use case |
| `PerformShootUseCase` | Use case |
| `ExecuteBattleCommandUseCase` | Application coordinator, если оправдан после extraction |
| `IBattleFieldPort` | Port к состоянию поля, если нужен |
| `IBattleMovementPort` | Port к pathfinding, если нужен |
| `IBattleOutcomePort` | Port применения результата к миру, если нужен |

Предварительный input DTO:

```kotlin
sealed class BattleCommand {
    abstract val troopId: Int

    data class Move(override val troopId: Int, val target: Point) : BattleCommand()
    data class Attack(
        override val troopId: Int,
        val target: Point,
        val attackFrom: Point?
    ) : BattleCommand()
    data class Shoot(override val troopId: Int, val target: Point) : BattleCommand()
    data class Skip(override val troopId: Int) : BattleCommand()
}
```

Финальную сигнатуру определить после USAGES и characterization-тестов.

Правила Application DTO:

- id и value objects вместо UI/legacy объектов;
- никаких `IBattleTile`, `TileInfo`, `Vector2`;
- никаких UI-текстов;
- output event не содержит actor, screen или UI callback;
- DTO не содержит бизнес-логику entity.

### 3.3 Presentation/UI

Каталог:

```text
core/src/com/unciv/ui/battlescreen/
```

| Класс | Роль |
|---|---|
| `BattleScreen` | View: actors, ввод, отображение, анимации |
| `BattleUiCommandMapper` | UI input → `BattleCommand`, только если преобразование не помещается естественно в существующий код |
| `BattleEventRenderer` | Event → визуальный эффект, только если обработчик действительно нужно извлечь |
| `BattleController` | Presentation lifecycle/orchestration, после characterization `GlobalScope` |
| `BattleView` | View boundary, только если нужен для тестирования controller |

Presentation может зависеть от Application. Application не знает о Presentation.

### 3.4 Infrastructure и Legacy Integration

Это концептуальная роль, а не обязательный каталог и не требование создавать отдельный класс на
каждую зависимость.

Здесь допустимы `TileMap`, `TileInfo`, `Vector2`, `MapUnit`, `CityInfo`, Ruleset и другие внешние
детали. Код этой роли переводит внешний формат в контракт внутреннего слоя и обратно.

Примеры возможных реализаций:

- преобразование `Point ↔ Vector2`;
- реализация field port поверх `TileMap/TileInfo`;
- реализация movement port через текущий pathfinding;
- применение `BattleOutcome` к `MapUnit` или городу;
- `RealBattleRandom` как реализация `IBattleRandom`.

Adapter — технический переводчик на границе, а не новая бизнес-сущность.

### 3.5 Composition Root

Возможные точки композиции:

- `BattleScreen.fromCombatants`;
- `GameModule`;
- отдельная фабрика, только если текущая точка становится перегруженной;
- `DesktopLauncherBattle` для sandbox.

Composition Root создаёт concrete dependencies и передаёт их внутрь. Domain/Application не вызывают
Koin напрямую.

## 4. Политика ports и adapters: не плодить сущности

### 4.1 Port создаётся только под реальную потребность

Port — интерфейс, через который внутреннему слою нужна внешняя возможность.

Не переносить API `TileMap` в новый интерфейс один в один. Сначала определить минимальную
потребность use case. Если нужны только позиции, port не должен возвращать `TileInfo`.

### 4.2 Adapter создаётся только при реальном несовпадении моделей

Отдельный adapter-класс оправдан, когда он скрывает тяжёлую зависимость или содержит заметное
преобразование:

- `Point ↔ Vector2/TileInfo`;
- чистый field contract ↔ `TileMap`;
- чистый outcome ↔ изменения `MapUnit/CityInfo`;
- чистый `Troop` ↔ legacy `MovableUnit`.

Отдельный класс не нужен, если достаточно:

- простого параметра;
- существующей делегации;
- чистой mapper-функции;
- lambda seam;
- реализации небольшого port существующим классом.

### 4.3 Лестница минимального решения

Перед созданием adapter-класса проверять варианты по порядку:

1. простой параметр/value object;
2. существующая делегация;
3. чистая mapper-функция;
4. lambda seam;
5. небольшой port с реализацией в существующем классе;
6. отдельный adapter-класс — только если предыдущие варианты не изолируют зависимость.

### 4.4 Каталог adapters не создаётся заранее

- не создавать `logic/battle/adapter` ради архитектурной симметрии;
- один mapper может жить рядом с вызывающим внешним кодом;
- реализация port может жить рядом с конкретной интеграцией;
- отдельный каталог появляется только после формирования реальной группы адаптеров;
- расположение выбирается по фактической структуре проекта после extraction.

### 4.5 Решения для текущего плана

- `ArmyInfoBattleAdapter` заранее не создавать: делегация `ArmyInfo → Army` дешевле;
- `LegacyBattleResultMapper` не создавать, если result можно удалить прямой миграцией;
- `LegacyBattleRequestMapper` нужен только при временном сосуществовании двух API;
- `TileMapBattleFieldAdapter` — имя-кандидат, не утверждённый класс;
- movement adapter создавать только после аудита `TroopMovementAdapter` и pathfinding;
- outcome adapter создавать только после characterization world side effects.

## 5. Классификация существующих типов

| Тип | Сейчас | Целевое решение |
|---|---|---|
| `BattleActionRequest` | Legacy UI DTO | Заменить `BattleCommand`; временный mapper при необходимости |
| `BattleActionResult` | Legacy DTO | Заменить `BattleCommandResult` + `BattleEvent` |
| `ErrorId` | Legacy error code | Заменить `BattleRejection`; UI выбирает текст |
| `BattleEvent` | Application output, объявлен как Domain | Выровнять каталог/package как Application DTO |
| `Point` | Domain value, файл лежит в Application | Переместить в Domain |
| `IBattleField` | Mixed, зависит от LibGDX/legacy | Seam-аудит; минимальный port либо очистка интерфейса |
| `IBattleTile` | Смешивает поле и `Troop` | Не передавать через Application DTO; не удалять до USAGES |
| `INavigableTile` | Зависит от `Vector2` | Оставить за внешней границей либо очистить |
| `Perform*UseCase.Input/Output` | Протекают legacy-типы | Перевести на Domain values и `BattleRejection` |
| `TroopMovementAdapter` | Настоящий adapter в `pure/application` | Переместить наружу или заменить более узким seam |
| `BattleManager` | Legacy service + integration | Чистый coordinator либо тонкий facade |
| `AIBattle` | Application policy с concrete manager | Перевести на Application contracts |
| `BattleScreen` | Presentation + orchestration + world integration | Оставить View; извлекать подтверждённые ответственности |
| `ArmyInfo` | Serializable wrapper над `Army` | Продолжить делегацию без нового adapter по умолчанию |

## 6. Правила тестирования

### Characterization legacy-кода

1. выбрать сценарий и точки наблюдения;
2. записать временную гипотезу или `printReality`;
3. запустить;
4. увидеть фактический результат;
5. зашить фактические значения;
6. получить зелёный тест;
7. только затем менять production-код.

Characterization не задаёт желаемое поведение.

### Unit-тесты нового кода

Для новых DTO, value objects, mappers, ports и sprout-классов контракт задаётся заранее через TDD.

### Тестовый стек

Сохранить JUnit 4 и собственные fake-классы. JUnit 5/MockK — отдельная задача.

## 7. Skills

- `feathers-characterize`: перед изменением legacy-контракта.
- `feathers-seam`: перед введением port/adapter или разрывом зависимости.
- `feathers-sprout`: для нового DTO, mapper, implementation или coordinator.
- `feathers-extract-override`: временный seam для одной тяжёлой зависимости.
- `find-smells`: перед структурными изменениями `BattleManager` и `BattleScreen`.
- `write-unittest`: не использовать, пока MaxVibes возвращает `Unknown skill`.

## 8. Шаг 01 — базовая линия и layer audit

1. Запустить BUILD.
2. Запустить текущие battle-, event-, AI-, use case- и TurnQueue-тесты.
3. Отдельно запустить `BattleSimulationSimTest`.
4. Документировать существующие падения.
5. Через `find-smells` подтвердить smells по телам/usages.
6. Через `feathers-seam` классифицировать жёсткие зависимости.
7. Для каждого battle-файла определить текущий и целевой слой.
8. Для каждой зависимости выбрать самый дешёвый seam.

Production-код не менять.

## 9. Шаг 02 — characterization контракта боя

Для SKIP, MOVE, ATTACK, SHOOT, смерти, завершения, очереди, морали и удачи наблюдать:

- legacy result;
- текущего бойца;
- позиции и тайлы;
- состав армий;
- очередь;
- события и их порядок.

Особенно исследовать:

- кто продвигает очередь;
- порядок mutation/events;
- удаление погибшего;
- момент и повторяемость `BattleEnded`;
- поведение без callback;
- приоритеты legacy-ошибок.

Не указывать ожидаемые значения до запуска. Выполнить цикл
`printReality/гипотеза → запуск → факт → зелёный тест`.

## 10. Шаг 03 — очистить Domain value objects

1. Переместить `Point` в физический `pure/domain/battle`.
2. Проверить, нужно ли направление как Domain concept.
3. Не создавать `BattleDirection`, если это UI/TileMap detail.
4. Добавить unit-тесты value semantics.
5. Добавить `Vector2 ↔ Point` как функцию рядом с внешним потребителем; отдельный класс без
   необходимости не создавать.

## 11. Шаг 04 — ввести Application DTO

Через `feathers-sprout` и TDD создать:

- `BattleCommand`;
- `BattleCommandResult`;
- `BattleRejection`;
- выровненный Application `BattleEvent`;
- `BattleOutcome`, только если отдельный DTO нужен.

Правила:

- id/value objects вместо legacy objects;
- никаких tiles/LibGDX/UI;
- mapper остаётся во внешнем слое;
- временный legacy mapper создаётся только при параллельном существовании двух API.

## 12. Шаг 05 — очистить Perform*UseCase boundaries

Для MOVE, ATTACK и SHOOT по одному:

1. охарактеризовать ветки и приоритеты ошибок;
2. заменить `ErrorId` на `BattleRejection`;
3. заменить `IBattleTile` в Input/Output на `Point`, id или чистый DTO;
4. убрать UI-импорты;
5. оставить mutation/orchestration снаружи чистого расчёта;
6. прогнать characterization- и unit-тесты.

Критерий: `pure/application/battle` зависит только от Domain/Application.

## 13. Шаг 06 — изолировать поле и движение

1. Применить `feathers-seam` к `IBattleField`, `IBattleTile`, `INavigableTile`,
   `TroopMovementAdapter`.
2. Сформулировать минимальные потребности use case’ов.
3. Сначала попробовать параметр, функцию, делегацию или очистку существующего интерфейса.
4. Ввести port только при реальной потребности Application.
5. Реализовать port существующим внешним классом, если это не размывает его ответственность.
6. Отдельный adapter-класс создать только при существенном преобразовании.
7. Не создавать adapter-каталог до появления устойчивой группы классов.
8. Не удалять legacy interfaces до USAGES.

Критерий: Application не импортирует `TileInfo`, `Vector2`, legacy `Direction`, `INavigableTile`.

## 14. Шаг 07 — мигрировать BattleManager

1. Добавить исполнение `BattleCommand`.
2. Старый request оставить временным adapter method только при необходимости.
3. Перевести MOVE, ATTACK, SHOOT, SKIP на чистые use case’ы.
4. Централизовать применение state changes.
5. Публиковать Application `BattleEvent`.
6. Возвращать `BattleCommandResult`.
7. Удалить legacy request/result после USAGES.
8. Выбрать чистый manager, отдельный coordinator или тонкий facade по результату extraction; не
   создавать лишний coordinator заранее.

## 15. Шаг 08 — мигрировать AI

1. Охарактеризовать AI через `AIBattleCharTest`.
2. Перевести AI на `BattleCommand`.
3. Если concrete manager мешает тестированию, сначала попробовать узкий интерфейс/read-функции.
4. Отдельный read adapter создавать только при реальном несовпадении моделей.
5. Разделить выбор и исполнение команды только если они действительно смешаны.
6. Прогнать grid simulation.

## 16. Шаг 09 — вынести world outcome

1. Охарактеризовать последствия победы для MapUnit, City и sandbox.
2. Создать `BattleOutcome`, только если события недостаточно.
3. Сначала извлечь маленький handler во внешнем слое.
4. `IBattleOutcomePort` вводить только при реальной потребности Application.
5. Отдельный outcome adapter создавать только при заметном переводе моделей.
6. `BattleScreen` перестаёт напрямую менять world state.

## 17. Шаг 10 — Presentation controller

Сначала охарактеризовать input, AI turn, rejection, `advanceTurn`, dispose/BACK/BattleEnded и
callbacks после закрытия.

Затем:

- извлечь controller только при реальном смешении lifecycle и view;
- controller зависит от Application API;
- правила боя в него не переносятся;
- `BattleView` вводить только если нужен для теста;
- `BattleEventRenderer` вводить только как самостоятельную ответственность;
- заменить `GlobalScope` управляемым lifecycle scope.

## 18. Шаг 11 — Composition Root

1. Связать реальные зависимости в существующей точке создания.
2. Не создавать `BattleComposition`, если `BattleScreen.fromCombatants` остаётся небольшой фабрикой.
3. Если фабрика перегружается — выделить composition object.
4. Koin использовать во внешнем слое.
5. Sandbox подставляет безопасные реализации без world side effects.

## 19. Проверки

В каждом implementation-коммите:

1. BUILD;
2. узкий TESTS scope;
3. characterization-тесты затронутого legacy-контракта;
4. unit-тесты нового Domain/Application кода;
5. battle package tests после шага;
6. simulation после изменений AI/pathfinding/damage/queue/events;
7. полный suite перед завершением.

Ручная проверка через `DesktopLauncherBattle`:

- move/attack/shoot;
- pointer/очередь;
- смерть/view;
- morale/luck;
- завершение;
- BACK/dispose;
- отсутствие world side effects в sandbox.

## 20. Архитектурные критерии готовности

- Domain импортирует только Kotlin/JDK и Domain.
- Application импортирует только Domain/Application.
- В Domain/Application нет LibGDX, `TileInfo`, UI, Koin, `MapUnit`, `ArmyInfo`.
- Package совпадают с каталогами.
- UI зависит от Application, но не наоборот.
- Ports минимальны и отражают потребность use case.
- Adapter-классы существуют только там, где есть реальный перевод/тяжёлая зависимость.
- Adapter-каталог создан только при фактически сложившейся группе классов.
- `ArmyInfo` продолжает делегировать `Army` без лишней обёртки.
- AI и игрок используют `BattleCommand`.
- Success выдаётся через `BattleEvent`, отказ — через `BattleRejection`.
- World outcome находится вне `BattleScreen`.
- Coroutine lifecycle не использует `GlobalScope`.
- Тесты и `DesktopLauncherBattle` работают.
- Правила боя не меняются внутри extraction-коммитов.

## 21. Спецификация коммитов

### 21.1 Номер шага в subject

Каждый коммит этой серии содержит номер шага из плана:

```text
<type>(battle): step NN <короткое описание>
```

Примеры:

```text
test(battle): step 02 characterize command event order
refactor(battle): step 03 move Point to domain
refactor(battle): step 04 add battle command DTO
refactor(battle): step 06 isolate field dependency
```

Допустимые `type`:

- `test` — characterization/unit test без production-изменений;
- `refactor` — изменение структуры без намеренного изменения поведения;
- `fix` — отдельное исправление подтверждённого дефекта;
- `chore` — механическая очистка/удаление мёртвого кода;
- `docs` — изменение документации.

Номер шага стабилен и связывает историю Git с этим документом.

### 21.2 Шаг не равен одному коммиту

Эта последовательность задаёт рамку, а не жёсткое количество коммитов.

Если безопасный шаг требует нескольких коммитов, использовать суффиксы:

```text
refactor(battle): step 05a clean move use case input
refactor(battle): step 05b clean attack use case input
refactor(battle): step 05c clean shoot use case input
```

Можно использовать несколько коммитов с одним номером, если subject достаточно различается.
Предпочтительнее суффикс, когда он облегчает чтение истории.

### 21.3 Короткий subject

Subject должен:

- помещаться в одну короткую строку;
- говорить о главном результате;
- не перечислять все изменённые файлы;
- не использовать общие формулировки вроде `updates` или `refactoring stuff`.

### 21.4 Body из 2–3 пунктов

Каждый содержательный коммит должен иметь короткое описание из 2–3 пунктов: что фактически сделано и
зачем это важно для шага.

Пример:

```text
refactor(battle): step 04 add battle command DTO

- Add pure MOVE, ATTACK, SHOOT and SKIP commands
- Keep LibGDX and legacy tile types outside the DTO
- Cover command value semantics with unit tests
```

Требования к body:

- обычно 2–3 коротких пункта;
- описывать результат, а не пошаговый процесс работы;
- упоминать тестовую страховку, если она добавлена или обновлена;
- не превращать commit message в длинный отчёт;
- для совсем маленького механического коммита допустим один пункт, если двух честных пунктов нет.

### 21.5 Рамка предполагаемых коммитов

```text
test(battle): step 02 characterize command and event contract
refactor(battle): step 03 align Point with domain layer
refactor(battle): step 04 add application battle DTOs
refactor(battle): step 05a clean move use case boundary
refactor(battle): step 05b clean attack use case boundary
refactor(battle): step 05c clean shoot use case boundary
refactor(battle): step 06 isolate field dependency with cheapest seam
refactor(battle): step 06 isolate movement dependency with cheapest seam
refactor(battle): step 07 migrate manager to application commands
refactor(battle): step 08 migrate AI to application commands
refactor(battle): step 09 extract world outcome handling
refactor(battle): step 10 extract presentation lifecycle if justified
refactor(battle): step 11 centralize composition if justified
chore(battle): step 11 remove obsolete legacy contracts
```

Шаг 01 может не создавать production-коммит. Если baseline/audit фиксируется документом,
использовать:

```text
docs(battle): step 01 record baseline and layer audit
```

Не смешивать characterization, extraction, архитектурное перемещение и изменение игровых правил в
одном коммите.

## 22. Следующая задача

1. Выполнить шаг 01: baseline и dependency/layer audit.
2. Для каждой зависимости выбрать самый дешёвый seam.
3. Выполнить шаг 02 через `feathers-characterize`.
4. Не менять production-код на этапе characterization.
5. Выполнить шаг 03: выровнять `Point` как первый маленький Domain-шаг.
6. Выполнить шаг 04: создать `BattleCommand` как Application input DTO через sprout и TDD.
7. Не создавать adapter-класс или каталог, пока конкретная зависимость не докажет их необходимость.
8. Во всех коммитах использовать номер шага и короткий body из 2–3 пунктов.
