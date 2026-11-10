# 01. Изолировать запуск

Статус: DONE.

Добавлены CityScreenContext, CitySandbox и DesktopLauncherCity. Контекст заменяет прямое требование WorldScreen в CityScreen, ConstructionInfoTable и SpecialistAllocationTable. createCityScreen сохраняет его при пересоздании, переименовании, покупке клетки и переходах.

Фабрика через initialScreenFactory создаёт карту TileMap радиуса 6, Rome, один город с очередью Granary и посещающего Warrior с отрядом Spearman. Характеристики читаются из действующего ruleset, сценарий остаётся в памяти. ImageGetter получает тот же ruleset перед созданием экрана.

Граница изменений — UI и адаптер запуска. GameStarter, loadGame, WorldScreen и цикл ходов не вызываются; UncivGame.gameInfo остаётся null. Минимальный GameInfo необходим внутренним менеджерам города. Сохранения и игровые правила не меняются.

Фактический результат: отдельный запуск компилируется; GL-проверка подтвердила отсутствие мирового экрана, наличие героя и сохранение модели при пересоздании. Подробности — [проверки](STEP_02_Verification.md).

[План](PLAN.md)
