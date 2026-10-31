# HomogenousTerrain: код и запуск

Реализация и автоматические проверки завершены. Пользователь подтвердил первоначальный просмотр игровых текстур. Новые настройки размеров и широт ещё требуют ручного просмотра; визуальные baseline не утверждены.

[Исследование](Research/RESEARCH.md) · [План](Plan/PLAN.md) · [Этапы](Plan/STEPS.md) · [Файлы](Plan/files.md) · [Размеры и широты](Plan/STEP_07_LatitudeRange.md)

## Путь вычислений

Игра и редактор → MapGenerator → TerrainGenerationAdapter → общие доменные генераторы → TileMap.

CLI и песочница → TerrainGenerationPipeline → те же генераторы → TerrainExperimentResult. ALL означает рельеф → климат → растительность. Берега, лёд, реки и игровые объекты в него не входят.

В `core/src/com/unciv/pure/domain/mapgeneration/` находятся PerlinNoise, ClimateGenerator, ElevationGenerator, VegetationGenerator и LatitudeRange. Последний проверяет границы широт и интерполирует их по вертикальной координате. В `pure/application/mapgeneration/` находятся снимки, координатор, TerrainGridFactory и TerrainMetrics. Чистый код не зависит от движка и файлов.

В `core/src/com/unciv/logic/map/mapgenerator/` TerrainGenerationAdapter применяет этапы, TerrainRuleSnapshotAdapter снимает правила, TerrainPreviewMapAdapter создаёт отдельную карту для игровых спрайтов. `logic/map/Perlin.kt` сохраняет прежний API. Диапазон широт также используется генерацией редактора; выключенный диапазон сохраняет прежний климат.

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

## Редактор уровней

Открыть обычный редактор → Generate → New map. Размеры задаются существующими Map Shape и World Size → Custom. Блок Use latitude range находится рядом с размерами. Пример: Rectangular 40×30, Top latitude 60, Bottom latitude 20, затем Create. Диапазон 20…−20 охватывает экватор. Flat Earth требует выключенного диапазона.

Настройки сохраняются с картой и восстанавливаются при открытии. Форма редактирует отдельный черновик. Для текущей карты изменить границы в New map, затем выбрать Partial → Humidity and temperature → Generate. Размеры текущей карты не меняются. Лёд и растительность запускаются отдельными шагами; прежние особенности не очищаются автоматически. Редактор сохраняет историческое автоматическое изменение seed при повторении действий, в отличие от песочницы.

MapParameters хранит useLatitudeRange/topLatitude/bottomLatitude и проверяет их через LatitudeRange. MapLatitudeAdapter преобразует Y для климата, льда и процентных ограничений чудес. Геометрические широты TileInfo не меняются. MapLatitudeControls обслуживает форму; MapEditorGenerateTab проверяет итоговые параметры до запуска потока и передаёт их нужным шагам. MapEditorScreen восстанавливает черновик при открытии карты.

Сохранение использует прежний формат 3 с необязательными полями настроек. TileInfo.setTerrainFeatures копирует вход в ArrayList для корректного JSON round-trip; температура и влажность остаются transient. Старые карты не перегенерируются при чтении.

Проверены MapLatitudeParametersTest (5), TerrainFullGenerationTest (7), TerrainGenerationCharacterizationTest (8), TerrainParametersCompatibilityTest (5), чистая TerrainGenerationTestSuite (39) и расширенный UiGoldenTest#terrainParametersRender. UI-сценарий включает настоящий редактор, загрузку другой карты и отказ неверного Partial. Это capture с проверками поведения, а не сравнение с baseline.

Подробный [ручной сценарий редактора](Plan/STEP_08_EditorIntegration.md) ещё предстоит выполнить пользователю.
## Основные масштабы и просмотр слоёв редактора

В Generate → New map масштабы Climate scale, Vegetation scale и Elevation scale находятся в основном блоке рядом с размерами. Значения постоянно видны; Advanced больше не содержит их дубликатов. Общий Reset возвращает 6/3/2. Климатический ползунок редактора допускает целые 1–30, остальные — 0.5–30 с шагом 0.5. Обычная форма новой игры сохраняет прежние настройки.

View layer внутри Generate переключает Game terrain, Base terrain, Temperature, Humidity, Elevation и Vegetation. Он доступен только в Generate, включая New map и Partial. Выход в View/Edit возвращает игровой вид; возврат в Generate восстанавливает выбранный слой. Выбор не запускает генерацию и не меняет карту, isDirty, seed или положение камеры. Рисование остаётся доступным. Рельеф показывает категории, растительность — текущие особенности. Климатические слои показывают последние вычисленные значения; после загрузки отсутствующие transient-поля серые. Цвета и их смысл указаны в легенде.

MapEditorTerrainLayer содержит UI-палитру; MapEditorLayerOverlay читает текущую клетку при рисовании. EditorMapHolder добавляет заливку под выделение оригинальных и зеркальных гексов. MapEditorLayerControls предоставляется GenerationPage как fixed content страницы Generate; выбранный режим хранится в MapEditorScreen и восстанавливается при загрузке/recreate, но не сохраняется в карте. SolidHexagonDrawable используется также прежней боевой обёрткой BattleSupportFillDrawable.

Проверки: UiGoldenTest#terrainParametersRender (масштабы и Reset), #terrainEditorLayersRender (слои, кисть, ввод, layout, wrap и загрузка), #terrainPlaygroundRender (песочница), #battleHexFillRender (отрисовка общих боевых заливок), TerrainParametersCompatibilityTest (5 тестов). BUILD прошёл. Capture не является сравнением с baseline; новый интерфейс ещё требует ручного просмотра. Подробный сценарий: [этап 09](Plan/STEP_09_EditorLayers.md).
## Исправление отображения слоёв

Панель больше не глобальная: открыть Generate и выбрать View layer под заголовком этого раздела. Она общая для New map и Partial. При выходе из Generate диагностическая заливка скрывается; выбранный режим сохраняется для возвращения. Легенда переносится с естественной высотой текста.

MapEditorLayerOverlay создаётся через Image(drawable), задающий исходные размеры до sizeAndPlaceOverHex. Прежний пустой Image мог приводить к NaN в расчёте высоты. Конечные положительные размеры теперь проверяются явно. MapEditorScreen.terrainLayersVisible управляется активацией GenerationPage, а EditorMapHolder учитывает его при отображении.

terrainEditorLayersRender сравнивает реальные пиксели только области карты между игровым и диагностическими режимами и проверяет обратимость Game terrain. Это устраняет ложный успех прежней проверки только currentColor. BUILD и UI-сценарии слоёв, параметров и песочницы прошли. Визуальный baseline не утверждался. [Этап 10](Plan/STEP_10_GenerateLayerFix.md) содержит детали и ручной сценарий.
## Закреплённая генерация и прокрутка Generate

Под View layer находится одна кнопка Generate, общая для New map и Partial. Она полностью создаёт карту с текущими настройками и новым положительным случайным seed, отличным от предыдущего. Нижний дубль полной генерации удалён; отдельное применение выбранного шага Partial сохранено. Во время работы кнопка заблокирована, повторный запуск дополнительно отклоняет обработчик. После завершения редактор остаётся в Generate.

GenerationPage локально отключает внешнюю прокрутку страницы и восстанавливает её при выходе. Container с нулевым минимальным размером передаёт вложенному MapEditorGenerateTab оставшуюся высоту; внутренний pager прокручивает форму до последнего элемента. Общий TabbedPager не изменялся.

MapParametersTable.reseed принимает необязательный явный seed и синхронизирует поле. MapEditorScreen.rememberGenerationParameters делегирует сохранение переданному адаптеру persistGenerationDefaults: по умолчанию прежнее сохранение настроек, в UI-сценарии пустой обработчик без записи пользовательских предпочтений. При recreate адаптер сохраняется.

TerrainEditorGenerationGoldenChecks включён в UiGoldenTest.terrainEditorLayersRender. Проверяются полностью видимый Reset, цель клика, неподвижность кнопки, два асинхронных запуска через Stage с разными seed, сохранение настроек, блокировка повторного ввода и раздел Generate после загрузки результата. Координаты ScrollPane читаются после draw; кнопки проверяются через принятый Unciv isEnabled. BUILD и три UI-сценария прошли; baseline не обновлялся. [Этап 11](Plan/STEP_11_PinnedGeneration.md) содержит результаты и короткую ручную приёмку.
## Вода и полный сброс настроек

В игровом редакторе вода создаётся MapLandmassGenerator до климата и рельефа. Тип карты управляет геометрией суши; Water level в Advanced задаёт порог: выше значение — больше воды при одинаковом seed для шумовых типов. Archipelago и Inner Sea предназначены для других водных конфигураций, чем Pangaea. Климатический, растительный и рельефный масштабы не являются настройкой доли воды. Правила без водных террейнов намеренно работают в land-only режиме. Полный MapGenerator также обрабатывает озёра, берега и реки.

В закреплённой панели под Generate есть полный Reset to defaults с подтверждением. ConfirmPopup Cancel сохраняет значения; подтверждение восстанавливает стандартные параметры формы (Pangaea, Hexagonal, Medium, обычные ресурсы/флаги/масштабы/климат/уровень воды, выключенный диапазон широт), сохраняет текущие mods и baseRuleset и записывает настройки через rememberGenerationParameters. Открытая карта и dirty-флаг не меняются; Generate запускается отдельно. Кнопка отключена во время генерации. Нижний Advanced Reset по-прежнему сбрасывает только расширенные параметры.

MapEditorGenerateTab.restoreGenerationParameters принимает источник MapParameters, по умолчанию параметры открытой карты; полный сброс передаёт новый MapParameters. Перестраивается форма при сохранении объекта черновика. MapEditorToolsDrawer не перехватывает события из Popup, даже если кнопка диалога находится на X ручки.

Проверки: TerrainFullGenerationTest (8 тестов, вода нескольких типов и влияние Water level), UiGoldenTest.terrainEditorLayersRender (диалог, отмена/подтверждение, in-memory запись, неизменная карта, блокировка кнопки и геометрия) и UiGoldenTest.terrainParametersRender. BUILD и все указанные проверки прошли. Новых чистых правил нет; baseline не обновлялся. [Этап 12](Plan/STEP_12_WaterAndDefaults.md) содержит ручной сценарий; его пользовательский просмотр ещё не подтверждён.
## Mountain Cross и Mountain Zones

Выбрать Map Generation Type → Mountain Cross или Mountain Zones. Появится Zones (3..12, стандарт 5). Центр Cross входит в общее число: 5 означает центр и четыре периферийные области. Cross допускает переходы периферии только через центр. Zones строит дерево связей соседних областей. Обе карты сухопутные: Water level не управляет их структурой. Каждый проход имеет клетку, перекрывающую соединение; защитников устанавливает автор карты вручную.

Поддерживаются Hexagonal и Rectangular без World Wrap. Flat Earth и World Wrap явно отклоняются. Слишком маленькая карта требует уменьшения Zones или увеличения размера. Старые типы карт и их wrap остаются прежними. Размещение защитников, баланс содержимого зон и распределение стартов игроков в задачу не входят.

Чистые правила: core/src/com/unciv/pure/domain/mapgeneration/MountainZoneGenerator.kt. Вход — числовые координаты/соседство, count, mode, seed; выход — zoneByCell (-1 = стена), centers и passages с guard. Cross использует центральную область/сектора, Network — разбиение по центрам. Сохраняются связные комнаты, строится дерево одноклеточных соединений, проверяются физические разрезы. Вход не изменяется, граф и seed определяют результат; domain не зависит от application или движка.

Игровая граница: MountainZoneGenerationAdapter снимает TileMap/Ruleset, сортирует клетки по координатам и применяет готовую структуру. MapLandmassGenerator включает новый путь; MapGenerator применяет структуру до размещения содержимого и после полного запуска/Partial. Случайные горы/вода и блокирующие чудеса не могут разрушить стены и проходимость. Горные террейны должны быть непроходимыми, в ruleset должна существовать проходимая суша.

MapParameters.mountainZoneCount по умолчанию 5; clone, сохранение, восстановление формы, полный Reset и Landmass учитывают его. Старые JSON без поля совместимы. Partial горных карт использует seed структуры открытой карты и не передвигает границы из-за черновика или повторного reseed. Полная Generate создаёт новое разбиение с новым seed. Ручные изменения террейна автором могут менять топологию; применение генераторных Partial восстанавливает структурные ограничения шаблона.

Проверки из IDE: MountainZoneGeneratorTest (6), MountainZoneGenerationTest (4), TerrainFullGenerationTest (8), UiGoldenTest.terrainEditorLayersRender и UiGoldenTest.terrainParametersRender. Все прошли вместе с BUILD. UI-сценарий проверяет настоящие селекторы, генерацию обоих типов, пиксели карты, Reset и два последовательных Partial с неизменными стенами. Capture-only не сравнивает baseline. [Этап 13](Plan/STEP_13_MountainZones.md) содержит детали и короткую ручную приёмку.