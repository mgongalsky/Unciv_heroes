# Overview of the Architecture and Class Interactions

Our project follows a modular and extensible design, leveraging the **Model-View-Controller (MVC)** architectural pattern. This approach separates the logic, visual representation, and user interaction into distinct components. The main principles guiding this design are:

1. **Separation of Concerns**:
   Each class has a well-defined role, ensuring a clear distinction between data, logic, and presentation.

2. **Modularity**:
   Classes are designed to be reusable and easily testable, minimizing dependencies between components.

3. **Scalability**:
   The architecture supports easy addition of new features without disrupting the existing structure.

4. **Flexibility**:
   Classes communicate through clearly defined interfaces and objects (e.g., `BattleActionRequest`, `BattleActionResult`), allowing for easy replacement or extension of components.

The system is divided into two main subsystems:
1. **Army Management and Display**
   Handles the visualization and logic for managing armies and troops outside battles.
2. **Battle System**
   Implements the mechanics of battles, including troop interactions, AI logic, and battle resolution.

---

## 1. Army Management and Display

This subsystem handles the organization and display of armies within the user interface, as well as troop management operations like adding, removing, swapping, and splitting troops.

### **Key Classes**
1. **[CityScreen](#)**
    - **Role**: Displays armies within the city. It serves as the main interface for interacting with armies in a non-battle context.
    - **Interaction**: Uses `ArmyView` to display armies and `TroopArmyView` for individual troops.

2. **[ArmyManager](ArmyManagerEng.md)**
    - **Role**: Provides logic for managing two armies, including adding, removing, swapping, combining, and splitting troops.
    - **Interaction**: Operates on instances of `ArmyInfo` to perform modifications.

3. **[ArmyInfo](#)**
    - **Role**: A logical representation of an army. Stores information about troops and handles low-level operations, such as troop addition or removal.
    - **Interaction**: Used by `ArmyManager` and `BattleManager`.

4. **[TroopInfo](#)**
    - **Role**: Represents a single troop logically, including its type, size, and properties.
    - **Interaction**: Accessed by `ArmyInfo` for troop management and by `BattleManager` during battles.

5. **[TroopArmyView](#)**
    - **Role**: A visual representation of a single troop in an army, displayed within the army's interface.
    - **Interaction**: Used by `ArmyView` to display individual troop details.

6. **[ArmyView](#)**
    - **Role**: Provides a visual representation of an entire army. It organizes multiple `TroopArmyView` instances and offers interaction options for troop exchange.
    - **Interaction**: Accesses `ArmyManager` for logic operations and `TroopArmyView` for troop rendering.

---

## 2. Battle System

This subsystem handles the implementation of battles, including the logical mechanics, visual representation, and AI decision-making.

### **Key Classes**
1. **[BattleScreen](BattleScreenEng.md)**
    - **Role**: Provides the visual interface for battles. Handles user interactions and updates troop positions on the battlefield.
    - **Interaction**: Utilizes `BattleManager` for battle logic and `TroopBattleView` for troop rendering.

2. **[BattleManager](BattleManagerEng.md)**
    - **Role**: Contains the logic for battles, including turn management, troop actions, and battle resolution.
    - **Interaction**: Communicates with `BattleScreen` for visual updates and uses `ArmyInfo` and `TroopInfo` for battle logic.

3. **[AIBattle](#)**
    - **Role**: Handles AI-controlled troop actions during battles.
    - **Interaction**: Works with `BattleManager` to decide and execute actions for AI troops.

4. **[TroopBattleView](#)**
    - **Role**: A visual representation of a troop during battles. Displays troop actions, such as movement and attacks.
    - **Interaction**: Managed by `BattleScreen` and interacts with the battlefield tiles.

5. **[TroopInfo](#)**
    - **Role**: Same as in the army management section but used here for tracking troop states during battles.
    - **Interaction**: Accessed by `BattleManager` for troop-related logic.

6. **[ArmyInfo](#)**
    - **Role**: Same as in the army management section but used here for managing armies involved in the battle.
    - **Interaction**: Provides the necessary data to `BattleManager`.

---

## 3. Supporting Classes for Communication

To facilitate communication between different components, the system uses the following intermediary classes:

1. **BattleActionRequest**
    - **Purpose**: Represents an action request (e.g., move, attack) sent by a player or AI troop.
    - **Usage**: Passed from `BattleScreen` to `BattleManager`.

2. **BattleActionResult**
    - **Purpose**: Represents the result of a troop action (e.g., success or failure).
    - **Usage**: Returned by `BattleManager` to `BattleScreen` for updating the interface.

3. **BattleTotalResult**
    - **Purpose**: Represents the overall result of the battle, including the winning army.
    - **Usage**: Generated by `BattleManager` at the end of a battle.

---

## MVC Pattern in Practice

- **Model**:
    - `ArmyInfo`, `TroopInfo` for army and troop logic.
    - `BattleManager` and `AIBattle` for battle logic.

- **View**:
    - `ArmyView` and `TroopArmyView` for army interfaces.
    - `BattleScreen` and `TroopBattleView` for battle interfaces.

- **Controller**:
    - `ArmyManager` for managing troop interactions between armies.
    - `BattleManager` for handling troop actions during battles.

---

This architecture ensures a clean separation of concerns, making the system modular, maintainable, and scalable.
