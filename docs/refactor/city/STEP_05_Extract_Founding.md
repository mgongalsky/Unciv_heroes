# STEP 05 — Извлечение основания из конструктора и UI

Статус: завершён. Зависимость: [STEP 04](STEP_04_Persistence_Characterization.md).

## Результат

Последовательность основания перенесена из CityInfo в CityFounding вместе с выбором имени, стартовыми зданиями и дипломатическим эффектом соседства. Сохранён порядок гарнизона, transients, имени, истории столицы, регистрации, зданий, территории, дороги, очистки центра, населения, религии и дипломатии.

- CityFounding.create создаёт пустой CityInfo и выполняет основание с единственной регистрацией. Не выбирает производство и не уничтожает основателя.
- CityFounding.initialize — внутренний общий путь для create и совместимого конструктора CityInfo(civInfo, location, founder).
- CivilizationInfo.addCity делегирует create, затем по-прежнему выбирает производство.
- CityFounding.complete выполняет прежние игровые эффекты действия: addCity, улучшение центра, удаление дороги и уничтожение основателя.
- CityInfo() остаётся доступен для создания состояния и JSON без основания города.

## Граница UI

UnitActions сохраняет проверки доступности, подтверждение нарушения обещания, звук и callbacks внешних эффектов. Tutorial вызывается перед CityFounding.complete, обновление экрана — после неё. Игровые эффекты действия больше не дублируются в UI.

Проверены callers: production использует addCity и UnitActions; параметризованный конструктор также нужен CityTestFixture, характеристическим тестам и TestGame. Совместимый конструктор сохранён. Новые переопределяемые методы для тестирования не вводились.

## Проверки

Сборка прошла. После извлечения выполнены 25 тестов:

- CityConstructionTest — 3.
- CityFoundingCharacterizationTest, CityFoundingActionTest, CityFoundingDiplomacyTest — по 1.
- CityFoundingAvailabilityTest — 3.
- CityOwnershipCharacterizationTest, CityTurnCharacterizationTest, CityRecaptureCharacterizationTest, CitySupplyAndDefeatTest — по 2.
- CityDestructionDisplacementTest — 1.
- CityPersistenceCharacterizationTest — 2.
- CityGamePersistenceTest, CitySaveFormatTest, CityLegacyFixtureTest — по 1.
- SerializationTests — 2.

Все прошли. Проверены оба совместимых пути основания, полное действие человека и AI, подтверждение обещания, жизненный цикл, копирование и сохранения. После этого выполнена только очистка форматирования, импортов и комментариев.

## Ограничения

Операция пока использует legacy-модель, CivilizationInfo, MapUnit и LibGDX-координаты. Это извлечение поведения, а не чистое ядро. Нейтральность, формат сохранений, стоимость основания, дефекты копирования и владельца гарнизона не менялись. Проверки доступности остаются на прежней границе; complete предполагает, что вызывающая сторона уже их выполнила.
