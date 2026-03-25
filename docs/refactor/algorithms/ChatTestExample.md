## Наш случай: MapUnit в Unciv Heroes

---

**Отправная точка**

`MapUnit` — класс-монстр в игровом движке. Сотни строк, зависимости на весь игровой мир. Задача: написать первый тест не сломав ничего в продакшне.

---

**Шаг 1 — Конструкционный тест**

```kotlin
@Test
fun testCreate() {
    val unit = MapUnit()
}
```

Взрыв: `ExceptionInInitializerError` — `companion object` при загрузке класса создаёт `CivilizationInfo()`, который лезет в файловую систему за ruleset.

**Приём:** Introduce Static Setter — убрали eager initialization из `companion object`, добавили `setTestingInstance()`.

---

**Шаг 2 — Второй взрыв**

`CivilizationInfo()` сам по себе тяжёлый — в `init` вызывает `loadRulesetForMap()`.

**Приём:** Extract and Override Method — вынесли вызов в `open fun initialize()`, переопределили в `FakeCivilizationInfo` как пустой метод.

---

**Шаг 3 — Третий взрыв**

`ArmyInfo` создаётся прямо в теле класса и тянет `GameConstants.armySize` который читает файл через `Gdx.files`.

**Приём:** Extract and Override Factory Method — вынесли создание армии в `open fun createArmy()`, переопределили в `TestableMapUnit`.

---

**Тест зелёный. Коммит.**

---

**Шаг 4 — Characterization зонд**

Прогнали сценарии с едой через `println`. Никакой придуманной логики — просто смотрели что выдаёт код:

```
currentFood: 3.0
After addFood(5f): 8.0
After addFood(-10f): -2.0  // уходит в минус — нет ограничений
calculateArmyPopulation: 0  // пустая армия
```

**Зафиксировали в characterization test. Коммит.**

---

**Шаг 5 — Выделение юзкейса**

`checkHeroFoodWarning()` — приватный метод с чистой логикой но инфраструктурным хвостом (`currentTile`, `civInfo.addNotification`).

Разделили на два слоя:

```
HeroFoodWarningUseCase  ←  чистая логика, тестируется легко
        ↓
checkHeroFoodWarning()  ←  инфраструктура, делегирует юзкейсу
```

`FakeCivilizationInfo` переопределил `addNotification` — уведомления теперь пишутся в список `capturedNotifications` вместо игрового UI.

---

**Шаг 6 — Characterization зонд на юзкейс**

Прогнали 5 сценариев, зафиксировали:

```
food=0, consumption=2  →  "has no food left!"
food=2, consumption=2  →  "has food for only 1 turn!"
food=6, consumption=2  →  "has food for only 3 turns!"  // граница
food=20, consumption=2 →  []  // нет предупреждения
food=5, consumption=0  →  []  // нет потребления
```

**Зафиксировали в characterization test. Коммит.**

---

**Итог**

За одну сессию:
- 3 приёма Фезерса чтобы войти в класс
- 0 изменений продакшн-логики
- 2 файла фейков
- 6 characterization тестов которые документируют реальное поведение
