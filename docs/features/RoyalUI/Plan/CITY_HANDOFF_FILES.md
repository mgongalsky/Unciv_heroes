# Файлы нового городского handoff

Дополнение к [files.md](files.md), а не перечень уже реализованных изменений. Все пути относительно корня проекта. Имена новых исходников предварительные; перед созданием проверить существующие компоненты и уточнить границы. Не создавать все перечисленные классы заранее.

## Фактические изменения текущей задачи: только документация

Изменены docs/features/RoyalUI/Plan/PLAN.md и STEPS.md.
Созданы docs/features/RoyalUI/Research/CITY_HANDOFF.md, Plan/CITY_HANDOFF_FILES.md и Plan/STEP_04_City_Visual_Shell.md … STEP_09_City_Integration.md.

Игровые исходники, ресурсы, тесты и существующий CODE_GUIDE.md в задаче планирования не меняются. Путеводитель описывает текущий код; будущая архитектура находится в PLAN.md.

## 04. Локальная графика и layout

Предварительно создать под core/src/com/unciv/ui/cityscreen/: HeroicCityView.kt (корневой Actor/компоновка), HeroicCityStyle.kt (локальные стили/владение ресурсами), CityMapView.kt (переиспользуемый контейнер карты, если извлечение оправдано).

Изменить CitySandbox.kt для dev-включения нового варианта; CityScreen.kt/CityTileGroup.kt — только необходимые границы композиции. DesktopLauncherCity.kt менять лишь при необходимости явного выбора режима.

Ресурсы: предполагаемый runtime-каталог android/assets/ui/city/ и входы существующего TexturePacker pipeline. Окончательные пути atlas/png/fonts определить по действующему упаковщику до импорта. Не добавлять весь UI_bundles. Состав выбранных регионов и лицензии внести сюда по факту.

## 05. Данные и состояние представления

Предварительные CityPresentationState.kt и CityViewAdapter.kt под core/src/com/unciv/ui/cityscreen/. Ответственность: навигация/прокрутка и чтение CityInfo; не собственная игровая модель. При необходимости отдельные файлы вкладок в том же пакете.

Точки чтения: CityStatsTable.kt, DetailedStatsPopup.kt, CityScreenTileTable.kt, CityScreenCityPickerTable.kt, ConstructionInfoTable.kt, VisitingHeroTable.kt, ui/heroscreen/HeroOverviewScreen.kt. Их наличие в списке не означает обязательного изменения каждого файла.

## 06–07. Команды города

Кандидаты на изменение: core/src/com/unciv/ui/cityscreen/CityConstructionsTable.kt, CitizenManagementTable.kt, SpecialistAllocationTable.kt, CityScreenTileTable.kt, ConstructionInfoTable.kt, CityStatsTable.kt, DetailedStatsPopup.kt; core/src/com/unciv/logic/city/CityConstructions.kt только при необходимом общем адаптере операции.

Новая чистая операция, если требуется: core/src/com/unciv/pure/application/city/ReorderCityConstructionQueue.kt (предварительно). Не добавлять ID/миграцию без отдельного решения.

## 08. Армии и снабжение

Изучить и при необходимости изменить: core/src/com/unciv/logic/army/ArmyManager.kt, ArmyInfo.kt; core/src/com/unciv/ui/army/ArmyView.kt, TroopArmyView.kt; core/src/com/unciv/ui/cityscreen/HeroSupplyTable.kt; core/src/com/unciv/pure/application/CalculateFoodDistributionUseCase.kt, TransferFoodUseCase.kt, SupplyHeroMaximumUseCase.kt; core/src/com/unciv/pure/domain/hero/FoodDistributionState.kt, HeroWithSupply.kt.

Пути новых чистых правил: pure/domain/army или hero; координации — pure/application/city либо существующая предметная область. Точные классы определить после проверки контрактов, не заранее заводить дублирующие use case.

## Проверки 04–09

Расширить desktop/src/com/unciv/app/desktop/CityScreenGoldenChecks.kt и при разрастании выделить HeroicCityGoldenChecks.kt; подключить через UiGoldenTestRunner.kt и tests/src/com/unciv/testing/UiGoldenTest.kt / UiGoldenUpdateTest.kt.

Существующие кандидаты для расширения: tests/src/com/unciv/testing/city/CityConstructionTest.kt, CitySupplyAndDefeatTest.kt, CityGamePersistenceTest.kt; tests/src/com/unciv/testing/pure/application/TransferFoodUseCaseTest.kt, SupplyHeroMaximumUseCaseTest.kt. Сначала проверить реальную независимость старых pure-тестов.

Новые тесты чистого ядра — tests/src/com/unciv/testing/pure/domain/<area>/ и pure/application/<area>/; городские включить в CityTestSuite.kt. UI-состояние/геометрия — в UI-тестах, не игровом domain. GL/сериализацию не помещать в pure.

Принятые baseline: tests/golden/baseline/<platform>/; результаты tests/golden/results/ не коммитить. Обновление baseline отдельное, после просмотра.

## 09. Обычные входы и завершение

USAGES CityScreen определит фактические файлы подключения. Известные места: Notification.kt, CityOverviewTable.kt, CityButton.kt и WorldScreen.kt. Без необходимости не менять каждый вызов, если достаточно общей фабрики/существующего класса.

После реализации обновлять docs/features/RoyalUI/CODE_GUIDE.md, Plan/files.md, PLAN.md, STEPS.md, соответствующий STEP и Research/CITY_HANDOFF.md. Не переносить документацию завершённого CitySandbox и не смешивать посторонние правки пользователя.

[План](PLAN.md) · [Этапы](STEPS.md)
