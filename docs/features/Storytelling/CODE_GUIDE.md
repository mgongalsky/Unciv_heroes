# Путеводитель Storytelling

## Сценарные герои

`core/src/com/unciv/pure/domain/hero/ScenarioHero.kt` — исходное описание и локальные инварианты. `core/src/com/unciv/pure/application/scenario/PrepareScenarioHeroesUseCase.kt` проверяет связи и готовит независимые описания с разрешёнными владельцами.

`core/src/com/unciv/logic/scenario/InstantiateScenarioHeroesUseCase.kt` создаёт реальные юниты. Производственный вызов находится в GameStarter.addCivStartingUnits, после сценарных городов и до стандартных войск. Весь набор проверяется до размещения; прямое размещение исключает эффекты посещения на старте. Участники со сценарными героями не получают дополнительные стартовые отряды.

TileMap.scenarioHeroes хранит исходные описания. MapUnit.scenarioHeroId — стабильный ID, instanceName — отображаемое имя. Clone и перенос характеристик сохраняют личность. Карта копирует описания независимо. Владельцы разрешаются через существующий GameInfo.scenarioVictoryParticipants.

Формат карты 4 мигрирует старые карты с пустыми описаниями героев. Формат игры 7 сохраняет новые поля. Восстановление партии читает юнитов, не повторяя запуск сценария. ScenarioMapValidation проверяет структуру и типы; ScenarioCityEditorCommands защищает используемые слоты от удаления.

## Проверки через IDE

- `com.unciv.testing.pure.application.scenario.PrepareScenarioHeroesUseCaseTest` — чистая подготовка, 6 тестов, PASSED.
- `com.unciv.testing.story.ScenarioHeroInstantiationTest` — адаптер, формат карты, копирование, перенос ID и отказ размещения в нейтральном городе, PASSED. В файле 6 методов, счётчик IDE при полном запуске сообщил 7.
- `com.unciv.testing.city.ScenarioGameStarterTest#scenarioHeroWithoutCitySurvivesSaveAndIndependentRestart` — полный запуск без города, сохранение и повторный старт, PASSED.
- Адресная регрессия ScenarioGameStarterTest и ScenarioCityMapFormatTest — PASSED.
- BUILD после производственных изменений — PASSED.

## Ещё не реализовано

Сюжетные события, диалоги, последствия, видео, формы героев/зон/событий и демонстрационный сценарий остаются следующими этапами. Ручная визуальная приёмка не выполнялась. Начальные армии героев используют существующие defaults.

[План](Plan/PLAN.md) · [Исследование](Research/RESEARCH.md) · [Этапы](Plan/STEPS.md) · [Файлы](Plan/files.md)
