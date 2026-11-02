# Перечень файлов

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

## Дальнейшие кандидаты

SkinStrings.kt, общий Popup.kt и остальные экраны требуют отдельного аудита. В городе остаются согласованная раскладка всех панелей, выбор города и другие элементы. CityScreenGoldenChecks дополнительно наполняет разделы подготовленных событий и великих людей существующими определениями ruleset; применение игровых эффектов остаётся вне этой проверки раскладки.

## Ресурсы и исключения

android/assets/UI_bundles/ существовал до задачи и не отслеживался Git. Используемые изображения и шрифты необходимо учитывать с лицензиями; весь каталог автоматически не включать в коммит.

Не включать результаты захвата, lasterror.txt, локальные настройки, сборочные каталоги и прежние пользовательские изменения, включая wiki/Make-sprite-atlas.md. Untracked-эталон не считается созданным этой работой только по его наличию в рабочем каталоге.

[План](PLAN.md)
