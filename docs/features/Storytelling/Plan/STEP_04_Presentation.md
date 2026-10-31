# 04 — Диалоги и видеосцены

Статус: IN_PROGRESS.

## Реализовано

StoryPresentation вызывается мировым экраном и восстанавливает представление из сохраняемой очереди. StoryDialoguePopup показывает говорящего, портрет или placeholder, переносимый текст и доступные ответы. Подтверждение передаёт владельца, номер события и ревизию. Следующая сцена и продолжение ожидающей атаки выполняются на следующем кадре.

StoryChatterPanel показывает немодальную линейную реплику в верхней части экрана. Действия на карте разрешены, только когда вся очередь состоит из диалогов с pauseWorld=false без узлов выбора и отсутствует ожидающая атака. Выбор, ролик или игровые последствия в очереди блокируют действия. Незавершённая очередь удерживает передачу хода и начало боя. Панель обновляется при смене ревизии или размеров; устаревшая команда не завершает новую реплику. Немодальная панель не перемещает камеру.

StoryCinematicScreen — отдельный экран. GeneralPlatformSpecificHelpers предоставляет необязательную фабрику StoryVideoPlayer. Реальные платформенные декодеры не реализованы: сейчас используется авторский fallbackText с подтверждением. StoryVideoSession обрабатывает окончание, пропуск, ошибки создания/декодирования/паузы и однократное освобождение ресурса. Уход с экрана не подтверждает событие; повторное открытие начинает ролик заново. Пропуск доступен только при skippable=true; fallback всегда позволяет продолжить миссию.

## Проверено через IDE

- BUILD — PASSED после подключения немодальной панели.
- StoryVideoSessionTest — 6 тестов PASSED: отсутствие поддержки, ошибка фабрики, завершение, ошибки декодера и очистки, ошибка паузы, пропуск и закрытие.
- StoryRuntimeTest — 8 тестов PASSED: выбор и награды, AI, владелец, сериализация, завершение видеосцены и политика немодальной очереди.
- StoryMovementTest — 6 тестов PASSED, включая прохождение зоны без остановки при немодальной реплике и удержание хода.

## Визуальная проверка и оставшаяся работа

Добавлены общий StoryDialoguePreview и сценарий story-dialogue в UiGoldenTestRunner, UiGoldenTest и UiGoldenUpdateTest. Сравнение завершилось ошибкой; в каталогах windows-11 найден только story-dialogue-actual.png, эталон и diff отсутствуют. IDE передал PNG как текст: изображение не просмотрено, эталон не обновлялся, визуальное соответствие не подтверждено.

Остаются визуальная проверка длинных ответов, небольшого экрана и расположения панели относительно UnitTable, проверки UI после resize/закрытия/загрузки и отдельные визуальные сценарии fallback и немодальной реплики. Проверка сериализации прогресса не заменяет полный цикл загрузки мира с setTransients. Настоящее воспроизведение видео не заявляется.

## Файлы представления

Созданы:

- core/src/com/unciv/ui/story/StoryDialoguePopup.kt
- core/src/com/unciv/ui/story/StoryPresentation.kt
- core/src/com/unciv/ui/story/StoryChatterPanel.kt
- core/src/com/unciv/ui/story/StoryCinematicScreen.kt
- core/src/com/unciv/ui/story/StoryVideoPlayer.kt
- core/src/com/unciv/ui/story/StoryVideoSession.kt
- core/src/com/unciv/ui/playground/StoryDialoguePreview.kt
- tests/src/com/unciv/testing/StoryVideoSessionTest.java

Изменены:

- core/src/com/unciv/ui/utils/GeneralPlatformSpecificHelpers.kt
- core/src/com/unciv/ui/worldscreen/WorldScreen.kt
- core/src/com/unciv/logic/story/StoryRuntime.kt
- core/src/com/unciv/logic/map/MapUnit.kt
- core/src/com/unciv/logic/map/UnitMovementAlgorithms.kt
- desktop/src/com/unciv/app/desktop/UiGoldenTestRunner.kt
- tests/src/com/unciv/testing/UiGoldenTest.kt
- tests/src/com/unciv/testing/UiGoldenUpdateTest.kt
- tests/src/com/unciv/testing/story/StoryRuntimeTest.kt
- tests/src/com/unciv/testing/story/StoryMovementTest.kt

[Этапы](STEPS.md)
