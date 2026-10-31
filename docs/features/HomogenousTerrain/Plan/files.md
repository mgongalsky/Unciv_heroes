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

Комплект: Research/RESEARCH.md, CODE_GUIDE.md, Plan/PLAN.md, STEPS.md, files.md и STEP_01…STEP_06 в docs/features/HomogenousTerrain.

Отчёты серии находятся в tests/build/terrain-acceptance и не входят в исходники. Не включать в коммит фичи lasterror.txt, посторонние настройки, ассеты, документацию других тем и существовавшие изменения пользователя. Коммит и публикация не выполнялись.
