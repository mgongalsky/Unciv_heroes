# Enemy Unit Click Processing Flow in the Battle System

This document describes the sequence of steps and class interactions when the user clicks on an enemy unit in the battle system.

---

## General Principles
The system is divided into several layers of responsibility:
1. **User Interaction** — Managed in `BattleScreen`, where user actions (e.g., clicks) are tracked.
2. **Request Processing** — Handled by `BattleManager`, responsible for the battle's business logic.
3. **UI Updates** — Results are returned to `BattleScreen` to update the visual state.

---

## Process Steps

### 1. User Clicks on an Enemy Unit
When the user clicks on a tile containing an enemy unit, the event is handled by a click listener set up in the `addTiles` method of the `BattleScreen` class.

Code example:
```kotlin
tileGroup.addListener(object : ClickListener() {
    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        if (manager.isBattleOn()) {
            handleTileClick(tileGroup, x, y) // Pass click to handler
        }
    }
})
```

### 2. Processing Click in `handleTileClick`

The `handleTileClick` method analyzes the user's click to determine the type of action: attack, shoot, move, or an error. Based on this, a `BattleActionRequest` object is created.

Code Example:
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

### Key Steps:
1. **Determine Action:** Check if the click is an attack, shoot, or move attempt.
2. **Validate Action Feasibility:** Ensure the action can be performed:
    - Can the unit shoot? (`manager.canShoot`)
    - Is the tile occupied by an enemy? (`manager.isHexOccupiedByEnemy`)
3. **Create Request:** Construct a `BattleActionRequest` object describing the action (type, target, direction).
4. **Send Request:** Pass the object for processing via the `onPlayerActionReceived` callback.

---

## 3. Passing the Request to `BattleManager`

The `BattleScreen` passes the generated `BattleActionRequest` to the `performTurn` method in the `BattleManager` class for action execution.

### Code Example:
```kotlin
val result = manager.performTurn(actionRequest)
```

---

## 4. Action Processing in `BattleManager`

The `performTurn` method analyzes the request and executes the corresponding action:
- **Attack:** Calls the `attack` method.
- **Move:** Updates the unit's position.
- **Shoot:** Executes attack logic without position changes.

### Attack Handling Example:
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

### Damage Calculation in `attack`:
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

## 5. Returning the Result to `BattleScreen`

After executing the action, `BattleManager` returns a `BattleActionResult` object containing:
- Action success status.
- Action type (attack, shoot, move).
- Additional effects (e.g., luck or morale).

### Code Example:
```kotlin
val result = manager.performTurn(actionRequest)
handleBattleResult(result, currentTroop)
```

---

## 6. Updating the Interface

The `handleBattleResult` method updates the visual state:
- Moves the unit for movement actions.
- Updates health and troop count.
- Displays visual effects (e.g., luck rainbow, morale bird).

### Code Example:
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

## 7. Completing the Action

After successfully updating the interface:
1. **Advance Turn Queue:**
   ```kotlin
   manager.advanceTurn()
   movePointerToNextTroop()
   updateTilesShadowing()
   ```
2. **End the Battle if Needed:**
   ```kotlin
   if (result.battleEnded) {
       shutdownScreen()
   }
   ```

---

## Full Process Flow

1. **Click on an Enemy Unit:**
    - Tracked by the listener in `tileGroup.addListener`.
    - Processed in `handleTileClick`.
2. **Request Formation:**
    - Create a `BattleActionRequest` object.
3. **Request Transmission:**
    - Sent to `BattleManager.performTurn`.
4. **Action Execution:**
    - Logic executed in `BattleManager` (`attack`, `move`, `shoot`).
5. **Result Return:**
    - A `BattleActionResult` object is returned.
6. **Interface Update:**
    - Update troops and visuals (e.g., visual effects).
7. **Next Turn or End Battle:**
    - Advance the turn queue (`advanceTurn`).
    - Close the screen (`shutdownScreen`) if the battle is over.

This process ensures clear interaction between system components and proper separation of responsibilities.
```
