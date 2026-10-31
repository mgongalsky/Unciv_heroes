# Как читать код рефакторинга городов

Русские пояснения находятся рядом с классами, тестами и неочевидными участками. В больших legacy-файлах они сосредоточены на изменённых частях.

## Состояние и игровой мир

Начните с `core/src/com/unciv/pure/domain/city/CityState.kt`: локальное состояние, координаты, история и владение. `CityRegistry.kt` хранит состав городов и проверяет уникальность ID и центра. `CityPopulationRules.kt` содержит игровые формулы населения.

`logic/city/CityInfo.kt` связывает состояние с игровыми менеджерами. `WorldCityRegistry.kt` предоставляет цивилизациям списки городов через единый реестр. Для города без цивилизации используется `ownerOrNull`.

`CityFounding.kt` выполняет обычное основание, `CityInfoConquestFunctions.kt` — завоевание, `NeutralCityOwnership.kt` — нейтрализацию и получение пустого города.

## Сохранения

`GameInfo.write/read` сохраняет города корневой коллекцией и восстанавливает через реестр. `CitySaveEntry.kt` хранит владение, `CitySaveValidation.kt` проверяет определения и территорию. Самостоятельное чтение JSON города ещё не определяет владельца.

`pure/domain/army/ArmyCopy.kt` глубоко копирует активную армию. Старый `ArmyInfo.clone` сохраняет контракт с общими ссылками на войска.

Исторические данные описаны в [README ресурсов города](../../../tests/resources/city/README.md), ручной сценарий — в [README ручной проверки](../../../tests/resources/manual/README.md). JSON-фикстуры не содержат комментариев.

## Нейтральный город

`NeutralCityEntryRules.kt` решает, допустимы ли вход и бой. `NeutralCityEntry.kt` связывает правило с движением: пустой город переходит игроку при фактическом входе героя в центр.

`NeutralCityBattleSession.kt` готовит бой человека и AI, однократно учитывая мировую атаку. `BattleWorldOutcomeHandler.kt` применяет исход после очистки погибших войск. Нейтральная фаза `GameInfo.nextTurn` сохраняет статичное состояние города на границе раунда.

## Сценарий: domain и application

`pure/domain/city/ScenarioCity.kt` — исходное описание города и слота. `ScenarioCityRules.kt` содержит локальные инварианты одного города; ему не нужны карта и список участников. `ScenarioCityCapitals.kt` выбирает единственную столицу по правилам сценария.

`pure/application/scenario/ScenarioPlayerBindings.kt` разрешает назначения участников, `ScenarioPlayerSlotDefaults.kt` подбирает свободные слоты. `ValidateScenarioUseCase.kt` проверяет согласованность миссии: ID, ссылки на слоты, границы карты и межгородские пересечения. Чистота кода сама по себе не делает эти операции доменными.

`pure/application/city/PrepareScenarioCitiesUseCase.kt` координирует подготовку и возвращает отделённые от карты данные. Игровой `logic/city/InstantiateScenarioCitiesUseCase.kt` проверяет окружение и создаёт CityInfo только при запуске миссии. Загрузка партии восстанавливает сохранённые города.

`ScenarioCityEditorCommands.kt` применяет изменения описаний. `MapEditorCitiesTab.kt`, `MapEditorPlayersTab.kt` и `ScenarioCityEditorPanel.kt` связывают команды с формами. Поля города остаются черновиком до Apply; слоты меняются отдельными командами. Выбор спрайта общий для превью и мира в `TileGroup.getShownImprovement`.

## Проверки

Быстрый набор — `com.unciv.testing.pure.CityTestSuite`: 42 теста без GameInfo, движка, DI и файлов. Структура и правила расширения описаны в [README чистых тестов](../../../tests/src/com/unciv/testing/pure/README.md).

`tests/src/com/unciv/testing/city/` сохраняет интеграционные проверки игрового мира, сохранений и адаптеров. `CityTestFixture.kt` управляет минимальным миром и глобальным состоянием. `ScenarioGameStarterTest.kt` проверяет полный запуск, `NeutralCityManualSaveTest.kt` генерирует ручной ресурс.

`ui/playground` проверяет формы и акторов. `capture-only` сохраняет actual без сравнения с эталоном. `UiGoldenTest#cityEditor` сравнивает, `UiGoldenUpdateTest#cityEditor` обновляет эталон; захват не заменяет визуальную приёмку.

## Дополнительные материалы

[POSTMORTEM.md](POSTMORTEM.md) описывает историю и ограничения рефакторинга. Правила чистых тестов, выбора слоя и русских комментариев закреплены в `.claude/skills/write-unittest/SKILL.md` и `.claude/skills/feathers-sprout/SKILL.md`. Навык `ui-golden-tests` описывает графические проверки. Переводы содержат подписи форм. Изменение `MapEditorMainTabs.kt` в исходной ветке ограничивалось пустой строкой.
