# HomogenousTerrain: код и запуск

Реализация и автоматические проверки завершены. Визуальные baseline и ручная приёмка не выполнены; исторические эталоны охватывают три выделенных слоя, а не весь generateMap.

[Исследование](Research/RESEARCH.md) · [План](Plan/PLAN.md) · [Этапы](Plan/STEPS.md) · [Файлы](Plan/files.md) · [Приёмка](Plan/STEP_06_Acceptance.md)

## Путь вычислений

Игра и редактор → MapGenerator → TerrainGenerationAdapter → общие доменные генераторы → TileMap.

CLI и песочница → TerrainGenerationPipeline → те же генераторы → TerrainExperimentResult. ALL означает рельеф → климат → растительность. Берега, лёд, реки, ресурсы и остальные игровые этапы в экспериментальный ALL не входят.

Domain находится в `core/src/com/unciv/pure/domain/mapgeneration/`: PerlinNoise, ClimateGenerator, ElevationGenerator, VegetationGenerator. Application в соседнем `pure/application/mapgeneration/`: контракты и координатор TerrainGenerationPipeline, геометрия TerrainGridFactory, метрики TerrainMetrics. Здесь нет Gdx, файлов и игровых объектов.

Игровые адаптеры находятся в `core/src/com/unciv/logic/map/mapgenerator/`: TerrainGenerationAdapter применяет этапы, TerrainRuleSnapshotAdapter снимает правила, TerrainPreviewMapAdapter создаёт отдельную карту для игровых спрайтов. `logic/map/Perlin.kt` сохраняет прежний API делегированием.

## Снимки и файлы

JVM-адаптеры находятся в `desktop/src/com/unciv/app/desktop/terrain/`:

- TerrainExperimentSource загружает Terrains.json в локальные правила без RulesetCache. Дубликаты имён отклоняются; моды не объединяются неявно.
- TerrainExperimentFiles сохраняет входы и результаты с версией и SHA-256. Чтение результата проверяет повторное вычисление.
- TerrainExperimentExport создаёт result.json, metrics.csv, legend.csv и пять PNG без OpenGL. Существующий каталог не перезаписывается.

Снимок хранит геометрию, исходную маску, фактический порядок правил и настройки. Повторное чтение одного снимка устраняет неопределённость legacy HashSet между процессами. Отдельный этап сравним с игровым только при одинаковых исходных клетках и RNG-контексте.

## Запуск

Из корня проекта получить справку CLI:

```text
./gradlew tests:runTerrainExperimentCli --args="--help"
```

В PowerShell для готового снимка без пробелов в пути:

```powershell
.\gradlew.bat tests:runTerrainExperimentCli --args='--input=experiment.json --output=terrain-series --seeds=0,42 --climate-scales=6,12'
```

Новый эксперимент из базовых правил с внутренними кавычками вокруг пути:

```powershell
.\gradlew.bat tests:runTerrainExperimentCli --args='--terrains="android/assets/jsons/Civ V - Gods & Kings/Terrains.json" --shape=hexagonal --radius=8 --output=terrain-series --seeds=0,42'
```

Графический launcher:

```text
./gradlew desktop:runTerrain
```

Он работает из android/assets; относительные пути --input и --terrains считаются оттуда. Для отчётов серии удобнее абсолютный путь в поле экрана. Задачи можно запускать из Gradle-конфигураций IDE. В MaxVibes сборка и тесты выполняются через BUILD/TESTS.

## Настройки и UI

Defaults: tilesPerBiomeArea = 6, vegetationScale = 3.0, elevationScale = 2.0. Старые JSON получают исторические значения. Общая форма предлагает новые масштабы 0.5–30 с шагом 0.5; её Reset сохраняет исторический reseed. Partial фиксирует настройки выбранного этапа до фонового запуска.

TerrainPlaygroundScreen принимает файловые callbacks от DesktopLauncherTerrain. Параметры и исходная маска фиксируются до рабочего потока, текстура публикуется на GL-потоке. Generate / repeat не накапливает слои; New seed меняет seed явно. Open snapshot / report проверяет отчёт. Export displayed result сохраняет показанный результат, независимо от черновика формы.

Show game terrain использует EditorMapHolder без партии и обработчиков редактирования. Требуются стандартные незамкнутые гексагональные координаты в пределах −500…500 и совместимые определения террейнов загруженного графического ruleset. Show selected layer возвращает PNG. Пользовательские графы и другие правила можно исследовать через числовые слои и экспорт.

## Проверки

Native TESTS:

- `com.unciv.testing.pure.TerrainGenerationTestSuite`: 33 чистых теста.
- `com.unciv.testing.mapgeneration.TerrainGenerationCharacterizationTest`: 8 тестов и 36 исторических отпечатков слоёв/RNG.
- `com.unciv.testing.mapgeneration.TerrainParametersCompatibilityTest`: 5 тестов.
- В том же пакете TerrainExperimentFilesTest (4), TerrainExperimentExportTest (2), TerrainExperimentSourceTest (3), TerrainExperimentCliTest (4), TerrainFullGenerationTest (2).
- `com.unciv.testing.mapgeneration.TerrainAcceptanceSeriesTest`: 1 тест, сохраняющий восемь проверенных экспериментов в tests/build/terrain-acceptance; latest.txt указывает каталог.
- `com.unciv.testing.UiGoldenTest#terrainPlaygroundRender` и `#terrainParametersRender`: прошедшие capture-проверки. Методы без суффикса Render сравнивают с baseline; UiGoldenUpdateTest обновляет их только намеренно после просмотра.

Полный генератор проверен на повторяемость и старые параметры, включая Flat Earth и wrap. Исторический эталон всего конвейера не получен; руины и размещение стартовых регионов не покрыты этим интеграционным тестом. Точные данные серии и оставшиеся действия автора приведены в документе приёмки.
