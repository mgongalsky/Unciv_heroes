# Этап 04. Автоматические эксперименты

Статус: DONE.

[План](PLAN.md) · [Этапы](STEPS.md) · [Файлы](files.md) · [Команды запуска](../CODE_GUIDE.md)

## Реализовано

- TerrainGenerationPipeline: отделённые снимки, явный seed (включая 0), режимы ALL/ELEVATION/CLIMATE/VEGETATION. ALL выполняет только три выделенных слоя.
- TerrainGridFactory: прямоугольник и гексагон без wrap. Готовый снимок может содержать замкнутый граф; вырожденная климатическая геометрия отклоняется.
- TerrainMetrics: доли категорий, площади компонент, малые области, одиночные клетки и границы. Вода исключена из знаменателей трёх категориальных слоёв.
- TerrainRuleSnapshotAdapter и TerrainExperimentSource: локальная загрузка Terrains.json без RulesetCache и игрового приложения. Моды не объединяются неявно. Фактический порядок кандидатов растительности сохраняется в снимке.
- TerrainExperimentFiles: строгий JSON, версии, SHA-256 входа, round-trip включая −0.0, проверка результата повторным вычислением.
- TerrainExperimentExport: result.json, metrics.csv, legend.csv и пять PNG через BufferedImage без OpenGL. Существующий каталог не перезаписывается.
- TerrainExperimentCli: декартово произведение seed и трёх масштабов, до 1000 запусков. Все настройки проверяются до записи. Каждый запуск использует исходные клетки. input.json и index.csv связывают серию с отчётами.

## Запуск

Задача `tests:runTerrainExperimentCli` запускается из корня проекта. Синтаксис: `--input=<снимок или отчёт>` либо `--terrains=<Terrains.json>`, обязательный `--output=<новый каталог>`, списки `--seeds`, `--climate-scales`, `--vegetation-scales`, `--elevation-scales`, режим `--mode`. Для нового снимка доступны `--shape=hexagonal --radius=8` либо `--shape=rectangular --width=30 --height=20`. Размеры готового снимка переопределять нельзя.

Примеры с кавычками для PowerShell приведены в CODE_GUIDE. `--help` выводит полный синтаксис. MaxVibes выполняет автоматические проверки через TESTS.

## Подтверждённые проверки

Чистая suite: 33 теста; характеристические: 8; TerrainExperimentFilesTest: 4; TerrainExperimentExportTest: 2; TerrainExperimentSourceTest: 3; TerrainExperimentCliTest: 4. PNG читаются, повторный экспорт побайтово совпадает. Базовый Terrains.json читается без приложения.

TerrainAcceptanceSeriesTest дополнительно сохранил восемь проверенных запусков на одних правилах; параметры и путь приведены в этапе 06. Это не выбор новых defaults. Для сравнения разных процессов следует использовать сохранённый снимок, фиксирующий порядок legacy HashSet.
