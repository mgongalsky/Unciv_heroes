# Поток обработки клика на вражеского воина в системе битвы

Этот документ описывает последовательность шагов и взаимодействие классов при обработке клика пользователя на вражеского воина в системе битвы.

---

## Основные принципы
Система разделена на несколько уровней ответственности:
1. **Взаимодействие с пользователем** — Управляется в `BattleScreen`, где отслеживаются действия пользователя (например, клики).
2. **Обработка запросов** — Выполняется в `BattleManager`, который отвечает за бизнес-логику битвы.
3. **Обновление интерфейса** — Результаты передаются в `BattleScreen` для обновления визуального состояния.

---

## Этапы процесса

### 1. Пользователь кликает на вражеского воина
Когда пользователь кликает на тайл с вражеским воином, событие обрабатывается слушателем кликов, установленным в методе `addTiles` класса `BattleScreen`.

Пример кода:
```kotlin
tileGroup.addListener(object : ClickListener() {
    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        if (manager.isBattleOn()) {
            handleTileClick(tileGroup, x, y) // Передача клика в обработчик
        }
    }
})
```

---

### 2. Обработка клика в `handleTileClick`

Метод `handleTileClick` анализирует клик пользователя, чтобы определить тип действия: атака, стрельба, перемещение или ошибка. На основе этого создается объект `BattleActionRequest`.

Пример кода:
```kotlin
if (manager.canShoot(currentTroop, targetPosition) && manager.isHexOccupiedByEnemy(currentTroop, targetPosition)) {
    val actionRequest = BattleActionRequest(
        troop = currentTroop,
        targetPosition = targetPosition,
        actionType = ActionType.SHOOT
    )
    onPlayerActionReceived?.invoke(Pair(actionRequest, tileGroup))
    return
}

if (manager.isHexOccupiedByEnemy(currentTroop, targetPosition)) {
    val direction = pixelToDirection(x, y, tileGroup.baseLayerGroup.width)
    val attackRequest = BattleActionRequest(
        troop = currentTroop,
        targetPosition = targetPosition,
        actionType = ActionType.ATTACK,
        direction = direction
    )
    onPlayerActionReceived?.invoke(Pair(attackRequest, tileGroup))
    return
}
```

### Основные шаги:
1. **Определение действия:** Проверяется, является ли клик атакой, стрельбой или попыткой перемещения.
2. **Проверка условий действия:** Проверяется возможность выполнения:
    - Может ли юнит стрелять? (`manager.canShoot`).
    - Занята ли клетка вражеским юнитом? (`manager.isHexOccupiedByEnemy`).
3. **Формирование запроса:** Создается объект `BattleActionRequest`, описывающий действие (тип, цель, направление).
4. **Передача запроса:** Объект передается через callback `onPlayerActionReceived`.

---

## 3. Передача запроса в `BattleManager`

`BattleScreen` передает сформированный объект `BattleActionRequest` в метод `performTurn` класса `BattleManager` для выполнения действия.

Пример кода:
```kotlin
val result = manager.performTurn(actionRequest)
```

---

## 4. Обработка действия в `BattleManager`

Метод `performTurn` анализирует запрос и выполняет соответствующее действие:
- **Атака:** Вызывается метод `attack`.
- **Перемещение:** Обновляется позиция юнита.
- **Стрельба:** Выполняется логика атаки без изменения позиции.

Пример обработки атаки:
```kotlin
ActionType.ATTACK -> {
    val defender = getTroopOnHex(actionRequest.targetPosition)
    if (!isHexOccupiedByEnemy(actionRequest.troop, actionRequest.targetPosition)) {
        return BattleActionResult(success = false, errorId = ErrorId.INVALID_TARGET)
    }
    val isLuck = attack(defender, actionRequest.troop)
    return BattleActionResult(success = true, isLuck = isLuck)
}
```

### Расчет урона в `attack`:
```kotlin
fun attack(defender: TroopInfo, attacker: TroopInfo): Boolean {
    var damage = attacker.currentAmount * attacker.baseUnit.damage
    defender.currentAmount -= damage / defender.baseUnit.health
    if (defender.currentAmount <= 0) {
        perishTroop(defender)
    }
    return true
}
```

---

## 5. Возврат результата в `BattleScreen`

После выполнения действия `BattleManager` возвращает объект `BattleActionResult`, содержащий:
- Успешность действия.
- Тип действия (атака, стрельба, перемещение).
- Дополнительные эффекты (например, удача или мораль).

Пример обработки результата:
```kotlin
val result = manager.performTurn(actionRequest)
handleBattleResult(result, currentTroop)
```

---

## 6. Обновление интерфейса

Метод `handleBattleResult` обновляет визуальное состояние:
- Перемещает юнита, если это движение.
- Обновляет здоровье и количество войск.
- Отображает визуальные эффекты (например, радугу удачи, птицу морали).

Пример кода:
```kotlin
if (result.success) {
    when (result.actionType) {
        ActionType.ATTACK -> {
            refreshTroopViews()
            if (result.isLuck) showLuckRainbow(currentTroopView)
        }
        ActionType.MOVE -> {
            currentTroopView.updatePosition(targetTileGroup)
        }
    }
}
```

---

## 7. Завершение действия

После успешного обновления интерфейса:
1. **Изменение очереди ходов:**
   ```kotlin
   manager.advanceTurn()
   movePointerToNextTroop()
   updateTilesShadowing()
   ```
2. **Завершение битвы, если необходимо:**
   ```kotlin
   if (result.battleEnded) {
       shutdownScreen()
   }
   ```

---

## Полная последовательность

1. **Клик на вражеского юнита:**
    - Отслеживается слушателем в `tileGroup.addListener`.
    - Обрабатывается в `handleTileClick`.
2. **Формирование запроса:**
    - Создается объект `BattleActionRequest`.
3. **Передача запроса:**
    - Передается в `BattleManager.performTurn`.
4. **Выполнение действия:**
    - Логика выполняется в `BattleManager` (`attack`, `move`, `shoot`).
5. **Возврат результата:**
    - Возвращается объект `BattleActionResult`.
6. **Обновление интерфейса:**
    - Обновляются войска и визуальные эффекты.
7. **Переход к следующему ходу или завершение битвы:**
    - Меняется очередь ходов (`advanceTurn`).
    - Закрывается экран (`shutdownScreen`), если битва завершена.

Этот процесс обеспечивает четкое взаимодействие компонентов системы и разделение ответственности.
```
