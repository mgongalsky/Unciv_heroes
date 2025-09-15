# BattleScreen Class Overview

The `BattleScreen` class is responsible for managing the visual representation of a battle in the game. While the underlying battle logic is handled by the `BattleManager` class, `BattleScreen` focuses on rendering, user interactions, and animations related to the battle. It connects user inputs (e.g., clicks) to logical actions and displays results on the screen.

## Responsibilities
1. **Rendering the Battlefield**: Creates and manages a grid of hexagonal tiles representing the battlefield.
2. **Troop Representation**: Displays troops from both armies and updates their positions, actions, and stats during the battle.
3. **Pointer and Cursor Management**: Highlights the active troop and changes the mouse cursor based on user interactions (e.g., attack, move).
4. **Action Handling**: Translates user inputs into logical actions (e.g., move, attack) and communicates them to the `BattleManager`.
5. **Battle Animations**: Handles visual effects such as morale birds, luck rainbows, and troop movements.

## Key Methods

### `draw_pointer()`
Draws a visual pointer on the currently active troop's tile and ensures it is added to the appropriate group. It also manages the pointer's appearance and placement relative to the troop's position.

### `addTiles()`
Creates the battlefield by adding tiles and initializing troop positions. It also sets up event listeners for each tile to handle user interactions such as clicks and mouse movements.

### `handleTileClick(tileGroup: TileGroup, x: Float, y: Float)`
Determines the type of action (move, attack, shoot) based on the clicked tile. Validates the action and sends a request to the `BattleManager` for execution. Handles invalid or unreachable actions gracefully.

### `moveTroopView(troopView: TroopBattleView, targetTileGroup: TileGroup)`
Updates the visual position of a troop on the battlefield by moving its corresponding view to the target tile group.

### `refreshTroopViews()`
Synchronizes troop views with the current state of their respective armies in the `BattleManager`. Updates stats, handles troop deaths, and removes invalid views.

### `updateTilesShadowing()`
Highlights tiles that are reachable by the currently active troop, using transparency to indicate accessibility.

### `showMoraleBird(troopView: TroopBattleView)`
Displays a morale bird animation above a troop when it gains a morale bonus. The animation involves a fade-in and fade-out sequence.

### `showLuckRainbow(troopView: TroopBattleView)`
Displays a luck rainbow animation near a troop when it benefits from a luck effect. Similar to the morale bird, the animation fades in and out.

### `chooseCrosshair(tileGroup: TileGroup, x: Float, y: Float, width: Float)`
Determines the appropriate cursor (e.g., attack, move, shoot) based on the tile and mouse position, providing visual feedback for user actions.

### `pixelToDirection(x: Float, y: Float, width: Float): Direction`
Calculates the direction of an attack based on the mouse click position within a hex tile. Used to determine where a troop should move to attack an enemy.

## Architecture Position
The `BattleScreen` sits at the intersection of the game's UI and battle logic. It acts as a bridge between user inputs (via the UI) and the battle simulation (via the `BattleManager`). It ensures that the player's actions are visually represented and that feedback from the battle logic is displayed in an engaging and informative way.

## Usage Notes
- **Not for AI Battles**: The `BattleScreen` is designed for player interaction and should not be used for AI-only battles.
- **Event-Driven**: The class heavily relies on event listeners and callbacks to handle user interactions and update the UI dynamically.
- **Coroutines**: Some functions (e.g., `runBattleLoop`) leverage coroutines for asynchronous operations, such as waiting for user input.

