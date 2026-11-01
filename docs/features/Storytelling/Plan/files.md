# Файлы задачи

Пути относительно корня проекта. Файлы не удалялись и не переносились. Посторонние ресурсы, wiki, настройки и журналы ошибок не входят в коммиты.

## Созданы в core/src/com/unciv/ в этапах 01–07

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
- ui/mapeditor/ScenarioHeroPreview.kt
- ui/mapeditor/StoryScenePreview.kt
- ui/playground/StoryDialoguePreview.kt
- ui/playground/ScenarioStoryEditorPreview.kt

## Тесты этапов 01–07 в tests/src/com/unciv/testing/

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

## Существующие файлы, изменённые в этапах 01–07

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
- core/src/com/unciv/ui/mapeditor/EditorMapHolder.kt
- core/src/com/unciv/ui/mapeditor/MapEditorScreen.kt
- core/src/com/unciv/ui/mapeditor/MapEditorSaveTab.kt
- desktop/src/com/unciv/app/desktop/UiGoldenTestRunner.kt
- tests/src/com/unciv/testing/city/ScenarioGameStarterTest.kt
- tests/src/com/unciv/testing/UiGoldenTest.kt
- tests/src/com/unciv/testing/UiGoldenUpdateTest.kt

## Созданы в этапе 08

- core/src/com/unciv/pure/application/story/EditStoryDialogue.kt — команды графа на независимых копиях.
- core/src/com/unciv/ui/mapeditor/StoryZoneOverlay.kt — внешние рёбра и подписи зон.
- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphCanvas.kt — карточки и направленные связи.
- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphScreen.kt — полноэкранный редактор и локальный черновик.
- core/src/com/unciv/ui/playground/StoryZoneOverlayPreview.kt — проверки контуров, переключателей и зеркал.
- core/src/com/unciv/ui/playground/StoryDialogueGraphPreview.kt — клики карточек, переходы и resize.
- tests/src/com/unciv/testing/pure/application/story/EditStoryDialogueTest.kt — чистые тесты команд.
- tests/src/com/unciv/testing/StoryVideoSessionReasonsTest.kt — диагностика fallback без GL.
- docs/features/Storytelling/Plan/STEP_08_VisualStoryAuthoring.md — реализация, проверки и ручной маршрут.

## Изменены в этапе 08

- core/src/com/unciv/ui/mapeditor/EditorMapHolder.kt — соседство и контуры всех зон.
- core/src/com/unciv/ui/mapeditor/MapEditorScreen.kt — видимость слоя и recreate.
- core/src/com/unciv/ui/mapeditor/MapEditorStoryTab.kt — переключатель и отображение черновика.
- core/src/com/unciv/ui/mapeditor/MapEditorViewTab.kt — переключатель применённых зон.
- core/src/com/unciv/ui/mapeditor/ScenarioStoryEditorPanel.kt — список диалогов, вход в граф, проверка кандидата.
- core/src/com/unciv/ui/mapeditor/StoryDialogueEditor.kt — форма выбранного узла; старый метод полной формы удалён.
- core/src/com/unciv/ui/mapeditor/StoryEditorForm.kt — имена текстовых полей.
- core/src/com/unciv/ui/playground/ScenarioStoryEditorPreview.kt — новый UI-маршрут и регрессия.
- core/src/com/unciv/ui/story/StoryVideoSession.kt — причины fallback.
- core/src/com/unciv/ui/story/StoryCinematicScreen.kt — подготовка и видеозаглушка.
- docs/features/Storytelling/Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md и CODE_GUIDE.md — актуальные контракты, результаты и ограничения; все пути в этой строке относятся к docs/features/Storytelling/.

## Документация и визуальные результаты

Корень docs/features/Storytelling содержит Research/RESEARCH.md, CODE_GUIDE.md, Plan/PLAN.md, Plan/STEPS.md, Plan/files.md и STEP_01_Heroes.md, STEP_02_StoryEngine.md, STEP_03_Integration.md, STEP_04_Presentation.md, STEP_05_Editor.md, STEP_06_Acceptance.md, STEP_07_EditorUsability.md, STEP_08_VisualStoryAuthoring.md в Plan/.

Сценарии story-dialogue, hero-editor и story-editor зарегистрированы в GL-runner. PNG в tests/golden/results — результаты запуска, не утверждённые эталоны. Baseline не обновлялся.

[План](PLAN.md) · [Этапы](STEPS.md) · [Путеводитель](../CODE_GUIDE.md)

## Этап 09 — управление графом

Созданы:

- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphState.kt — локальные позиции, выбор, размер текста и история.
- core/src/com/unciv/ui/playground/StoryDialogueGraphGesturesPreview.kt — координатная проверка жестов и границ текста.
- tests/src/com/unciv/testing/StoryDialogueGraphStateTest.kt — проверки состояния представления без GL.
- docs/features/Storytelling/Plan/STEP_09_GraphInteraction.md — решения, проверки и ручная приёмка.

Изменены:

- core/src/com/unciv/pure/application/story/EditStoryDialogue.kt — изменение конкретного перехода на копии.
- tests/src/com/unciv/testing/pure/application/story/EditStoryDialogueTest.kt — переходы, отказы и сохранение данных.
- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphCanvas.kt — ограничение текста, порты и жесты.
- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphScreen.kt — команды, история, размер текста и клавиатура.
- core/src/com/unciv/ui/playground/ScenarioStoryEditorPreview.kt — подключение проверки жестов.
- docs/features/Storytelling/Plan/PLAN.md — поведение и границы.
- docs/features/Storytelling/Plan/STEPS.md — статус этапа.
- docs/features/Storytelling/Plan/files.md — фактический состав изменений.
- docs/features/Storytelling/Research/RESEARCH.md — источники и выводы.
- docs/features/Storytelling/CODE_GUIDE.md — текущие вызовы, управление и проверки.
