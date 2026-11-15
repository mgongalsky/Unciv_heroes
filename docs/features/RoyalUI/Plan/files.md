# Перечень файлов

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
