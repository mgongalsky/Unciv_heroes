# Файлы задачи

Все пути относительно корня проекта. Перечень отражает героев, сюжетное ядро и текущую интеграцию движения и боя. Удалений и переносов нет.

## Созданы: код

- `core/src/com/unciv/pure/domain/hero/ScenarioHero.kt` — описание героя.
- `core/src/com/unciv/pure/application/scenario/PrepareScenarioHeroesUseCase.kt` — проверка расстановки и владельцев.
- `core/src/com/unciv/logic/scenario/InstantiateScenarioHeroesUseCase.kt` — точное создание без посещения.
- `core/src/com/unciv/pure/domain/story/StoryScenario.kt` — авторские описания.
- `core/src/com/unciv/pure/domain/story/StoryProgress.kt` — события и сохраняемый прогресс.
- `core/src/com/unciv/pure/application/story/ValidateStoryScenario.kt` — проверка связей.
- `core/src/com/unciv/pure/application/story/ProcessStoryEvent.kt` — подбор триггеров и очередь.
- `core/src/com/unciv/pure/application/story/AdvanceStory.kt` — переходы и подтверждения.
- `core/src/com/unciv/logic/story/StoryRuntime.kt` — исполнение очереди и ответы AI.
- `core/src/com/unciv/logic/story/StoryWorldEffects.kt` — подготовка и применение игровых последствий.
- `core/src/com/unciv/logic/story/StoryMapEvents.kt` — события движения и видимости.
- `core/src/com/unciv/logic/story/StoryBattleEvents.kt` — отложенная атака, отмена и повторная проверка.

## Созданы: тесты

- `tests/src/com/unciv/testing/pure/application/scenario/PrepareScenarioHeroesUseCaseTest.kt`
- `tests/src/com/unciv/testing/story/ScenarioHeroInstantiationTest.kt`
- `tests/src/com/unciv/testing/pure/domain/story/StoryScenarioTest.kt`
- `tests/src/com/unciv/testing/pure/application/story/ValidateStoryScenarioTest.kt`
- `tests/src/com/unciv/testing/pure/application/story/ProcessStoryEventTest.kt`
- `tests/src/com/unciv/testing/pure/application/story/AdvanceStoryTest.kt`
- `tests/src/com/unciv/testing/story/StoryPersistenceTest.kt`
- `tests/src/com/unciv/testing/story/StoryWorldEffectsTest.kt`
- `tests/src/com/unciv/testing/story/StoryRuntimeTest.kt`
- `tests/src/com/unciv/testing/story/StoryMovementTest.kt`
- `tests/src/com/unciv/testing/story/StoryBattleEventsTest.kt`

## Изменены: код и тесты

- `core/src/com/unciv/logic/GameStarter.kt` — герои и вступительное событие.
- `core/src/com/unciv/logic/GameInfo.kt` — совместимость 8, прогресс, контекст атаки, копирование и удержание хода.
- `core/src/com/unciv/logic/MapFormatMigration.kt` — формат карты 5 и миграции.
- `core/src/com/unciv/logic/map/TileMap.kt` — герои, сюжет и независимое копирование.
- `core/src/com/unciv/logic/map/MapUnit.kt` — ID героя и остановка автоматического действия.
- `core/src/com/unciv/logic/map/ScenarioMapValidation.kt` — структура и определения сценария.
- `core/src/com/unciv/logic/map/ScenarioCityEditorCommands.kt` — защита используемого слота.
- `core/src/com/unciv/logic/map/UnitMovementAlgorithms.kt` — очередь событий и остановка маршрута.
- `core/src/com/unciv/logic/battle/Battle.kt` — предбоевой сюжетный вызов.
- `core/src/com/unciv/ui/worldscreen/WorldScreen.kt` — проверка очереди перед передачей хода.
- `tests/src/com/unciv/testing/city/ScenarioGameStarterTest.kt` — герои, сохранение, повторный старт и вступление.

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

## Предстоящие области

UI в `core/src/com/unciv/ui/story/`, формы в `core/src/com/unciv/ui/mapeditor/`, дополнительные проверки автоматизации и восстановления, порт видеосцен и демонстрационный сценарий. Точные пути будут внесены после реализации. Пользовательский wiki и посторонние ресурсы не входят в изменения задачи.
