# Путеводитель Storytelling

Пути кода ниже относительны `core/src/com/unciv/`.

## Герои и авторские данные

`pure/domain/hero/ScenarioHero.kt` хранит исходную личность героя. `pure/application/scenario/PrepareScenarioHeroesUseCase.kt` проверяет расстановку и разрешает владельцев. `logic/scenario/InstantiateScenarioHeroesUseCase.kt` создаёт героев при новом запуске после сценарных городов и до стандартных войск, без событий движения и посещения.

TileMap.scenarioHeroes содержит описания, MapUnit.scenarioHeroId — стабильный ID, instanceName — игровое имя. Владельцы разрешаются через GameInfo.scenarioVictoryParticipants. Участники со сценарными героями не получают дополнительные стандартные отряды.

`pure/domain/story/StoryScenario.kt` содержит зоны, триггеры, условия, последствия, диалоги и описания роликов. `StoryProgress.kt` хранит факты событий, очередь и прогресс партии. Авторские данные и прогресс копируются независимо.

## Исполнение

`pure/application/story/ValidateStoryScenario.kt` проверяет ссылки и авторский маршрут AI. `ProcessStoryEvent.kt` выбирает триггеры и формирует очередь. `AdvanceStory.kt` проверяет sequence и revision, переводит реплики и применяет флаги без доступа к миру.

`logic/story/StoryRuntime.kt` исполняет очередь до сцены человека; AI использует авторские ответы. `StoryWorldEffects.kt` подготавливает игровые последствия перед изменением мира. GameStarter публикует начало миссии только при новой игре.

`StoryMapEvents.kt` собирает движение и новую видимость. UnitMovementAlgorithms исполняет очередь после размещения героя. isMapActionBlocked разрешает движение при очереди только из немодальных линейных диалогов без ожидающей атаки. Передача хода и начало боя удерживаются при любой незавершённой очереди.

`StoryBattleEvents.kt` сохраняет конкретную ожидающую атаку, повторно проверяет цель и расходует решение однократно. `ui/story/StoryPresentation.kt` вызывает resume после завершения очереди.

## Представление

StoryDialoguePopup показывает реплики и доступные ответы. StoryChatterPanel показывает немодальную реплику. WorldScreen обновляет представление по сохраняемой очереди; команды содержат владельца, sequence и revision.

StoryCinematicScreen использует StoryVideoSession и необязательный порт StoryVideoPlayer из GeneralPlatformSpecificHelpers. Реальные платформенные декодеры отсутствуют: текущая реализация показывает авторский fallbackText. Пропуск и завершение подтверждают событие однократно; закрытие экрана само по себе его не завершает.

## Редактор и пример

Edit → Heroes и Edit → Story подключены через MapEditorHeroesTab и MapEditorStoryTab. ScenarioHeroEditorPanel редактирует размещение героя. ScenarioStoryEditorPanel хранит независимый черновик; StoryEditorForm, StoryDialogueEditor и StoryEffectsEditor обслуживают поля и вложенные последствия.

`logic/map/ScenarioStoryEditorCommands.kt` проверяет общий результат до публикации. ScenarioCityEditorCommands защищает сюжетные ссылки при удалении городов и слотов. ScenarioMapValidation проверяет структуру и определения героев и сюжетных войск без игровой инициализации BaseUnit.

`logic/story/StoryDemoScenario.kt` создаёт независимую карту. Кнопка Create story example map во вкладке Story открывает её в редакторе. Инструкция запуска и ожидаемые исходы — в [приёмке](Plan/STEP_06_Acceptance.md).

## Сохранения и проверки

Формат карты — 5, совместимость игры — 8. Старые карты получают пустые описания героев и отсутствие сюжета. TileMap.story хранит авторские данные; GameInfo.storyProgress и storyPendingAttack — состояние партии. Чтение и клонирование не исполняют последствия.

ScenarioGameStarterTest проверяет настоящий запуск и загрузку мира; демонстрационный тест проходит оба исхода после загрузки ожидающей атаки. ScenarioStoryEditorCommandsTest проверяет атомарность Apply, ссылки, определения и сериализацию. ScenarioStoryEditorRenderTest проверяет реальные виджеты, навигацию, пересоздание экрана и кнопку примера в GL-сессии.

Визуальные эталоны новых сюжетных экранов не утверждены. Capture-only не является визуальным сравнением. Общий откат произвольных исключений применения последствий не реализован. Начальные армии героев используют существующие defaults; тест боевого исхода усиливает армию и исполняет тактический бой как AI без графического экрана. Остальные ограничения перечислены в документах этапов.

[План](Plan/PLAN.md) · [Этапы](Plan/STEPS.md) · [Файлы](Plan/files.md)
