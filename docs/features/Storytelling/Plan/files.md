# Файлы задачи

Все пути ниже относительно корня проекта. Перечень актуален после реализации сценарных героев; будущие изменения отделены от фактических.

## Созданы: код и тесты

- `core/src/com/unciv/pure/domain/hero/ScenarioHero.kt` — описание и локальная проверка героя.
- `core/src/com/unciv/pure/application/scenario/PrepareScenarioHeroesUseCase.kt` — согласованность расстановки и разрешение владельцев.
- `core/src/com/unciv/logic/scenario/InstantiateScenarioHeroesUseCase.kt` — точное создание в мире без эффектов посещения.
- `tests/src/com/unciv/testing/pure/application/scenario/PrepareScenarioHeroesUseCaseTest.kt` — чистые проверки.
- `tests/src/com/unciv/testing/story/ScenarioHeroInstantiationTest.kt` — интеграция, копирование и формат карты.

## Изменены: код и тесты

- `core/src/com/unciv/logic/GameStarter.kt` — создание героев перед обычными войсками и исключение дополнительных отрядов.
- `core/src/com/unciv/logic/GameInfo.kt` — версия сохранения 7.
- `core/src/com/unciv/logic/MapFormatMigration.kt` — версия карты 4 и переход с версии 3.
- `core/src/com/unciv/logic/map/TileMap.kt` — список описаний и независимый clone.
- `core/src/com/unciv/logic/map/MapUnit.kt` — сохраняемый ID, clone и перенос при замене.
- `core/src/com/unciv/logic/map/ScenarioMapValidation.kt` — структура и определения героев.
- `core/src/com/unciv/logic/map/ScenarioCityEditorCommands.kt` — защита используемого слота.
- `tests/src/com/unciv/testing/city/ScenarioGameStarterTest.kt` — миссия без города, сохранение и повторный старт.

## Созданная документация

- `docs/features/Storytelling/Research/RESEARCH.md`
- `docs/features/Storytelling/CODE_GUIDE.md`
- `docs/features/Storytelling/Plan/PLAN.md`
- `docs/features/Storytelling/Plan/STEPS.md`
- `docs/features/Storytelling/Plan/files.md`
- `docs/features/Storytelling/Plan/STEP_01_Heroes.md`
- `docs/features/Storytelling/Plan/STEP_02_StoryEngine.md`
- `docs/features/Storytelling/Plan/STEP_03_Integration.md`
- `docs/features/Storytelling/Plan/STEP_04_Presentation.md`
- `docs/features/Storytelling/Plan/STEP_05_Editor.md`
- `docs/features/Storytelling/Plan/STEP_06_Acceptance.md`

## Предварительные области следующих этапов

Новые файлы в `core/src/com/unciv/pure/domain/story/`, `core/src/com/unciv/pure/application/story/`, `core/src/com/unciv/logic/story/`, `core/src/com/unciv/ui/story/`; формы в `core/src/com/unciv/ui/mapeditor/`; чистые тесты в `tests/src/com/unciv/testing/pure/domain/story/` и `tests/src/com/unciv/testing/pure/application/story/`.

Предполагаются дополнительные изменения GameInfo, TileMap, GameStarter, миграций, MapEditorEditTab и WorldScreen. Точки движения, обнаружения, боя, AI, переводы и UI-проверки уточняются при подключении. До завершения заменить предварительные области точными фактическими файлами. Удалений и переносов нет.
