# BattleManager

## Overview

The `BattleManager` class is the core logic component for handling battles in the game. It separates the battle logic from visual representation, ensuring clear architecture and maintainability. This class is responsible for initializing battles, managing the turn queue, handling troop actions (movement, attacks, and shooting), and determining the battle's outcome.

---

## Key Responsibilities

1. **Initialize the Turn Queue**: Organizes the order in which troops take turns based on their speed and other priority rules.
2. **Manage Troop Actions**: Handles troop movement, attacks, and shooting actions, ensuring all rules and constraints are applied.
3. **Track Battle State**: Determines whether the battle is ongoing or finished, and manages troop removal when they perish.
4. **Calculate Battle Outcomes**: Evaluates the winner based on surviving troops in attacker and defender armies.

---

## Architecture Role

- **Input**: Receives two armies (`ArmyInfo`) as inputs: the attacker and the defender.
- **Processing**: Applies game logic to simulate battles, including troop movement and interactions.
- **Output**: Produces results in the form of battle states, action results (`BattleActionResult`), and the overall winner (`BattleTotalResult`).

The `BattleManager` interacts primarily with `ArmyInfo`, `TroopInfo`, and `HexMath` for positioning and movement calculations. It avoids direct interaction with visual components, adhering to the **Model-View-Controller (MVC)** principle.

---

## Key Functions

### `initializeTurnQueue()`
- **Description**: Initializes the order of troops in the battle, prioritizing troops based on their speed. Attacking troops take priority when speeds are equal.
- **Purpose**: Ensures fairness and correct turn-taking during the battle.

### `getCurrentTroop()`
- **Description**: Retrieves the troop currently taking its turn from the turn queue.
- **Purpose**: Provides the active troop for the current turn.

### `performTurn(actionRequest: BattleActionRequest): BattleActionResult`
- **Description**: Executes a troop's action (move, attack, or shoot) based on the provided request.
- **Key Logic**:
    - Validates the action (e.g., checks if movement is possible or the target is valid).
    - Updates troop positions or health accordingly.
- **Output**: Returns the result of the action, including success, morale or luck triggers, and whether the battle has ended.

### `getBattleResult(): BattleTotalResult?`
- **Description**: Determines the winner of the battle based on surviving troops.
- **Output**:
    - Attacker wins if the defender has no remaining troops.
    - Defender wins if the attacker has no remaining troops.
    - Returns `null` if both armies still have troops.

### `attack(defender: TroopInfo, attacker: TroopInfo? = getCurrentTroop()): Boolean`
- **Description**: Simulates an attack action between an attacking troop and a defending troop.
- **Logic**:
    - Calculates damage based on attacker strength and defender health.
    - Applies special conditions like luck or morale.
    - Updates defender health and troop count, removing them if perished.
- **Output**: Indicates whether the attack was boosted by luck.

### `getReachableTiles(troop: TroopInfo): List<Vector2>`
- **Description**: Calculates all tiles a troop can move to based on its speed and position.
- **Purpose**: Provides valid movement options for the troop.

### `isHexAchievable(troop: TroopInfo, targetPosition: Vector2): Boolean`
- **Description**: Checks if a given hex tile is within the troop's movement range and is part of the battlefield.
- **Purpose**: Ensures valid movements within the game rules.

### `isBattleOn(): Boolean`
- **Description**: Determines if the battle is still ongoing by checking if both armies have surviving troops.
- **Purpose**: Provides a high-level check for battle completion.

### `removeTroop(troop: TroopInfo)`
- **Description**: Removes a troop from the battle, cleaning up the turn queue and army it belonged to.
- **Purpose**: Maintains an accurate state of the battlefield after troop losses.

---

## Usage Example

```kotlin
val attackerArmy = ArmyInfo(...)
val defenderArmy = ArmyInfo(...)
val battleManager = BattleManager(attackerArmy, defenderArmy)

// Initialize battle
battleManager.initializeTurnQueue()

// Perform turns
val actionRequest = BattleActionRequest(currentTroop, ActionType.MOVE, targetPosition)
val result = battleManager.performTurn(actionRequest)

// Check if the battle is ongoing
if (!battleManager.isBattleOn()) {
    val result = battleManager.getBattleResult()
    println("Battle Winner: ${result?.winningArmy}")
}
