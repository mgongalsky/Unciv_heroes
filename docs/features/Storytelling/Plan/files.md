# Файлы задачи

Пути относительно корня проекта. Удалений и переносов нет. Посторонние ресурсы, wiki и журналы ошибок не включаются в коммиты задачи.

## Созданы в core/src/com/unciv/

- pure/domain/hero/ScenarioHero.kt
- pure/application/scenario/PrepareScenarioHeroesUseCase.kt
- logic/scenario/InstantiateScenarioHeroesUseCase.kt
- pure/domain/story/StoryScenario.kt
- pure/domain/story/StoryProgress.kt
- pure/application/story/ValidateStoryScenario.kt
- pure/application/story/ProcessStoryEvent.kt
- pure/application/story/AdvanceStory.kt
- logic/story/StoryRuntime.kt
- logic/story/StoryWorldEffects.kt
- logic/story/StoryMapEvents.kt
- logic/story/StoryBattleEvents.kt
- logic/story/StoryDemoScenario.kt
- logic/map/ScenarioStoryEditorCommands.kt
- ui/story/StoryDialoguePopup.kt
- ui/story/StoryPresentation.kt
- ui/story/StoryChatterPanel.kt
- ui/story/StoryCinematicScreen.kt
- ui/story/StoryVideoPlayer.kt
- ui/story/StoryVideoSession.kt
- ui/mapeditor/MapEditorHeroesTab.kt
- ui/mapeditor/MapEditorStoryTab.kt
- ui/mapeditor/ScenarioHeroEditorPanel.kt
- ui/mapeditor/ScenarioStoryEditorPanel.kt
- ui/mapeditor/StoryDialogueEditor.kt
- ui/mapeditor/StoryEditorForm.kt
- ui/mapeditor/StoryEffectsEditor.kt
- ui/playground/StoryDialoguePreview.kt
- ui/playground/ScenarioStoryEditorPreview.kt

## Созданы в tests/src/com/unciv/testing/

- pure/application/scenario/PrepareScenarioHeroesUseCaseTest.kt
- story/ScenarioHeroInstantiationTest.kt
- pure/domain/story/StoryScenarioTest.kt
- pure/application/story/ValidateStoryScenarioTest.kt
- pure/application/story/ProcessStoryEventTest.kt
- pure/application/story/AdvanceStoryTest.kt
- story/StoryPersistenceTest.kt
- story/StoryWorldEffectsTest.kt
- story/StoryRuntimeTest.kt
- story/StoryMovementTest.kt
- story/StoryBattleEventsTest.kt
- story/ScenarioStoryEditorCommandsTest.kt
- StoryVideoSessionTest.java
- ScenarioStoryEditorRenderTest.kt

## Изменены существующие файлы

- core/src/com/unciv/logic/GameStarter.kt
- core/src/com/unciv/logic/GameInfo.kt
- core/src/com/unciv/logic/MapFormatMigration.kt
- core/src/com/unciv/logic/map/TileMap.kt
- core/src/com/unciv/logic/map/MapUnit.kt
- core/src/com/unciv/logic/map/ScenarioMapValidation.kt
- core/src/com/unciv/logic/map/ScenarioCityEditorCommands.kt
- core/src/com/unciv/logic/map/UnitMovementAlgorithms.kt
- core/src/com/unciv/logic/battle/Battle.kt
- core/src/com/unciv/ui/worldscreen/WorldScreen.kt
- core/src/com/unciv/ui/utils/GeneralPlatformSpecificHelpers.kt
- core/src/com/unciv/ui/mapeditor/MapEditorEditTab.kt
- desktop/src/com/unciv/app/desktop/UiGoldenTestRunner.kt
- tests/src/com/unciv/testing/city/ScenarioGameStarterTest.kt
- tests/src/com/unciv/testing/UiGoldenTest.kt
- tests/src/com/unciv/testing/UiGoldenUpdateTest.kt

## Документация

В docs/features/Storytelling/: Research/RESEARCH.md, CODE_GUIDE.md, Plan/PLAN.md, Plan/STEPS.md, Plan/files.md и документы этапов STEP_01_Heroes.md, STEP_02_StoryEngine.md, STEP_03_Integration.md, STEP_04_Presentation.md, STEP_05_Editor.md, STEP_06_Acceptance.md.

## Визуальные результаты

Сценарии story-dialogue, hero-editor и story-editor зарегистрированы в GL-runner. Фактические PNG в tests/golden/results являются результатами запуска, а не утверждёнными эталонами. Визуальная приёмка и обновление baseline не выполнены.
