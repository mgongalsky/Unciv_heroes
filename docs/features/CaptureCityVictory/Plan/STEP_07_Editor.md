# STEP 07 — Редактор и UI

Статус: реализация и автоматические проверки форм выполнены; ручное визуальное утверждение остаётся в финальной приёмке.

Edit → Victory: MapEditorVictoryTab и ScenarioVictoryEditorPanel. Настраиваются три режима всех каталоговых типов, Optional-defaults и несколько городов с допустимыми слотами. ScenarioVictoryEditorCommands проверяет независимый черновик до публикации. Используемые города/слоты защищены от удаления; форма города показывает отказ через attempt.

GameOptionsTable отображает Required/Forbidden заблокированными. NewGameScreen применяет превью и повторяет проверку перед стартом. VictoryScreen показывает текущее владение целями без отдельного прогресса.

ScenarioVictoryEditorCommandsTest — 2 PASSED. victoryEditorRender, victorySelectionRender и cityEditorRender — PASSED. Golden city-editor не прошёл из-за отсутствующего baseline; эталоны не обновлялись. Capture-only не считается визуальным сравнением. Новые подписи имеют английский fallback; полный перевод не добавлен.

Визуальная ручная приёмка: [CODE_GUIDE.md](../CODE_GUIDE.md#ручная-приёмка).
