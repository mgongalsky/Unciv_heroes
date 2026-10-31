# Файлы задачи

Пути относительно корня проекта. Удалений и переносов нет. Пользовательский wiki и посторонние ресурсы не входят в задачу.

## Созданы: герои и сюжет

- `core/src/com/unciv/pure/domain/hero/ScenarioHero.kt`
- `core/src/com/unciv/pure/application/scenario/PrepareScenarioHeroesUseCase.kt`
- `core/src/com/unciv/logic/scenario/InstantiateScenarioHeroesUseCase.kt`
- `core/src/com/unciv/pure/domain/story/StoryScenario.kt`
- `core/src/com/unciv/pure/domain/story/StoryProgress.kt`
- `core/src/com/unciv/pure/application/story/ValidateStoryScenario.kt`
- `core/src/com/unciv/pure/application/story/ProcessStoryEvent.kt`
- `core/src/com/unciv/pure/application/story/AdvanceStory.kt`
- `core/src/com/unciv/logic/story/StoryRuntime.kt`
- `core/src/com/unciv/logic/story/StoryWorldEffects.kt`
- `core/src/com/unciv/logic/story/StoryMapEvents.kt`
- `core/src/com/unciv/logic/story/StoryBattleEvents.kt`

## Созданы: представление

- `core/src/com/unciv/ui/story/StoryDialoguePopup.kt`
- `core/src/com/unciv/ui/story/StoryPresentation.kt`
- `core/src/com/unciv/ui/story/StoryChatterPanel.kt`
- `core/src/com/unciv/ui/story/StoryCinematicScreen.kt`
- `core/src/com/unciv/ui/story/StoryVideoPlayer.kt`
- `core/src/com/unciv/ui/story/StoryVideoSession.kt`
- `core/src/com/unciv/ui/playground/StoryDialoguePreview.kt`

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
- `tests/src/com/unciv/testing/StoryVideoSessionTest.java`

## Изменены: существующие код и тесты

- `core/src/com/unciv/logic/GameStarter.kt`
- `core/src/com/unciv/logic/GameInfo.kt`
- `core/src/com/unciv/logic/MapFormatMigration.kt`
- `core/src/com/unciv/logic/map/TileMap.kt`
- `core/src/com/unciv/logic/map/MapUnit.kt`
- `core/src/com/unciv/logic/map/ScenarioMapValidation.kt`
- `core/src/com/unciv/logic/map/ScenarioCityEditorCommands.kt`
- `core/src/com/unciv/logic/map/UnitMovementAlgorithms.kt`
- `core/src/com/unciv/logic/battle/Battle.kt`
- `core/src/com/unciv/ui/worldscreen/WorldScreen.kt`
- `core/src/com/unciv/ui/utils/GeneralPlatformSpecificHelpers.kt`
- `desktop/src/com/unciv/app/desktop/UiGoldenTestRunner.kt`
- `tests/src/com/unciv/testing/city/ScenarioGameStarterTest.kt`
- `tests/src/com/unciv/testing/UiGoldenTest.kt`
- `tests/src/com/unciv/testing/UiGoldenUpdateTest.kt`

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

Формы редактора, дополнительные проверки автоматизации и восстановления, демонстрационный сценарий. Точные пути будут внесены после реализации. Эталон story-dialogue ещё не создан; actual является результатом проверки, а не утверждённым ресурсом.
