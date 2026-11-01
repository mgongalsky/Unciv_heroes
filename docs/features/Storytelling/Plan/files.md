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

## Этап 10 — редактирование внутри карточек

Созданы:

- core/src/com/unciv/ui/playground/StoryDialogueInlinePreview.kt — реальные клики и клавиатурный ввод внутри графа.
- docs/features/Storytelling/Plan/STEP_10_InlineDialogueEditing.md — контракты, результаты и ручная оценка.

Изменены:

- core/src/com/unciv/pure/application/story/EditStoryDialogue.kt — общая команда добавления ответа.
- tests/src/com/unciv/testing/pure/application/story/EditStoryDialogueTest.kt — переходы, AI, копии и отказы добавления.
- core/src/com/unciv/ui/mapeditor/StoryEditorForm.kt — уведомления ввода и локальный компактный режим.
- core/src/com/unciv/ui/mapeditor/StoryDialogueEditor.kt — делегирование добавления и повторная фиксация AI после rename.
- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphCanvas.kt — встроенные поля и обновление подписей.
- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphScreen.kt — синхронизация, компактная панель и постоянная кнопка.
- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphState.kt — группировка ввода в истории.
- tests/src/com/unciv/testing/StoryDialogueGraphStateTest.kt — группировка и границы Undo/Redo.
- core/src/com/unciv/ui/playground/ScenarioStoryEditorPreview.kt — подключение сценария ввода.
- docs/features/Storytelling/Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md и CODE_GUIDE.md — актуализация документов темы; сокращённые пути относятся к docs/features/Storytelling/.

StoryEffectsEditor не изменялся: компактный режим обходит существующие вложенные формы. Удалений и переносов файлов нет.

## Этап 11 — вступительное видео примера

Созданы или добавлены:

- android/assets/cinematics/Audience_01.mp4 — исходный ролик пользователя.
- android/assets/cinematics/Audience_01.webm — подготовленная копия VP8/Vorbis со звуком.
- core/src/com/unciv/ui/story/GdxStoryVideoPlayer.kt — адаптер декодера, отрисовка с сохранением пропорций и владение ресурсами.
- tests/src/com/unciv/testing/StoryVideoPlaybackTest.kt — настоящий desktop-декодер, кадры, завершение и пропуск.
- docs/features/Storytelling/Plan/STEP_11_IntroVideo.md — контракт, результаты и ограничения.

Изменены:

- core/build.gradle.kts, desktop/build.gradle.kts, tests/build.gradle.kts — gdx-video 1.3.3 без транзитивного обновления libGDX.
- core/src/com/unciv/logic/story/StoryDemoScenario.kt — ресурс вступительной сцены.
- desktop/src/com/unciv/app/desktop/PlatformSpecificHelpersDesktop.kt — производственная фабрика плеера.
- tests/src/com/unciv/testing/city/ScenarioGameStarterTest.kt — ресурс нового примера и переход к первому диалогу.
- docs/features/Storytelling/Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md и CODE_GUIDE.md — актуальные результаты; сокращённые пути относятся к docs/features/Storytelling.

Android-адаптер и существующие видеосессия/экран не изменены. Удалений и переносов нет.

## Этап 12 — размер текста и ввод в карточке

Изменены:

- core/src/com/unciv/ui/mapeditor/StoryEditorForm.kt — компактный шрифт и горизонтальная подпись CheckBox.
- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphScreen.kt — размеры кнопок и создание ответа в нажатой карточке.
- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphCanvas.kt — обработчик клика Continue.
- core/src/com/unciv/ui/playground/StoryDialogueInlinePreview.kt — размеры, существующие ответы и Continue, фокус, история и переходы.
- core/src/com/unciv/ui/playground/ScenarioStoryEditorPreview.kt — подключение UI-проверок.
- docs/features/Storytelling/Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md и CODE_GUIDE.md — результаты; сокращённые пути относятся к docs/features/Storytelling.

Создан docs/features/Storytelling/Plan/STEP_12_GraphTextAndInput.md. Удалений и переносов нет.

## Этап 13 — ответы в карточке и подготовка видео

Созданы:

- core/src/com/unciv/ui/story/StoryVideoFilePreparation.kt — ограниченный кэш сжатого файла.
- tests/src/com/unciv/testing/StoryVideoFilePreparationTest.java — границы кэша и очистка потоков.
- docs/features/Storytelling/Plan/STEP_13_CardAnswersAndVideoPreparation.md — контракт и результаты.

Изменены:

- core/src/com/unciv/ui/mapeditor/StoryDialogueGraphCanvas.kt — нижняя строка с кнопкой ответа.
- core/src/com/unciv/ui/playground/StoryDialogueInlinePreview.kt и ScenarioStoryEditorPreview.kt в том же каталоге — реальные клики и подключение проверки.
- core/src/com/unciv/ui/story/GdxStoryVideoPlayer.kt — фоновое чтение, подготовка декодера, отмена и запрос кадров.
- core/src/com/unciv/ui/story/StoryVideoPlayer.kt — признак готовности.
- core/src/com/unciv/ui/story/StoryCinematicScreen.kt — подготовка, пропуск и кадры без постоянной отрисовки.
- tests/src/com/unciv/testing/StoryVideoPlaybackTest.kt — настоящий декодер без ввода и отмена подготовки.
- tests/src/com/unciv/testing/StoryVideoSessionTest.java — совместимость Java-фейка с контрактом готовности.
- docs/features/Storytelling/Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md и CODE_GUIDE.md — результаты; сокращённые пути относятся к docs/features/Storytelling.

Удалений и переносов нет.

## Этап 14 — фактический состав

Изменены core/src/com/unciv/pure/application/story/ValidateStoryScenario.kt (удаление требования выхода), tests/src/com/unciv/testing/pure/application/story/ValidateStoryScenarioTest.kt (регрессия цепочки), core/src/com/unciv/ui/playground/StoryDialogueInlinePreview.kt (публикация полного сценария).

Создан docs/features/Storytelling/Plan/STEP_14_DialogueWithoutImmediateExit.md. Обновлены документы в docs/features/Storytelling: Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md, CODE_GUIDE.md. Удалений и переносов нет.
