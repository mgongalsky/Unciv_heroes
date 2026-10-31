# HomogenousTerrain: код и запуск

Реализация и автоматические проверки завершены. Пользователь подтвердил первоначальный просмотр игровых текстур. Новые настройки размеров и широт ещё требуют ручного просмотра; визуальные baseline не утверждены.

[Исследование](Research/RESEARCH.md) · [План](Plan/PLAN.md) · [Этапы](Plan/STEPS.md) · [Файлы](Plan/files.md) · [Размеры и широты](Plan/STEP_07_LatitudeRange.md)

## Путь вычислений

Игра и редактор → MapGenerator → TerrainGenerationAdapter → общие доменные генераторы → TileMap.

CLI и песочница → TerrainGenerationPipeline → те же генераторы → TerrainExperimentResult. ALL означает рельеф → климат → растительность. Берега, лёд, реки и игровые объекты в него не входят.

В `core/src/com/unciv/pure/domain/mapgeneration/` находятся PerlinNoise, ClimateGenerator, ElevationGenerator, VegetationGenerator и LatitudeRange. Последний проверяет границы широт и интерполирует их по вертикальной координате. В `pure/application/mapgeneration/` находятся снимки, координатор, TerrainGridFactory и TerrainMetrics. Чистый код не зависит от движка и файлов.

В `core/src/com/unciv/logic/map/mapgenerator/` TerrainGenerationAdapter применяет этапы, TerrainRuleSnapshotAdapter снимает правила, TerrainPreviewMapAdapter создаёт отдельную карту для игровых спрайтов. `logic/map/Perlin.kt` сохраняет прежний API. Диапазон широт пока используется только экспериментами; обычная игра сохраняет прежний климат.

## Снимки и совместимость

В `desktop/src/com/unciv/app/desktop/terrain/`:

- TerrainExperimentSource загружает Terrains.json без RulesetCache; дубликаты отклоняются, моды не объединяются неявно.
- TerrainExperimentFiles сохраняет входы и отчёты с SHA-256 и проверяет повторное вычисление.
- TerrainExperimentExport создаёт result.json, metrics.csv, legend.csv и пять PNG без OpenGL. Каталог должен быть новым.

Снимок содержит геометрию, маску, порядок правил и настройки. Legacy использует `homogenous-terrain-1`; диапазон — `homogenous-terrain-latitude-1`. У legacy отсутствует поле latitudeRange, поэтому канонические байты и checksum сохраняются. Несогласованные версии отклоняются.

Отдельный этап сравним с игровым только при одинаковом входе и RNG-контексте. Сохранённый порядок правил устраняет неопределённость legacy HashSet между процессами.

## Запуск песочницы

В IDE открыть Run → Edit Configurations, выбрать существующую Gradle-конфигурацию Terrain либо создать Gradle-конфигурацию. Gradle project — корень проекта, задача — `desktop:runTerrain`. Запустить конфигурацию. Launcher работает из android/assets; для открытия отчётов удобнее абсолютный путь.

Размеры находятся в начале левой панели. New grid shape выбирает hexagonal с радиусом или rectangular с шириной и высотой. Равные ширина и высота задают квадратный контур, клетки остаются гексами. Create new land mask применяет размеры; Generate / repeat сохраняет текущую сетку.

Для участка северного полушария выбрать Latitude model = Latitude range и задать верхнюю 60, нижнюю 20. Для экватора — 20 и −20. Допустимы −90…+90, верхняя не меньше нижней. Одна строка использует середину диапазона, одинаковые границы — постоянную широту. Flat Earth с диапазоном несовместим.

Seed фиксирует случайность. Повтор с тем же полным входом воспроизводится; изменение размера не обязано сохранять рисунок общих клеток. New seed меняет его явно. Параметры фиксируются до рабочего потока, результат публикуется на GL-потоке. Ошибка оставляет прежний результат.

Show game terrain показывает игровые текстуры с навигацией без обработчиков редактора. Show selected layer возвращает PNG. После генерации можно снова нажать Show game terrain. Open snapshot / report проверяет и восстанавливает вход, Export displayed result сохраняет показанный результат независимо от черновика формы.

Игровой вид требует стандартного незамкнутого соседства гексов, координат −500…500 и совместимого графического ruleset. Произвольные графы доступны через PNG и метрики. Общие игровые настройки lastGameSetup не меняются.

Подробный [ручной сценарий](Plan/STEP_07_LatitudeRange.md) использует seed 42, прямоугольник 40×30 и диапазоны 60°…20° и 20°…−20°.

## CLI

Из корня проекта в PowerShell:

```powershell
.\gradlew.bat tests:runTerrainExperimentCli --args='--help'
```

Новый прямоугольный эксперимент:

```powershell
.\gradlew.bat tests:runTerrainExperimentCli --args='--terrains="android/assets/jsons/Civ V - Gods & Kings/Terrains.json" --shape=rectangular --width=40 --height=30 --top-latitude=60 --bottom-latitude=20 --seeds=42 --output=terrain-north-42'
```

Повтор отчёта сохраняет его диапазон:

```powershell
.\gradlew.bat tests:runTerrainExperimentCli --args='--input=terrain-north-42/run-0000/result.json --output=terrain-north-repeat'
```

Обе границы задаются вместе. `--latitude-mode=range` требует границ; `--latitude-mode=legacy` отключает диапазон и не допускает границы. Геометрия импортированного снимка не переопределяется: для новой сетки нужен `--terrains`. Неверные параметры отклоняются до создания каталога. CSV-индекс содержит режим и границы широт.

## Проверки

В MaxVibes сборка и тесты запускаются native BUILD/TESTS. После расширения прошли:

- `com.unciv.testing.pure.TerrainGenerationTestSuite`: 39 тестов.
- `com.unciv.testing.mapgeneration.TerrainGenerationCharacterizationTest`: 8 тестов, 36 исторических отпечатков и RNG.
- `com.unciv.testing.mapgeneration.TerrainExperimentFilesTest`: 7 тестов, включая фиксированную legacy JSON-фикстуру.
- `com.unciv.testing.mapgeneration.TerrainExperimentCliTest`: 7 тестов, включая однострочный прямоугольник.
- `com.unciv.testing.UiGoldenTest#terrainPlaygroundRender`: обработчики размеров, широт, повтора, ошибок, импорта и игровых текстур.

Первоначальный этап также проверил параметры, источник правил, экспорт, полный генератор, общую форму и серию из восьми экспериментов; детали в [приёмке](Plan/STEP_06_Acceptance.md). Исторического эталона всего generateMap нет. UI Render — capture, а не сравнение с baseline; эталоны намеренно не обновлялись.
