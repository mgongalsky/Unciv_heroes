# STEP 04 — Конфигурация запуска

Статус: выполнен.

CaptureCity регистрируется в Ruleset.add как встроенный сценарный тип для базовых правил и модов. Конкретные цели в каталоге отсутствуют; VictoryManager исключает CaptureCity из проверки пустых milestones.

GameStarter повторно проверяет сценарий и разрешает ограничения до создания городов. Required включается, Forbidden удаляется; включённый CaptureCity без целей отклоняется. Слот → civName сохраняется после разрешения Random существующим запуском.

NewGameScreen использует превью и общий resolver; GameOptionsTable блокирует авторские ограничения. victorySelectionInitialized различает defaults и явный пустой выбор. GameParameters.clone сохраняет все флаги.

ScenarioVictoryStartupTest — 3 PASSED. Настоящий UI victorySelectionRender — PASSED: defaults, блокировка, Optional после refresh и пустой выбор после recreate. BUILD — PASSED.
