# Путеводитель Storytelling

Пути кода ниже относительны `core/src/com/unciv/`.

## Герои и авторские данные

`pure/domain/hero/ScenarioHero.kt` хранит исходную личность. `pure/application/scenario/PrepareScenarioHeroesUseCase.kt` проверяет расстановку и владельцев. `logic/scenario/InstantiateScenarioHeroesUseCase.kt` создаёт героев после сценарных городов и до стандартных войск, без событий движения и посещения.

TileMap.scenarioHeroes содержит описания, MapUnit.scenarioHeroId — стабильный ID, instanceName — игровое имя. Владельцы разрешаются через GameInfo.scenarioVictoryParticipants. Участники со сценарными героями не получают дополнительные стандартные отряды.

`pure/domain/story/StoryScenario.kt` содержит зоны, триггеры, условия, последствия, диалоги и видеосцены. `StoryProgress.kt` хранит события, очередь и общие флаги партии. Авторские данные и прогресс копируются независимо. Выбор в одном разговоре может влиять на следующие разговоры и триггеры через эти флаги.

## Исполнение

`pure/application/story/ValidateStoryScenario.kt` проверяет ссылки и авторский маршрут AI. `ProcessStoryEvent.kt` выбирает триггеры, `AdvanceStory.kt` проверяет sequence/revision, переводит реплики и применяет флаги без доступа к миру.

`logic/story/StoryRuntime.kt` исполняет очередь до сцены человека; AI использует авторские ответы. `StoryWorldEffects.kt` проверяет игровые последствия перед изменением мира. GameStarter публикует начало миссии только при новой игре.

`StoryMapEvents.kt` собирает движение и новую видимость. UnitMovementAlgorithms исполняет очередь после размещения героя. isMapActionBlocked допускает движение при очереди только из немодальных линейных диалогов без ожидающей атаки. Передача хода и начало боя удерживаются при незавершённой очереди.

`StoryBattleEvents.kt` сохраняет ожидающую атаку, повторно проверяет цель и расходует решение однократно. `ui/story/StoryPresentation.kt` продолжает исполнение после завершения очереди.

## Представление и видео

StoryDialoguePopup показывает реплики и ответы, StoryChatterPanel — немодальную реплику. WorldScreen обновляет представление по сохраняемой очереди; команды содержат владельца, sequence и revision.

StoryCinematicScreen рисует кадр подготовки, затем использует StoryVideoSession и порт StoryVideoPlayer из GeneralPlatformSpecificHelpers. Незаданный/отсутствующий файл либо недоступный плеер приводят к отдельной заглушке с причиной, путём и fallbackText. Ошибки открытия, декодирования и управления различаются. Continue подтверждает сцену, закрытие экрана само по себе её не завершает. Реальные декодеры не подключены; наличие файла ещё не означает возможность воспроизведения.

## Редактор

Edit → Heroes и Story подключены через MapEditorHeroesTab и MapEditorStoryTab. ScenarioHeroPreview рисует героя без MapUnit. ScenarioStoryEditorPanel владеет общим черновиком; ScenarioStoryEditorCommands проверяет Apply до публикации на карту. ScenarioCityEditorCommands защищает ссылки при удалении городов/слотов, ScenarioMapValidation проверяет определения войск без игровой инициализации BaseUnit.

EditorMapHolder.refreshStoryZoneOverlays разрешает соседей через TileMap.getIfTileExistsOrNull и создаёт StoryZoneOverlay на обычных и зеркальных клетках. Внутренние рёбра скрыты. MapEditorScreen.storyZonesVisible — состояние просмотра, сохраняемое при recreate. Переключатели есть в Story и View. В Story слой показывает черновик, после выхода — применённые зоны.

Dialogues → Edit dialogue открывает StoryDialogueGraphScreen с независимой копией. StoryDialogueGraphCanvas автоматически размещает карточки и стрелки; клик выбирает узел. StoryDialogueEditor.createNode, StoryEditorForm и StoryEffectsEditor обслуживают только выбранную реплику и её ответы. Полотно поддерживает панорамирование и масштабирование.

`pure/application/story/EditStoryDialogue.kt` добавляет/заменяет/удаляет узлы и назначает старт. Rename переносит входящие переходы, самоссылки и ключ AI. Удаление старта или цели входящих стрелок отклоняется до явного перенаправления. Команды возвращают глубокую копию. Apply dialogue проверяет полный кандидат и возвращает его в черновик Story; Apply story записывает на карту. Cancel и Escape/Back отбрасывают локальную копию графа.

Preview dialogue / Preview cinematic проверяют черновик и создают StoryScenePreview. Используются общий AdvanceStory и производственные окна, но мировые последствия только отображаются. Save → Save and play записывает снимок и открывает NewGameScreen с файлом и слотами. editRevision защищает новые правки от завершения старого сохранения.

`logic/story/StoryDemoScenario.kt` создаёт независимую карту; кнопка Create story example map открывает её. Запуск и исходы описаны в [приёмке](Plan/STEP_06_Acceptance.md), новые действия — в [этапе 08](Plan/STEP_08_VisualStoryAuthoring.md).

## Сохранения и проверки

Формат карты — 5, совместимость игры — 8. Этап 08 форматы не меняет. TileMap.story хранит описания, GameInfo.storyProgress и storyPendingAttack — прогресс. Чтение и клонирование не исполняют последствия. Экранные координаты графа и видимость зон не сериализуются в сценарий.

В IDE запускать native TESTS с конкретным классом:

- com.unciv.testing.pure.application.story.EditStoryDialogueTest — 6 тестов команд графа.
- com.unciv.testing.pure.application.story.AdvanceStoryTest — 4 теста исполнения выборов.
- com.unciv.testing.story.ScenarioStoryEditorCommandsTest — 7 тестов Apply, ссылок и сериализации.
- com.unciv.testing.StoryVideoSessionTest — 6 тестов ресурса видео.
- com.unciv.testing.StoryVideoSessionReasonsTest — 3 теста причин fallback.
- com.unciv.testing.ScenarioStoryEditorRenderTest — 1 GL-тест реальных кликов, контуров, графа, resize, предпросмотра и Save and play. Использует ScenarioStoryEditorPreview, StoryZoneOverlayPreview и StoryDialogueGraphPreview.

Все перечисленные проверки и итоговый BUILD прошли. ScenarioGameStarterTest ранее проверил настоящий запуск и оба исхода демонстрации после загрузки ожидающей атаки.

## Ограничения

Capture-only не является визуальным сравнением; эталоны не утверждены. Раскладка графа автоматическая, отдельные карточки не перетаскиваются и их позиции не сохраняются. На сложных схемах стрелки могут пересекаться; подписи сокращены, полные тексты доступны в форме. Палитра зон конечна. Слой зон действует в редакторе.

Видеодекодера и переноса прогресса между миссиями нет. Общий откат произвольных исключений мировых последствий не реализован. Армии героев используют существующие defaults; автоматический тест боевого исхода усиливает армию и использует тактическое исполнение AI. Полный проход через WorldScreen и ручная визуальная приёмка остаются открытыми.

[План](Plan/PLAN.md) · [Этапы](Plan/STEPS.md) · [Файлы](Plan/files.md)
