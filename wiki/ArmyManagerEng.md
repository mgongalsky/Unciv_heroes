# Overview of the ArmyManager Class

The `ArmyManager` class provides functionality for managing armies in the game. It enables adding, removing, swapping, combining, and splitting troops within one or two armies. Its primary purpose is to simplify operations on armies and ensure their correctness.

## Key Responsibilities
1. **Army Management**: Supports working with one or two armies simultaneously.
2. **Adding and Removing Troops**: Allows dynamic modification of army composition.
3. **Troop Swapping**: Facilitates moving troops between armies.
4. **Combining and Splitting**: Enables merging identical troops or splitting them into parts.

## Key Methods

### `addTroop(troop: TroopInfo, toArmy1: Boolean = true): Boolean`
Adds the specified troop to the chosen army (`army1` or `army2`).

- **Parameters**:
    - `troop`: The troop to add.
    - `toArmy1`: If `true`, adds to `army1`. Otherwise, adds to `army2`.
- **Returns**: `true` if the troop was successfully added; `false` otherwise.

### `removeTroop(index: Int, fromArmy1: Boolean = true): TroopInfo?`
Removes a troop by its index from the specified army.

- **Parameters**:
    - `index`: The index of the troop to remove.
    - `fromArmy1`: If `true`, removes from `army1`. Otherwise, removes from `army2`.
- **Returns**: The removed troop, or `null` if the operation failed.

### `getTroop(index: Int, fromArmy1: Boolean = true): TroopInfo?`
Retrieves a troop by its index from the specified army.

- **Parameters**:
    - `index`: The index of the troop to retrieve.
    - `fromArmy1`: If `true`, retrieves from `army1`. Otherwise, retrieves from `army2`.
- **Returns**: The troop at the specified index, or `null` if not found.

### `swapOrCombineTroops(...)`
Swaps or combines two troops between the same or different armies.

- **Parameters**:
    - `firstArmy`, `secondArmy`: The armies involved in the operation.
    - `firstIndex`, `secondIndex`: The indices of the troops in the armies.
    - `combine`: If `true`, attempts to combine the troops if they are of the same type.
- **Returns**: `true` if the operation was successful.

### `splitTroop(...)`
Splits a troop into two separate slots within the same or different armies.

- **Parameters**:
    - `sourceArmy`, `targetArmy`: The armies from and to which the split is performed.
    - `sourceIndex`, `targetIndex`: The indices of the troops in the armies.
    - `finalFirstTroopCount`: The number of units remaining in the source troop.
- **Returns**: `true` if the split was successful.

## Usage Examples
1. **Adding a troop**:
   ```kotlin
   val troop = TroopInfo(100, "Swordsman")
   val success = armyManager.addTroop(troop, toArmy1 = true)
