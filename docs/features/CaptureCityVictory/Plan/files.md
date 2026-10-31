# Файлы CaptureCityVictory

Пути относительно корня проекта. Перечень отражает реализованный код; подробности — [CODE_GUIDE.md](../CODE_GUIDE.md).

## Чистое ядро

- `core/src/com/unciv/pure/domain/victory/CaptureCityObjective.kt` — цели и проверка текущего владения всеми назначенными городами.
- `core/src/com/unciv/pure/domain/scenario/ScenarioVictorySettings.kt` — Required/Optional/Forbidden, defaults, цели и глубокое копирование.
- `core/src/com/unciv/pure/application/scenario/ResolveVictoryOptionsUseCase.kt` — разрешение выбора типов побед.
- `core/src/com/unciv/pure/application/scenario/ValidateScenarioUseCase.kt` — проверка ссылок и структуры.
- `core/src/com/unciv/pure/application/victory/ProcessCityCapturedUseCase.kt` — проверка после завершённого захвата.

## Карта, запуск и результат

- `core/src/com/unciv/logic/map/TileMap.kt` — настройки карты и clone.
- `core/src/com/unciv/logic/map/ScenarioMapValidation.kt` — адаптер валидации.
- `core/src/com/unciv/logic/MapFormatMigration.kt` — формат карты 3.
- `core/src/com/unciv/logic/MapSaver.kt` — превью настроек.
- `core/src/com/unciv/logic/GameStarter.kt` — ограничения запуска и назначение слотов.
- `core/src/com/unciv/logic/GameInfo.kt` — формат игры 6, первый результат, участники и граница остановки хода.
- `core/src/com/unciv/models/metadata/GameParameters.kt` — сохранение явного выбора и флагов clone.
- `core/src/com/unciv/models/ruleset/Ruleset.kt` — встроенный CaptureCity для баз и модов.
- `core/src/com/unciv/logic/civilization/VictoryManager.kt` — адаптер чистой проверки и чтение сохранённого результата.
- `core/src/com/unciv/logic/city/NeutralCityEntry.kt`, `core/src/com/unciv/logic/battle/BattleWorldOutcomeHandler.kt`, `core/src/com/unciv/logic/city/CityInfoConquestFunctions.kt` — события завершённого захвата.
- `core/src/com/unciv/logic/automation/civilization/NextTurnAutomation.kt` — остановка действий AI.

Общие transfer/moveToCiv, Victory.kt и базовые VictoryTypes.json не изменялись ради регистрации целей.

## Редактор и UI

- `core/src/com/unciv/logic/map/ScenarioVictoryEditorCommands.kt` — атомарное применение черновика.
- `core/src/com/unciv/logic/map/ScenarioCityEditorCommands.kt` — защита используемых городов и слотов.
- `core/src/com/unciv/ui/mapeditor/MapEditorVictoryTab.kt`, `ScenarioVictoryEditorPanel.kt`, `MapEditorEditTab.kt`, `ScenarioCityEditorPanel.kt` — вкладка Victory и связанные формы.
- `core/src/com/unciv/ui/newgamescreen/GameOptionsTable.kt`, `NewGameScreen.kt` — ограничения выбора и его восстановление.
- `core/src/com/unciv/ui/victoryscreen/VictoryScreen.kt`, `core/src/com/unciv/ui/worldscreen/WorldScreen.kt` — цели, результат и продолжение.
- `core/src/com/unciv/ui/playground/ScenarioVictoryEditorPreview.kt`, `ScenarioVictorySelectionPreview.kt` — проверяемые формы.
- `desktop/src/com/unciv/app/desktop/UiGoldenTestRunner.kt` — регистрация preview.

Новые подписи используют английский fallback; файлы переводов не дополнены.

## Тесты

Чистые проверки целей, события захвата, выбора и валидации подключены к `tests/src/com/unciv/testing/pure/CityTestSuite.kt`.

В `tests/src/com/unciv/testing/victory/` добавлены VictoryLegacyCharacterizationTest, CaptureCityIntegrationTest, ScenarioVictoryStartupTest, ScenarioVictoryEditorCommandsTest, VictoryPersistenceTest, VictorySelectionPersistenceTest, VictoryTurnFlowTest, VictoryTestSuite и VictoryRegressionSuite.

Обновлены `tests/src/com/unciv/testing/city/CityGameMigrationTest.kt`, `tests/src/com/unciv/testing/UiGoldenTest.kt` и `UiGoldenUpdateTest.kt`. Исторические фикстуры не переписаны. Baseline не обновлялись, actual-снимки в коммиты не входят.

## Документы

`Plan/PLAN.md`, `Plan/STEPS.md`, восемь `Plan/STEP_*.md`, этот перечень, `Research/RESEARCH.md` и `CODE_GUIDE.md` описывают контракт, реализацию, проверки и оставшуюся ручную приёмку.
