# Файлы нового городского handoff

## Текущий шаг: синяя палитра и заполнение сцены

По следующему просмотру пользователь выбрал единый синий акцент. CityScreen убирает внешние зазоры, задаёт левой панели высоту сцены и локально очищает кадр пергаментным цветом. Общий BaseScreen.clearColor не меняется; дополнительная фоновая картинка не добавляется. Квадрат карты ограничен доступной высотой и читаемой шириной управления.

HeroicCityStyle применяет основы azure к городским TextButton, включая вновь созданные после обновления. Обход ограничен compactRoot, CheckBox пропускаются; стили помечаются и Drawables кешируются в Skin. Disabled сохраняет серую ткань. RoyalRibbonDrawable.useAzureSelection и TabbedPager.useAzureRoyalHeader включают синий акцент только у городского экземпляра ленты; остальные экраны сохраняют прежнее оформление.

Размер герба исправлен по secondary-decor.mjs: min(width * 0.78, clamp(width * 0.58, 220, 380) * 0.65). Это заменяет прежние 65% ширины карты. Пики не менялись. Runtime-основы кнопок снова png/skin/azure/button-base-{up,over,down,disabled}.9.png; sage этим helper больше не используется.

Затронуты CityScreen.kt, HeroicCityStyle.kt, ui/utils/RoyalRibbonDrawable.kt и TabbedPager.kt, а также документы темы. Первый BUILD этой версии превысил 300 секунд. После закрытия приложения повторный native BUILD прошёл за 3 секунды. Прежний успешный BUILD за 2 секунды относится к предыдущей версии. UI/golden-тесты не запускались по указанию пользователя. После завершения сборки проверить фон без синих просветов, синюю активную вкладку и кнопки, серое disabled-состояние и уменьшенный герб. Повторная визуальная приёмка пока не выполнена.


## Исправления после первого просмотра MVP

Пользователь подтвердил запуск первого MVP. В CityScreen название и выход перенесены в левую панель; квадрат карты справа занимает максимально доступную высоту с ограничением минимальной ширины управления. Декор карты и герб накладываются через Stack без резервирования отдельной нижней строки.

В HeroicCityStyle исправлена геометрия панели по brocade-frame.mjs (угол 82, оси 16/21); полосы повторяются без растяжения, остаток обрезается UV. Добавлен mapOverlay по twin-pike-frame.mjs: связанные головы пик, повторяемое древко, отражённая правая сторона и герб поверх карты. Кнопка выхода использует sage с ланцетами 18×32. Все изменения остаются в общем UI игры и песочницы.

Дополнительные runtime-ресурсы относительно libgdx-ready: frames/azure/pike-head-left.png, pike-shaft-tile-left.png, pike-foot-left.png; png/skin/sage/button-base-{up,over,down,disabled}.9.png. Основы кнопок azure больше не используются этим helper. PNG пока остаются в локальном handoff-бандле; весь бандл автоматически не включается в коммит.

После исправлений diff --check прошёл. Первый BUILD превысил 300 секунд. Повторный native BUILD по просьбе пользователя прошёл за 2 секунды. Прежний BUILD за 57 секунд относится к первой версии. UI/golden-тесты отложены пользователем. Повторная визуальная приёмка ещё не выполнена; полный этап 04 остаётся IN_PROGRESS. Это актуализирует описание первого MVP ниже.


## Фактический первый MVP

Создан core/src/com/unciv/ui/cityscreen/HeroicCityStyle.kt; изменён core/src/com/unciv/ui/cityscreen/CityScreen.kt. CitySandbox и DesktopLauncherCity используют тот же экран без отдельной реализации. Обновлены PLAN.md, STEPS.md, STEP_04_City_Visual_Shell.md, files.md, этот перечень и ../CODE_GUIDE.md. Предварительные классы ниже не создавались.

Runtime-корень MVP: android/assets/UI_bundles/Heroic-Civs-LibGDX-Handoff/heroic-civs-libgdx-handoff/libgdx-ready/. Используются:

- frames/azure/panel-corner-tl.png, panel-rail-horizontal.png, panel-rail-vertical.png;
- frames/azure/footer-shield-banner.png;
- png/skin/lancet-left.png, lancet-right.png;
- png/skin/azure/button-base-up.9.png, button-base-over.9.png, button-base-down.9.png, button-base-disabled.9.png.

Бандл уже находился в рабочем дереве и не отслеживался Git; MVP читает эти файлы непосредственно, не импортирует весь каталог. Для воспроизводимого коммита необходимы перечисленные ресурсы с соответствующими лицензиями. Перенос в отдельный runtime-каталог и упаковка в атлас пока не выполнены. BUILD прошёл за 57 секунд; он не подтверждает загрузку PNG во время запуска. UI/golden-тесты не запускались и не менялись по указанию пользователя.

Следующий текст — исходный план полного переноса, а не утверждение, что работа всё ещё ограничена документацией.


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
