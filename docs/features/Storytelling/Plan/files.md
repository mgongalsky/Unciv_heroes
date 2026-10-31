# Файлы задачи

## Документация этого пакета

Создаются: `docs/features/Storytelling/Research/RESEARCH.md`, `CODE_GUIDE.md`, `Plan/PLAN.md`, `Plan/STEPS.md`, `Plan/files.md`, `Plan/STEP_01_Heroes.md`, `Plan/STEP_02_StoryEngine.md`, `Plan/STEP_03_Integration.md`, `Plan/STEP_04_Presentation.md`, `Plan/STEP_05_Editor.md`, `Plan/STEP_06_Acceptance.md` (все сокращённые пути относительно `docs/features/Storytelling/`).

## Предварительные пути реализации

- `core/src/com/unciv/pure/domain/hero/ScenarioHero.kt` — исходное описание и локальные инварианты героя.
- `core/src/com/unciv/pure/application/scenario/PrepareScenarioHeroesUseCase.kt` — проверка и подготовка расстановки.
- `core/src/com/unciv/logic/scenario/InstantiateScenarioHeroesUseCase.kt` — создание игровых героев.
- `core/src/com/unciv/pure/domain/story/` — новые модели сценария и прогресса; точные файлы определить на этапе 2.
- `core/src/com/unciv/pure/application/story/` — обработка событий, выборов и проверка связей; точные файлы определить на этапе 2.
- `core/src/com/unciv/logic/story/` — игровые адаптеры; точные файлы определить на этапе 3.
- `core/src/com/unciv/ui/story/` — презентация разговоров и видеосцен; точные файлы определить на этапе 4.
- `core/src/com/unciv/ui/mapeditor/` — новые формы и команды; точные файлы определить на этапе 5.
- `tests/src/com/unciv/testing/pure/domain/hero/`, `pure/domain/story/`, `pure/application/scenario/`, `pure/application/story/` — чистые проверки (последние пути относительно `tests/src/com/unciv/testing/`).
- `tests/src/com/unciv/testing/story/` — интеграция и совместимость, точные классы определить по реализации.

## Предполагаемые изменения существующих файлов

Все следующие пути относительно корня проекта:

- `core/src/com/unciv/logic/GameStarter.kt` — запуск героев и начального события.
- `core/src/com/unciv/logic/GameInfo.kt` — прогресс, копирование и версия сохранения.
- `core/src/com/unciv/logic/MapFormatMigration.kt` — совместимость карт.
- `core/src/com/unciv/logic/map/TileMap.kt` — описания и глубокое копирование.
- `core/src/com/unciv/logic/map/MapUnit.kt` — стабильная идентичность и её перенос.
- `core/src/com/unciv/logic/map/ScenarioMapValidation.kt` — новые проверки границы.
- `core/src/com/unciv/logic/map/ScenarioCityEditorCommands.kt` — защита используемых слотов и объектов.
- `core/src/com/unciv/ui/mapeditor/MapEditorEditTab.kt` — регистрация инструментов.
- `core/src/com/unciv/ui/worldscreen/WorldScreen.kt` — презентация очереди.

Точки движения, обнаружения, атаки, AI, переводы и UI-проверки дополнить после исследования. Этот список предварительный, а не отчёт об изменённом коде. До финала заменить каталоги точными фактическими путями; удалений и переносов пока не запланировано.
