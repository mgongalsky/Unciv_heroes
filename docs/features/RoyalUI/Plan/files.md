# Перечень файлов

## Итерация Stats: граница данных

Фактические пути относительно корня проекта:

- core/src/com/unciv/pure/application/city/CityStatsSnapshot.kt — независимые идентификаторы и защищённые копии коллекций снимка.
- core/src/com/unciv/ui/cityscreen/CityStatsSnapshotAdapter.kt — чтение готовых расчётов legacy и отдельного счастья.
- core/src/com/unciv/ui/cityscreen/CityIncomeTable.kt — таблица по DTO, форматирование, прокрутка и детализация источников.
- core/src/com/unciv/ui/cityscreen/CityStatsTable.kt — подключение новой сводки для Minimal с сохранением прежних разделов.
- desktop/src/com/unciv/app/desktop/CityPreviewCapture.kt — режим CITY_PREVIEW_STATS=1.
- tests/src/com/unciv/testing/pure/application/city/CityStatsSnapshotTest.kt — три проверки изоляции снимка.
- tests/src/com/unciv/testing/pure/CityTestSuite.kt — регистрация нового класса.
- docs/features/RoyalUI/Plan/PLAN.md, STEPS.md, files.md, STEP_07_City_Tiles_Stats.md, Research/RESEARCH.md и CODE_GUIDE.md — описание реализации и проверки.

CityScreen.kt в этой итерации не изменялся. Новые изображения не добавлялись. Захваты и журналы в desktop/build/city-preview не предназначены для коммита. Посторонние файлы сохранены.

## Текущая итерация: вкладка Tile

Фактические файлы относительно корня проекта:

- core/src/com/unciv/ui/cityscreen/CityTileSummary.kt — новая сводка клетки, отдельное превью и сетка доходов.
- core/src/com/unciv/ui/cityscreen/CityScreenTileTable.kt — встроенный режим, пустое состояние и переиспользование свойств и действий.
- core/src/com/unciv/ui/cityscreen/CityScreen.kt — вкладка Tile, автоматический переход, подсветка и восстановление выбора.
- desktop/src/com/unciv/app/desktop/CityPreviewCapture.kt — режим захвата Tile через CITY_PREVIEW_TILE=1, открытие Map на узком окне перед поиском клетки.
- docs/features/RoyalUI/Plan/PLAN.md, STEPS.md, files.md, STEP_07_City_Tiles_Stats.md, Research/RESEARCH.md и CODE_GUIDE.md — границы и фактические результаты.

Новые изображения и тесты не добавлялись. PNG, отчёты и журналы захвата находятся в desktop/build/city-preview и не предназначены для коммита. Посторонний wiki/Make-sprite-atlas.md не изменялся этой работой.

## Описание проекта и полная очередь — результат итерации

Фактические изменения итерации: core/src/com/unciv/ui/cityscreen/CityConstructionsTable.kt — переключение каталога/описания, локальные действия, полная очередь; ConstructionInfoTable.kt в том же каталоге — встроенная карточка; CityScreen.kt — переход на Construction; CitySandbox.kt — Granary, Worker, Scout и частичный прогресс. Обновлены Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Plan/STEP_04_City_Visual_Shell.md, Research/RESEARCH.md и CODE_GUIDE.md. Новых файлов нет.

Native BUILD прошёл за 11 секунд. UI/golden-тесты и ручная приёмка не выполнялись. Это описание заменяет прежнее решение скрывать текущий проект из нижней очереди.


## Исправления: компактные действия и очередь

После просмотра пользователь отметил переполнение Exit city. В CityCompactHeader.kt выход использует размер 104×32 и текст 16 UI units вместо произвольного масштаба 0.8. HeroicCityStyle.kt содержит отдельный компактный стиль azure без ланцетов; размер надписи рассчитывается через Fonts.ORIGINAL_FONT_SIZE, общий BitmapFont не меняется. Маркер стиля сохраняет компактное оформление при обходе кнопок и обновлении доступности.

CityConstructionsTable.kt показывает в нижней очереди только ожидающие проекты с исходными индексами от 1. Текущий проект остаётся в шапке; кнопка многоточия выбирает его и открывает доступные действия покупки в существующей полосе, стрелка вниз понижает приоритет, крестик отменяет проект. Плюс каталога и действия очереди имеют размер 32×32. Запрет изменения города проверяется перед отменой и перестановкой. Используются прежние игровые операции, модель очереди и прогресса не меняется. Это заменяет прежнее описание дублирования текущего проекта внизу.

Фактические файлы этой правки: core/src/com/unciv/ui/cityscreen/CityCompactHeader.kt, HeroicCityStyle.kt, CityConstructionsTable.kt и эти документы темы. Native BUILD прошёл за 11 секунд; diff --check прошёл до сборки. UI/golden-тесты не запускались по согласованному ограничению.

Ручная приёмка ещё не выполнена: проверить надпись выхода, добавить два проекта и проверить независимую прокрутку, выбор/покупку текущего проекта, отмену и обе стрелки изменения порядка; проверить город без разрешения на изменения. Прочие замечания о свитках, вкладках, материалах и карте остаются в плане этапа 04.


## Итерация: компактная шапка и независимая очередь

Реализован следующий шаг после checkpoint 2c7ebd942. CityCompactHeader.kt показывает название, население, статусы и выход в одной строке, реальные доходы с существующим выбором фокуса, текущую стройку с прогрессом и расчётными ходами. Шапка обновляется из CityScreen.updateCompactLayout; CityInfo остаётся источником состояния. CityScreenCityPickerTable.kt не изменён и сохраняется для legacy-пути.

CityConstructionsTable.kt использует ширину контейнера для строк. Каталог и очередь получают независимые ScrollPane внутри страницы Construction, а действия добавления/покупки — отдельную горизонтальную строку. Асинхронное обновление каталога больше не переопределяет размеры компактных областей. Существующие обработчики покупки, добавления, отмены и изменения порядка сохранены; модель прогресса и сохранения не менялись. В очереди пока сохранено legacy-представление, включая текущий проект; отдельные компактные строки и drag-and-drop ещё не реализованы.

Фактически изменены core/src/com/unciv/ui/cityscreen/CityScreen.kt и CityConstructionsTable.kt; создан CityCompactHeader.kt. Обновлены Plan/PLAN.md, STEPS.md, files.md, STEP_04_City_Visual_Shell.md, Research/RESEARCH.md и CODE_GUIDE.md. Изменения относятся к UI-адаптерам, нового чистого игрового правила нет.

Финальный native BUILD прошёл за 11 секунд. UI/golden-тесты не запускались по согласованному ограничению. Визуальная и функциональная ручная приёмка этой итерации ещё не выполнена: открыть DesktopLauncherCity и обычный город, проверить смену приоритета и обновление доходов/ETA, прокрутить каталог и очередь независимо, проверить покупку/добавление, длинное название, узкое окно и выход. Сборка не подтверждает отсутствие визуальных перекрытий.

Полный перечень оставшихся замечаний находится в Plan/STEP_04_City_Visual_Shell.md: финальные свитки, шесть вкладок, селектор категорий, компактные строки и описания, drag-and-drop, лавровый узор, рисованные ресурсы и спокойные маркеры карты. Полный этап 04 остаётся IN_PROGRESS; текущая итерация готова к просмотру.


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


## Первый MVP нового города

- core/src/com/unciv/ui/cityscreen/CityScreen.kt — общая компоновка Minimal для игры и песочницы, квадратная карта справа при ширине от 900, существующие панели во вкладках, сохранение навигации выбора улучшения.
- core/src/com/unciv/ui/cityscreen/HeroicCityStyle.kt — новый локальный набор Drawables; текстуры принадлежат Skin, глобальные стили не заменяются.
- docs/features/RoyalUI/Plan/PLAN.md, STEPS.md, STEP_04_City_Visual_Shell.md, CITY_HANDOFF_FILES.md, files.md и ../CODE_GUIDE.md — фактический объём MVP, результат BUILD и маршрут просмотра.

Используемые PNG и ограничения перечислены в [CITY_HANDOFF_FILES.md](CITY_HANDOFF_FILES.md). Тесты и эталоны в этом MVP не менялись.


Пути относительно корня проекта. Перечень отражает текущую частичную реализацию; окончательный состав сверяется с Git diff, включая staged-изменения.

## Документация

В docs/features/RoyalUI: Research/RESEARCH.md, Plan/PLAN.md, Plan/STEPS.md, Plan/STEP_01_Theme.md, Plan/STEP_02_Screens.md, Plan/STEP_03_Verification.md, Plan/files.md и CODE_GUIDE.md.

## Общая тема

- core/src/com/unciv/ui/utils/RoyalUi.kt — новый компонент кнопок и типографики.
- core/src/com/unciv/ui/utils/RoyalPanelDrawable.kt — новые адаптивные пергаментные рамки.
- core/src/com/unciv/ui/utils/RoyalRibbonDrawable.kt — новая непрерывная лента вкладок.
- core/src/com/unciv/ui/utils/BaseScreen.kt — тема Minimal и атлас на экземпляр приложения.
- core/src/com/unciv/ui/utils/TabbedPager.kt — лента, подписи и отключение вкладок.
- core/src/com/unciv/ui/utils/ExpanderTab.kt — локальное оформление и перенос заголовка через setHeaderAppearance.
- core/src/com/unciv/ui/utils/Fonts.kt — контракт комплектного шрифта.
- desktop/src/com/unciv/app/desktop/NativeFontDesktop.kt — TTF и fallback Desktop.
- android/src/com/unciv/app/NativeFontAndroid.kt — TTF и fallback Android; платформенная проверка ещё нужна.
- core/src/com/unciv/ui/civilopedia/FormattedLine.kt — локальный цвет обычного текста.
- core/src/com/unciv/ui/civilopedia/MarkupRenderer.kt — renderWithTextColor с сохранением прежнего render.

## Экраны и панели

- core/src/com/unciv/ui/army/SplitTroopPopup.kt — разделение отряда; пакет com.unciv.ui.popup.
- core/src/com/unciv/ui/army/UnitInfoPopup.kt — внешнее оформление характеристик.
- core/src/com/unciv/ui/battlescreen/BattleTroopInfoPanel.kt — оформление, обновление и размещение подсказки.
- core/src/com/unciv/ui/battlescreen/BattleAttackPreviewTable.kt — явный параметр светлого прогноза.
- core/src/com/unciv/ui/battlescreen/BattleResultPopup.kt — адаптивный результат боя и прокрутка.
- core/src/com/unciv/ui/heroscreen/HeroOverviewScreen.kt — характеристики героя.
- core/src/com/unciv/ui/popup/ConfirmPopup.kt — адаптивное подтверждение.
- core/src/com/unciv/ui/popup/AskTextPopup.kt — ввод текста и ошибка.
- core/src/com/unciv/ui/popup/AskNumberPopup.kt — ввод числа и сетка действий.
- core/src/com/unciv/ui/cityscreen/ConstructionInfoTable.kt — карточка постройки с переносом и прокруткой.
- core/src/com/unciv/ui/cityscreen/CityScreenTileTable.kt — карточка клетки, локальные цвета и прокрутка; исправлен недопустимый литерал цвета.
- core/src/com/unciv/ui/cityscreen/CityConstructionsTable.kt — контейнеры очереди и списка, перенос строк, действия и цвета индикатора.
- core/src/com/unciv/ui/cityscreen/CitizenManagementTable.kt — светлые кнопки населения и выбранный фокус.
- core/src/com/unciv/ui/cityscreen/HeroSupplyTable.kt — подписи, отдельная строка ползунка и оформление автоснабжения.
- core/src/com/unciv/ui/cityscreen/SpecialistAllocationTable.kt — перенос мест и показателей специалистов.
- core/src/com/unciv/ui/cityscreen/CityReligionInfoTable.kt — явный параметр городской светлой раскладки с сохранением варианта карты.
- core/src/com/unciv/ui/cityscreen/CityStatsTable.kt — сводка, здания, события, великие люди и общая прокрутка Minimal.
- core/src/com/unciv/ui/cityscreen/CityScreen.kt — передача доступной высоты общей панели статистики.

## Выбор города и армейские слоты

- core/src/com/unciv/ui/cityscreen/CityScreenCityPickerTable.kt — светлый фон Minimal, перенос названия и отдельная строка значков состояний.
- core/src/com/unciv/ui/army/TroopArmyView.kt — общий белый ресурс вместо двух текстур на слот, локальные цвета фона и численности.
- core/src/com/unciv/ui/army/ArmyView.kt — перенос слотов только в CityScreen с Minimal; индексы сохраняются.
- CityConstructionsTable резервирует фактическую высоту гарнизона до асинхронного наполнения списка.

## Уведомления и подробная статистика

- core/src/com/unciv/ui/popup/ToastPopup.kt — оформление Minimal, ограничение размеров и отмена Job таймера при скрытии или закрытии.
- core/src/com/unciv/ui/cityscreen/DetailedStatsPopup.kt — ограниченная область таблицы, прокрутка по обеим осям и объединение счастья на копиях данных.
- desktop/src/com/unciv/app/desktop/ToastPopupGoldenChecks.kt — размеры уведомлений, прокрутка, закрытие нажатием и однократное продвижение очереди.
- desktop/src/com/unciv/app/desktop/CityScreenGoldenChecks.kt — размеры подробной статистики, закрытие и неизменность модели при повторном открытии.
- UiGoldenTestRunner и UiGoldenTest регистрируют toast-popup-layout и toastPopupRender.

## Проверки и жизненный цикл

- core/src/com/unciv/utils/concurrency/Concurrency.kt — GL-диспетчер на экземпляр приложения.
- tests/src/com/unciv/testing/UiGoldenTest.kt — сценарии рендеринга и повторных запусков.
- desktop/src/com/unciv/app/desktop/UiGoldenTestRunner.kt — регистрация сценариев и ожидание асинхронного открытия города.
- desktop/src/com/unciv/app/desktop/BattleResultLayoutChecks.kt — новая проверка размеров, прокрутки и Continue.
- desktop/src/com/unciv/app/desktop/HeroOverviewGoldenChecks.kt — новая заготовка мира и проверка пересоздания обзора героя.
- desktop/src/com/unciv/app/desktop/ConfirmPopupGoldenChecks.kt — новая проверка расположения и callbacks.
- desktop/src/com/unciv/app/desktop/AskTextPopupGoldenChecks.kt — новая проверка фокуса, фильтра и валидации.
- desktop/src/com/unciv/app/desktop/AskNumberPopupGoldenChecks.kt — новая проверка торговых шагов, ограничений и валидации.
- desktop/src/com/unciv/app/desktop/CityScreenGoldenChecks.kt — новая заготовка настоящего города с героем, специалистами и религией; проверки карточек, разделов, общей высоты и прокрутки статистики.
- desktop/build.gradle.kts — прямая зависимость kotlinx-coroutines-core 1.6.1 для городского сценария.
- core/src/com/unciv/ui/playground/BattleTroopPreview.kt — размещение прогноза у краёв и восстановление масштаба.
- core/src/com/unciv/ui/playground/ScenarioCityEditorTabsPreview.kt — отключение и повторное включение вкладок.

## Главное меню

- core/src/com/unciv/MainMenuScreen.kt — общий Royal-стиль кнопок, перенос подписей и выбор числа колонок по логической ширине.
- desktop/src/com/unciv/app/desktop/MainMenuGoldenChecks.kt — ожидание фоновой карты, проверка границ, длинной подписи и доступности последнего пункта после кадра прокрутки.
- UiGoldenTestRunner и UiGoldenTest регистрируют main-menu и mainMenuRender; визуальный эталон не утверждается.

## Игровое меню

- core/src/com/unciv/ui/worldscreen/mainmenu/WorldScreenMenuPopup.kt — пергаментная рамка, перенос подписей и ограничения размеров меню Minimal.
- desktop/src/com/unciv/app/desktop/WorldMenuGoldenChecks.kt — размеры, длинная подпись, прокрутка и закрытие через Stage.
- CityScreenGoldenChecks вызывает проверку игрового меню после загрузки мира в сценарии empire-overview-stats.

## Дальнейшие кандидаты

SkinStrings.kt, общий Popup.kt и остальные экраны требуют отдельного аудита. В городе остаются согласованная раскладка всех панелей, выбор города и другие элементы. CityScreenGoldenChecks дополнительно наполняет разделы подготовленных событий и великих людей существующими определениями ruleset; применение игровых эффектов остаётся вне этой проверки раскладки.

## Ресурсы и исключения

android/assets/UI_bundles/ существовал до задачи и не отслеживался Git. Используемые изображения и шрифты необходимо учитывать с лицензиями; весь каталог автоматически не включать в коммит.

Не включать результаты захвата, lasterror.txt, локальные настройки, сборочные каталоги и прежние пользовательские изменения, включая wiki/Make-sprite-atlas.md. Untracked-эталон не считается созданным этой работой только по его наличию в рабочем каталоге.

[План](PLAN.md)

## Настройки: точка просмотра

- core/src/com/unciv/ui/options/OptionsPopup.kt — рамка, размеры, перенос флажков и адаптивные строки.
- core/src/com/unciv/ui/options/AboutTab.kt — явная ширина пергаментного содержимого.
- core/src/com/unciv/ui/options/DisplayTab.kt — адаптация двух ползунков, четырёх списков и пояснения.
- core/src/com/unciv/ui/options/GameplayTab.kt — адаптация строки истории уведомлений.
- desktop/src/com/unciv/app/desktop/OptionsPopupGoldenChecks.kt — новый сценарий размеров и закрытия.
- desktop/src/com/unciv/app/desktop/ConfirmPopupGoldenChecks.kt — подключение сценария настроек.
- docs/features/RoyalUI/Plan/PLAN.md, STEPS.md, STEP_02_Screens.md, STEP_03_Verification.md, files.md и ../CODE_GUIDE.md — граница этапа, покрытие и маршрут просмотра.


## Компактная сводка Stats

Компактная компоновка меняет только CityIncomeTable.kt и документы RoyalUI. DTO, адаптер чтения, игровые формулы и тесты не меняются. Захваты stats-compact-1568-690.png и stats-compact-400-690.png находятся в desktop/build/city-preview и не предназначены для коммита.


## Buildings и городская полоса вкладок

Фактические Kotlin-файлы: core/src/com/unciv/pure/application/city/CityBuildingsSnapshot.kt; core/src/com/unciv/ui/cityscreen/CityBuildingsSnapshotAdapter.kt, CityBuildingsTable.kt, CityTabsDrawable.kt, CityScreen.kt, CityStatsTable.kt, CitySandbox.kt; core/src/com/unciv/ui/utils/TabbedPager.kt; desktop/src/com/unciv/app/desktop/CityPreviewCapture.kt; tests/src/com/unciv/testing/pure/application/city/CityBuildingsSnapshotTest.kt и tests/src/com/unciv/testing/pure/CityTestSuite.kt. Обновлены PLAN.md, STEPS.md, files.md, STEP_07_City_Tiles_Stats.md, Research/RESEARCH.md, CODE_GUIDE.md. CityTabsDrawable использует существующие frames/azure/scroll-left.png, scroll-right.png, scroll-centre-tile.png, materials/azure/cloth-tab.png и png/frames/azure/tabs-trim-420x64.png из libgdx-ready городского бандла. Захваты остаются в desktop/build/city-preview. Посторонний wiki/Make-sprite-atlas.md не относится к задаче.


## Текущая итерация: слоты и Supply

Фактические файлы итерации: core/src/com/unciv/ui/army/TroopArmyView.kt — фон, рамка и размеры слота; core/src/com/unciv/pure/application/city/CityHeroSupplySnapshot.kt — независимый контракт значений; core/src/com/unciv/ui/cityscreen/CityHeroSupplyAdapter.kt — чтение и проверенные команды; CityHeroSupplyTable.kt — панель и ползунок; CityScreen.kt — подключение к Army и обновление; CityStatsTable.kt — устранение дублирования; CitySandbox.kt — отдельные запасы режима захвата. Последние пять имён относятся к core/src/com/unciv/ui/cityscreen/. desktop/src/com/unciv/app/desktop/CityPreviewCapture.kt — CITY_PREVIEW_ARMY. Документы: Plan/PLAN.md, STEPS.md, files.md, STEP_08_City_Army_Supply.md, Research/RESEARCH.md и CODE_GUIDE.md. ArmyView.kt и игровые формулы не менялись. Новые изображения и тесты не добавлялись. Захваты в desktop/build/city-preview не предназначены для коммита. wiki/Make-sprite-atlas.md — посторонняя пользовательская правка.


## Городские всплывающие окна: единый стиль

Фактические файлы итерации окон: core/src/com/unciv/ui/cityscreen/CityPopupStyle.kt — локальный стиль городской сцены; core/src/com/unciv/ui/popup/Popup.kt — применение до измерения и обновление динамических элементов; core/src/com/unciv/ui/cityscreen/TestFantasyPopup.kt — цвета итогов и выделения; core/src/com/unciv/ui/battlescreen/BattleTroopInfoPanel.kt — рамка временной карточки в городе; desktop/src/com/unciv/app/desktop/CityPreviewCapture.kt — режимы CITY_PREVIEW_POPUP. Документы: Plan/PLAN.md, STEPS.md, files.md, STEP_02_Screens.md, Research/RESEARCH.md, CODE_GUIDE.md. Новые изображения и тесты не добавлялись. Результаты захватов находятся в desktop/build/city-preview. Изменения Supply относятся к предыдущей итерации; docs/Modders/Creating-a-UI-skin.md и wiki/Make-sprite-atlas.md не относятся к этой задаче.
