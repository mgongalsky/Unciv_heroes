# Отдельный запуск города

## Запуск из IDE

Создать Application-конфигурацию:

- Main class: com.unciv.app.desktop.DesktopLauncherCity.
- Classpath/module: desktop (его JVM runtime classpath).
- Working directory: $PROJECT_DIR$/android/assets.
- Аргументы не требуются.

Открывается окно Heroic Civs - City Sandbox размером 1280×800 с существующим CityScreen. Используются текущие настройки шрифта, скина и масштаба. Для просмотра текущего Royal-оформления нужен скин Minimal. Этот вход не внедряет новую раскладку handoff-бандла.

## Путь выполнения

DesktopLauncherCity настраивает LibGDX и DI → UncivGame загружает ресурсы → initialScreenFactory вызывает CitySandbox.createScreen → создаются небольшой GameInfo, карта, Rome, город и герой → ImageGetter получает ruleset → CityScreen открывается с явным CityScreenContext.

CitySandbox создаёт карту радиуса 6, город City Sandbox с очередью Granary, посещающего Warrior и отряд Spearman. Вместимость армии берётся из настроек игры. Данные предназначены для автономного просмотра; каждый запуск создаёт новый сценарий.

WorldScreen, GameStarter, loadGame и цикл ходов не запускаются. Минимальный GameInfo остаётся окружением модели города, но не записывается в UncivGame.gameInfo. Автосохранение партии поэтому не получает сценарий; штатные настройки приложения могут сохраняться. CityScreen не отмечает городские задачи обучения в режиме песочницы.

## Контекст и пересоздание

CityScreenContext находится в UI, поскольку описывает подключение экрана к источнику разрешений. Он не дублирует игровые правила. fromWorld сохраняет штатные разрешения мира, включая сетевые и сюжетные ограничения. Песочница разрешает работу с собственным состоянием в памяти.

CityScreen.createCityScreen сохраняет контекст. Его используют recreate/page, переименование в CityScreenCityPickerTable, покупка клетки и переход в CityScreenTileTable. ConstructionInfoTable и SpecialistAllocationTable читают isPlayersTurn через экран.

## Проверки

Native checks:

- BUILD — успешно, 36 с.
- TESTS com.unciv.testing.UiGoldenTest#citySandboxRender — успешно, 35 с.
- TESTS com.unciv.testing.UiGoldenTest#cityScreenRender — успешно, 30 с.
- TESTS com.unciv.testing.UiGoldenTest#compactCityScreenRender — успешно, 31 с.

Все три метода запускались отдельно, по одному тесту без ошибок. git diff --check HEAD прошёл. Изменений чистого ядра нет, поэтому новые pure unit-тесты не добавлялись.

Сценарий city-sandbox проверяет производственную фабрику, модель и пересоздание; он первым стоит в списке runner, до сценариев, публикующих партию в приложении. Захват находится в tests/golden/results/<platform>/city-sandbox-actual.png. Capture-only не подтверждает визуальное соответствие.

Для будущего принятого эталона предусмотрены com.unciv.testing.UiGoldenTest#citySandbox и com.unciv.testing.UiGoldenUpdateTest#citySandbox. Эталон в этой задаче не создавался, Update не запускался. Не использовать обновление эталона для сокрытия регрессии.

## Ручной просмотр

Запустить main, проверить карту и армию героя, выбрать клетку и постройку, переименовать город, изменить размер окна, выйти и запустить повторно. Ожидается исходный City Sandbox после нового запуска. Проверка реального изменения окна, диалогов действий и выхода пользователем ещё не выполнена.

Подробное покрытие и ограничения: [проверки](Plan/STEP_02_Verification.md). Новый дизайн, полный перенос шести вкладок и новая графика требуют следующего согласованного этапа.

[План](Plan/PLAN.md) · [Исследование](Research/RESEARCH.md) · [Файлы](Plan/files.md)
