# Clean Architecture: Unciv (Stage 0–12)

## 1. Слои и зависимости
- clean/domain — бизнес-сущности и DomainEvent (без UI/IO/Time).
- clean/application — usecases и ports (Clock, Repositories, Ruleset, EventBus, Notifications, Random, Battle).
- clean/adapters — реализации портов (persistence, events, random, ruleset, logger, …).
- clean/presentation — отображение событий и маппинг в UI-модели (без прямых ссылок на домен/IO).
- logic/ — тонкий делегат вокруг прежних API (переезд к домену идёт постепенно).

Направление зависимостей: adapters -> ports <- usecases <- domain.

## 2. События (DomainEvent)
- Генерируются в домене/use case.
- Сбрасываются через FlushDomainEventsUseCase в конце операции (граница use-case).
- Доставка — через DomainEventBus (упорядоченные DomainEventHandler по приоритетам).
- Ошибки хэндлеров не валят цепочку.

## 3. Уведомления
- Старый путь: EventToNotificationMapper -> civ.addNotification.
- Новый путь (за фиче-флагом): EventToNotificationMapperV2 (чистый) -> NotificationsPort через UiNotificationsEventHandlerV2.
- По умолчанию выключено, чтобы избежать дублей.

## 4. Бой
- ResolveAttackUseCase + CombatResolver (адаптер к legacy).
- Анимации/всплывашки — в presentation через события.
- RNG изолирован RandomPort для тестируемости/детерминизма.

## 5. Сохранения
- DTO-слой: GameInfoDto — отделяет формат от домена.
- Versioned save: SaveEnvelopeDto (V1) + SaveMigrationRegistry.
- По умолчанию запись LEGACY (нулевая регрессия), чтение — legacy + V1.
- Переключение на V1-запись — фиче-флаг.

## 6. CompositionRoot
- Централизованный wiring: eventBus, handlers, VersionedSaveIO, фиче-флаги.
- Создаётся при старте сессии, передаётся адаптерам/репозиториям/use case.

## 7. Тестирование
- Юнит-тесты use case без UI/IO.
- Контрактные тесты портов (например, VersionedSaveIO).
- Е2Е смоук — прежние сценарии запуска.

## 8. Правила вклада (TL;DR)
- В домене/апп — никаких ссылок на UI/IO/Time/Random (только порты).
- Новые эффекты — через DomainEvent + хэндлеры.
- Публичные API меняем за адаптером (минимум диффа).
- Любые изменения сериализации — только через DTO/Versioned слой.
