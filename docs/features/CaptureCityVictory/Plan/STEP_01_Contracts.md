# STEP 01 — Контракты и характеризация

Статус: выполнен. Контракт: [PLAN.md](PLAN.md). Реальные вызовы и итог: [CODE_GUIDE.md](../CODE_GUIDE.md).

Проверены VictoryManager, GameInfo.write/read/clone/nextTurn, NeutralCityEntry, BattleWorldOutcomeHandler, завоевание/освобождение и общая передача. moveToCiv обслуживает также торговлю; проверка захвата туда не добавлена. Слоты разрешает существующий ScenarioPlayerBindings.

Самостоятельно приняты правила All, текущего владения, события назначенной цели, отсутствия стартовой победы, учёта пустого нейтрального города и неизменности первого результата. Решения записаны в PLAN.

VictoryLegacyCharacterizationTest закрепляет порядок, пустые milestones обычных типов и фильтрацию неизвестных/выключенных типов. Существующие CityOwnershipCharacterizationTest и NeutralCityBattleOutcomeTest использованы для регрессии. Проверки прошли; итоговый VictoryRegressionSuite — 10 PASSED. Локальный коммит характеризации: 2f461ee8c.
