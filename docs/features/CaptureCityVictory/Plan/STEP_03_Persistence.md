# STEP 03 — Форматы и валидация

Статус: выполнен.

TileMap хранит scenarioVictorySettings и копирует их глубоко. GameInfo хранит victoryWinner, victoryType и scenarioVictoryParticipants; write/read используют существующую сериализацию, clone отделяет новые коллекции. Отдельный DTO результата не создан.

Карта обновлена до версии 3, игра — до 6. Старые карты получают null-политику. Исторический контейнер городов версии 4 продолжает мигрировать. Превью MapSaver читает настройки без клеток и городов. ValidateScenarioUseCase/ScenarioMapValidation проверяют ссылки.

VictoryPersistenceTest: JSON/сжатие, clone, отсутствие событий при чтении и старые карты — PASSED. Полный save/load результата также прошёл в ScenarioVictoryStartupTest. CityGameMigrationTest обновлён на текущую константу для новой записи и следующую версию для отказа; исторический вход 4 сохранён. Регрессионный suite — PASSED.
