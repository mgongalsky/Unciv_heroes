# Файлы HomogenousTerrain

[План](PLAN.md) · [Этапы](STEPS.md) · [Исследование](../Research/RESEARCH.md) · [Путеводитель](../CODE_GUIDE.md)

Пути относительно корня проекта. Перечень отражает реализацию, а не предварительные имена классов.

## Ядро

В `core/src/com/unciv/pure/domain/mapgeneration/`:

- PerlinNoise.kt — единственная реализация шума.
- ClimateGenerator.kt — климат, интервалы, классификация и связанные значения.
- ElevationGenerator.kt — рельеф и клеточные проходы.
- VegetationGenerator.kt — маска и выбор растительности.

В `core/src/com/unciv/pure/application/mapgeneration/`:

- TerrainGenerationPipeline.kt — контракты снимков, настройки, режимы, валидация и координатор.
- TerrainGridFactory.kt — геометрия и соседство без движка.
- TerrainMetrics.kt — компоненты, доли и границы.

Отдельные TerrainGenerationModel.kt и TerrainExperiment.kt не понадобились.

## Игровое подключение

| Путь | Назначение |
|---|---|
| core/src/com/unciv/logic/map/mapgenerator/TerrainGenerationAdapter.kt | Преобразование TileMap и применение этапов |
| core/src/com/unciv/logic/map/mapgenerator/TerrainRuleSnapshotAdapter.kt | Снимок фактических правил |
| core/src/com/unciv/logic/map/mapgenerator/TerrainPreviewMapAdapter.kt | Отдельный TileMap для игровых спрайтов |
| core/src/com/unciv/logic/map/mapgenerator/MapGenerator.kt | Делегирование трёх этапов; удаление неиспользуемых помощников после usages |
| core/src/com/unciv/logic/map/Perlin.kt | Совместимая обёртка |
| core/src/com/unciv/logic/map/MapParameters.kt | Новые масштабы, clone/reset/описание |
| core/src/com/unciv/ui/mapeditor/MapGeneratorSteps.kt | Передача масштабов соответствующим этапам |
| core/src/com/unciv/ui/mapeditor/MapEditorGenerateTab.kt | Снимок настроек до рабочего потока |
| core/src/com/unciv/ui/newgamescreen/MapParametersTable.kt | Ползунки общей формы |

EditorMapHolder используется без изменений. TileMap, TileInfo, Terrains.json и BattlefieldTerrain.kt не изменялись ради этой фичи.

## Инструменты и UI

| Путь | Назначение |
|---|---|
| tests/src/com/unciv/testing/simulations/terrain/TerrainExperimentCli.kt | Пакетная точка входа без зависимости от JUnit |
| desktop/src/com/unciv/app/desktop/terrain/TerrainExperimentSource.kt | Загрузка Terrains.json и создание исходной сетки |
| desktop/src/com/unciv/app/desktop/terrain/TerrainExperimentFiles.kt | JSON, версии, checksum, воспроизведение отчёта |
| desktop/src/com/unciv/app/desktop/terrain/TerrainExperimentExport.kt | CSV, легенда и PNG без OpenGL |
| desktop/src/com/unciv/app/desktop/DesktopLauncherTerrain.kt | Отдельный графический запуск и файловые callbacks |
| core/src/com/unciv/ui/mapeditor/TerrainPlaygroundScreen.kt | Управление экспериментом, слои, метрики и игровые спрайты |
| desktop/build.gradle.kts | runTerrain и каталог ассетов |
| tests/build.gradle.kts | runTerrainExperimentCli |

Core/UI не импортирует desktop или tests. TerrainImageExporter.kt из предварительного плана заменён TerrainExperimentExport.kt.

## Тесты

В `tests/src/com/unciv/testing/mapgeneration/`:

- TerrainGenerationCharacterizationTest.kt — 36 исторических отпечатков внутри класса, геометрия и RNG.
- TerrainParametersCompatibilityTest.kt — старые параметры, сериализация и копирование.
- TerrainExperimentFilesTest.kt, TerrainExperimentExportTest.kt, TerrainExperimentSourceTest.kt, TerrainExperimentCliTest.kt — файловые границы и CLI.
- TerrainFullGenerationTest.kt — повторяемость полного генератора и совместимость параметров.
- TerrainAcceptanceSeriesTest.kt — сохраняемая серия для ручной приёмки.

В `tests/src/com/unciv/testing/pure/domain/mapgeneration/`: PerlinNoiseTest.kt, ClimateGeneratorTest.kt, ElevationGeneratorTest.kt, VegetationGeneratorTest.kt.

В `tests/src/com/unciv/testing/pure/application/mapgeneration/`: TerrainGenerationPipelineTest.kt, TerrainGridFactoryTest.kt, TerrainMetricsTest.kt.

Suite: `tests/src/com/unciv/testing/pure/TerrainGenerationTestSuite.kt`.

Предварительные fixtures `tests/resources/terrain-generation/characterization.json` и `legacy-parameters.json` не создавались: данные находятся в тестах.

## UI-проверки

- desktop/src/com/unciv/app/desktop/TerrainPlaygroundGoldenChecks.kt
- desktop/src/com/unciv/app/desktop/TerrainParametersGoldenChecks.kt
- desktop/src/com/unciv/app/desktop/UiGoldenTestRunner.kt
- tests/src/com/unciv/testing/UiGoldenTest.kt
- tests/src/com/unciv/testing/UiGoldenUpdateTest.kt

Сценарии: terrain-playground и terrain-parameters. Actual находятся в `tests/golden/results/<платформа>/`; baseline этой фичи не утверждались.

## Документация и исключения

Комплект: Research/RESEARCH.md, CODE_GUIDE.md, Plan/PLAN.md, STEPS.md, files.md и STEP_01…STEP_07 в docs/features/HomogenousTerrain.

Отчёты серии находятся в tests/build/terrain-acceptance и не входят в исходники. Не включать в коммит фичи lasterror.txt, посторонние настройки, ассеты, документацию других тем и существовавшие изменения пользователя. Публикация расширения не входит в текущую задачу.

## Расширение: размеры и широты

Созданы:

- core/src/com/unciv/pure/domain/mapgeneration/LatitudeRange.kt — проверка градусов и проекция по вертикали.
- tests/src/com/unciv/testing/pure/domain/mapgeneration/LatitudeRangeTest.kt — границы, постоянная широта и одна строка.
- docs/features/HomogenousTerrain/Plan/STEP_07_LatitudeRange.md — контракт, результаты и ручной сценарий.

Изменены:

- core/src/com/unciv/pure/application/mapgeneration/TerrainGenerationPipeline.kt — необязательный диапазон, версия результата, проекция и валидация.
- desktop/src/com/unciv/app/desktop/terrain/TerrainExperimentFiles.kt — хранение диапазона и совместимость прежних байтов JSON.
- tests/src/com/unciv/testing/simulations/terrain/TerrainExperimentCli.kt — парные границы, явный legacy и столбцы CSV.
- core/src/com/unciv/ui/mapeditor/TerrainPlaygroundScreen.kt — размеры в начале панели, динамические поля формы и широт, публикация новой исходной маски после успешного вычисления.
- desktop/src/com/unciv/app/desktop/TerrainPlaygroundGoldenChecks.kt — реальные действия с формами, размерами, диапазоном и импортом.
- tests/src/com/unciv/testing/pure/application/mapgeneration/TerrainGenerationPipelineTest.kt — влияние широт, повторяемость, одна строка и несовместимость Flat Earth.
- tests/src/com/unciv/testing/pure/TerrainGenerationTestSuite.kt — включение LatitudeRangeTest.
- tests/src/com/unciv/testing/mapgeneration/TerrainExperimentFilesTest.kt — диапазон, версии и фиксированная legacy JSON-фикстура внутри теста.
- tests/src/com/unciv/testing/mapgeneration/TerrainExperimentCliTest.kt — диапазон, повтор, ошибки без каталога и новая сетка 7×1.
- docs/features/HomogenousTerrain/Research/RESEARCH.md, CODE_GUIDE.md, Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Plan/STEP_05_Playground.md, Plan/STEP_06_Acceptance.md — актуальное состояние и ссылки на расширение.

В расширении нет переносов или удалений файлов. Обычная игровая широтная модель не изменена.

## Этап 08 — фактический перечень

Созданы:

- core/src/com/unciv/logic/map/mapgenerator/MapLatitudeAdapter.kt — общий адаптер проекции для климата, льда и чудес.
- core/src/com/unciv/ui/newgamescreen/MapLatitudeControls.kt — блок широт редактора.
- tests/src/com/unciv/testing/mapgeneration/MapLatitudeParametersTest.kt — JSON, clone/reset, ошибки и Partial.
- docs/features/HomogenousTerrain/Plan/STEP_08_EditorIntegration.md — результат и ручная приёмка.

Изменены:

- core/src/com/unciv/logic/map/MapParameters.kt — настройки диапазона и проверка.
- core/src/com/unciv/logic/map/TileInfo.kt — собственный сериализуемый список особенностей.
- core/src/com/unciv/logic/map/mapgenerator/TerrainGenerationAdapter.kt — климатическая проекция.
- core/src/com/unciv/logic/map/mapgenerator/MapGenerator.kt — ранняя проверка и широты льда.
- core/src/com/unciv/logic/map/mapgenerator/NaturalWonderGenerator.kt — мировые широтные ограничения.
- core/src/com/unciv/ui/newgamescreen/MapParametersTable.kt — подключение блока, Reset и смена формы.
- core/src/com/unciv/ui/mapeditor/MapEditorGenerateTab.kt — ранняя проверка, Partial и восстановление формы.
- core/src/com/unciv/ui/mapeditor/MapGeneratorSteps.kt — передача нужных настроек климату, льду и чудесам.
- core/src/com/unciv/ui/mapeditor/MapEditorScreen.kt — независимый черновик открытой карты.
- tests/src/com/unciv/testing/mapgeneration/TerrainFullGenerationTest.kt — диапазон, лёд, отказ, сохранение, проекция и особенности.
- desktop/src/com/unciv/app/desktop/TerrainParametersGoldenChecks.kt — поля и настоящий редактор.
- docs/features/HomogenousTerrain/Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md, CODE_GUIDE.md — актуальная документация.

MapLatitudeIntegrationTest.kt не создавался: проверки размещены в существующем TerrainFullGenerationTest. TerrainParametersCompatibilityTest запускался без изменений. Переносов и удалений нет. MapSaver и MapFormatMigration не изменялись.

## Этап 09 — фактический перечень

Созданы:

- core/src/com/unciv/ui/mapeditor/MapEditorTerrainLayer.kt — режимы и палитра текущей карты.
- core/src/com/unciv/ui/mapeditor/MapEditorLayerOverlay.kt — диагностическая заливка без ввода и мутации.
- core/src/com/unciv/ui/mapeditor/MapEditorLayerControls.kt — общий переключатель и компактная легенда.
- core/src/com/unciv/ui/utils/SolidHexagonDrawable.kt — общая геометрия сплошной заливки без выделения текстуры.
- desktop/src/com/unciv/app/desktop/TerrainEditorLayersGoldenChecks.kt — настоящий экран редактора, слои, кисть, wrap, загрузка и layout.
- docs/features/HomogenousTerrain/Plan/STEP_09_EditorLayers.md — результат и ручной сценарий.

Изменены:

- core/src/com/unciv/ui/newgamescreen/MapParametersTable.kt — основные масштабы редактора, подписи и общий Reset без дубликатов.
- core/src/com/unciv/ui/mapeditor/EditorMapHolder.kt — заливки оригинальных и зеркальных клеток редактора.
- core/src/com/unciv/ui/mapeditor/MapEditorScreen.kt — состояние слоя и его восстановление при recreate.
- core/src/com/unciv/ui/mapeditor/MapEditorMainTabs.kt — доступная высота вкладок под общей панелью.
- core/src/com/unciv/ui/mapeditor/MapEditorToolsDrawer.kt — панель слоёв, ограничение layout и текущий holder при перетаскивании.
- core/src/com/unciv/ui/battlescreen/BattleSupportFillDrawable.kt — совместимая обёртка общей заливки.
- desktop/src/com/unciv/app/desktop/TerrainParametersGoldenChecks.kt — масштабы вне Advanced, числовые подписи и Reset.
- desktop/src/com/unciv/app/desktop/UiGoldenTestRunner.kt — сценарий настоящего редактора и ожидание проверок.
- tests/src/com/unciv/testing/UiGoldenTest.kt — terrainEditorLayersRender и battleHexFillRender.
- docs/features/HomogenousTerrain/CODE_GUIDE.md, Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md — актуальные инструкции и результаты.

Файлы не переносились и не удалялись. Domain/application, генераторы и формат карты в этапе 09 не менялись. Capture-файлы в tests/golden/results не являются утверждёнными baseline и не входят в исходники фичи.

## Этап 10 — фактический перечень

Создан docs/features/HomogenousTerrain/Plan/STEP_10_GenerateLayerFix.md.

Изменены:

- core/src/com/unciv/ui/mapeditor/MapEditorLayerOverlay.kt — Image с размерами drawable до расчёта геометрии.
- core/src/com/unciv/ui/mapeditor/MapEditorLayerControls.kt — естественная высота легенды без наложения.
- core/src/com/unciv/ui/mapeditor/MapEditorMainTabs.kt — GenerationPage с fixed content только для Generate.
- core/src/com/unciv/ui/mapeditor/MapEditorToolsDrawer.kt — удаление глобальной панели и возврат высоты вкладкам.
- core/src/com/unciv/ui/mapeditor/MapEditorScreen.kt — признак активности диагностического просмотра.
- core/src/com/unciv/ui/mapeditor/EditorMapHolder.kt — заливка только при активном Generate.
- desktop/src/com/unciv/app/desktop/TerrainEditorLayersGoldenChecks.kt — конечные размеры, реальный framebuffer, обратимость, область видимости панели, легенда, wrap и загрузка.
- docs/features/HomogenousTerrain/Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md, CODE_GUIDE.md — результаты исправления и актуальная инструкция.

MapEditorGenerateTab.kt менять не потребовалось: панель подключена через оболочку страницы. Файлы не переносились и не удалялись; формулы и формат карт не менялись.

## Этап 11 — фактический перечень

Созданы:

- desktop/src/com/unciv/app/desktop/TerrainEditorGenerationGoldenChecks.kt — проверка нижней границы формы, закреплённой кнопки и двух реальных запусков с разными seed.
- docs/features/HomogenousTerrain/Plan/STEP_11_PinnedGeneration.md — контракт, реализация, проверки и ручной сценарий.

Изменены:

- core/src/com/unciv/ui/mapeditor/MapEditorMainTabs.kt — локальное отключение внешней прокрутки Generate и ограничение минимального размера вложенного pager.
- core/src/com/unciv/ui/mapeditor/MapEditorLayerControls.kt — кнопка под селектором слоя.
- core/src/com/unciv/ui/mapeditor/MapEditorGenerateTab.kt — общая закреплённая кнопка, новый seed после валидации, блокировка повторного запуска, сохранение раздела Generate; удалён нижний дубль полной генерации.
- core/src/com/unciv/ui/mapeditor/MapEditorScreen.kt — передаваемый адаптер сохранения настроек, сохраняющийся при recreate.
- core/src/com/unciv/ui/newgamescreen/MapParametersTable.kt — явный seed в reseed, имена Advanced и Reset для проверки геометрии.
- desktop/src/com/unciv/app/desktop/TerrainEditorLayersGoldenChecks.kt — подключение сценария генерации и отключение записи пользовательских настроек в тесте.
- desktop/src/com/unciv/app/desktop/TerrainParametersGoldenChecks.kt — актуальный reflection-вызов и проверка отсутствия фонового запуска при ошибочных широтах.
- docs/features/HomogenousTerrain/Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md, CODE_GUIDE.md — итоговое поведение и результаты.

Файлы не переносились и не удалялись. Общий TabbedPager, чистое ядро и формат карт не менялись.

## Этап 12 — фактический перечень

Созданы:

- desktop/src/com/unciv/app/desktop/TerrainEditorDefaultsGoldenChecks.kt — подтверждение, отмена, полный сброс, сохранение контекста и in-memory проверка записи настроек.
- docs/features/HomogenousTerrain/Plan/STEP_12_WaterAndDefaults.md — контракт и результаты.

Изменены:

- core/src/com/unciv/ui/mapeditor/MapEditorGenerateTab.kt — resetDefaultsButton, ConfirmPopup, полный сброс через общий метод восстановления, блокировка во время генерации и сохранение настроек.
- core/src/com/unciv/ui/mapeditor/MapEditorLayerControls.kt — размещение полного Reset под Generate.
- core/src/com/unciv/ui/mapeditor/MapEditorToolsDrawer.kt — исключение событий Popup из перехвата ручкой drawer.
- desktop/src/com/unciv/app/desktop/TerrainEditorGenerationGoldenChecks.kt — подключение проверки сброса, диагностика границ панели и блокировка Reset во время генерации.
- desktop/src/com/unciv/app/desktop/TerrainEditorLayersGoldenChecks.kt — in-memory адаптер записи параметров.
- tests/src/com/unciv/testing/mapgeneration/TerrainFullGenerationTest.kt — вода после полного запуска нескольких типов и влияние Water level при одном seed.
- docs/features/HomogenousTerrain/Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md, CODE_GUIDE.md — итоговые контракты и проверки.

Переносов и удалений нет. Чистое ядро, MapParameters, формулы генерации воды и формат карт не менялись.

## Этап 13 — фактический перечень

Созданы:

- core/src/com/unciv/pure/domain/mapgeneration/MountainZoneGenerator.kt — числовая геометрия, Cross/Network, горные стены, проверенные узкие проходы и инварианты графа.
- core/src/com/unciv/logic/map/mapgenerator/MountainZoneGenerationAdapter.kt — снимок геометрии TileMap, проверка террейнов Ruleset и применение структуры.
- tests/src/com/unciv/testing/pure/domain/mapgeneration/MountainZoneGeneratorTest.kt — 6 независимых тестов чистого ядра.
- tests/src/com/unciv/testing/mapgeneration/MountainZoneGenerationTest.kt — 4 интеграционные проверки полного пути, Partial, сериализации и ранних отказов.
- desktop/src/com/unciv/app/desktop/TerrainMountainZonesGoldenChecks.kt — реальные селекторы, число зон, генерация обоих типов и пиксели карты.
- desktop/src/com/unciv/app/desktop/TerrainMountainPartialGoldenChecks.kt — два реальных Partial с неизменными seed структуры и координатами стен.
- docs/features/HomogenousTerrain/Plan/STEP_13_MountainZones.md — контракт, ограничения и результаты.

Изменены:

- core/src/com/unciv/logic/map/MapParameters.kt — два типа, mountainZoneCount, clone и ранняя проверка запроса.
- core/src/com/unciv/logic/map/mapgenerator/MapLandmassGenerator.kt — новый структурный путь без изменения старого RNG.
- core/src/com/unciv/logic/map/mapgenerator/MapGenerator.kt — применение структуры до размещения содержимого и после полного/частичного запуска.
- core/src/com/unciv/ui/newgamescreen/MapParametersTable.kt — два шаблона и контекстный селектор числа зон.
- core/src/com/unciv/ui/mapeditor/MapEditorGenerateTab.kt — валидация, сообщения ошибок, восстановление числа зон и сохранение seed структуры в Partial.
- core/src/com/unciv/ui/mapeditor/MapGeneratorSteps.kt — копирование числа зон для Landmass.
- desktop/src/com/unciv/app/desktop/TerrainEditorGenerationGoldenChecks.kt — подключение UI-сценариев шаблонов и Partial.
- desktop/src/com/unciv/app/desktop/TerrainEditorDefaultsGoldenChecks.kt — новое поле в проверке полного Reset.
- docs/features/HomogenousTerrain/Plan/PLAN.md, Plan/STEPS.md, Plan/files.md, Research/RESEARCH.md, CODE_GUIDE.md — итоговые контракты и проверки.

Переносов и удалений нет. TileMap, старые шаблоны и их world wrap не менялись. Новое необязательное поле параметров не требует миграции старых карт.