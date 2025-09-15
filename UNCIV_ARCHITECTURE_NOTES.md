# Unciv Architecture Notes

## Ruleset Loading System

### Key Architecture Feature: Modular Ruleset Loading

**IMPORTANT**: Unciv uses a modular ruleset system where multiple JSON folders are loaded sequentially:

1. **Base Ruleset**: `jsons/Civ V - Vanilla/` - loaded first
2. **Extension Ruleset**: `jsons/Civ V - Gods & Kings/` - loaded second and **OVERWRITES/ADDS** to base

### Implications for Development:

When adding new JSON files (like GameMechanics.json), you MUST create them in **BOTH** folders:
- `/android/assets/jsons/Civ V - Vanilla/GameMechanics.json`
- `/android/assets/jsons/Civ V - Gods & Kings/GameMechanics.json`

If you only add to Vanilla, the Gods & Kings loading will result in **0 entries** in the final combined ruleset.

### Loading Process Example:
```
DEBUG: Loading ruleset from: jsons/Civ V - Vanilla
DEBUG: Found GameMechanics.json file
DEBUG: Loaded 3 game mechanics
DEBUG: Total gameMechanics in ruleset: 3

DEBUG: Loading ruleset from: jsons/Civ V - Gods & Kings
DEBUG: Found GameMechanics.json file  <-- THIS IS CRUCIAL
DEBUG: Loaded 3 game mechanics
DEBUG: Total gameMechanics in ruleset: 3  <-- Final count
```

### Debugging Tips:
- Add logging to `Ruleset.kt` `load()` method to see which folders are being loaded
- Add logging to `CivilopediaScreen.kt` to see final category entry counts
- Categories with 0 entries are automatically hidden from the UI

### Files Modified for GameMechanics Feature:
1. `CivilopediaCategories.kt` - Added GameMechanics enum entry
2. `GameMechanic.kt` - New class for representing game mechanics
3. `Ruleset.kt` - Added loading logic for GameMechanics.json
4. `CivilopediaScreen.kt` - Added category support in getCategoryIterator()
5. `/android/assets/jsons/Civ V - Vanilla/GameMechanics.json` - Base content
6. `/android/assets/jsons/Civ V - Gods & Kings/GameMechanics.json` - Extension content

This architecture allows for modular expansion and mod support.

## Object Cloning and State Persistence System

### Key Architecture Feature: Turn-Based Object Recreation

**CRITICAL**: Unciv uses a turn-based object cloning system where game objects are recreated each turn:

- Every turn, all game objects (CityInfo, CivilizationInfo, MapUnit, etc.) are **cloned**
- The previous turn's objects are discarded
- All state must be persisted in **serializable fields** only
- `@Transient` fields are recalculated each turn and **not saved**

### Implications for Development:

When adding new state fields to game classes, you MUST:

1. **Add to class definition**: `var newField: Type = defaultValue`
2. **Add to clone() method**: `toReturn.newField = newField`
3. **Never rely on @Transient fields** for persistent state

### Example:
```kotlin
class CityInfo {
    // This WILL be saved and persist between turns
    var autoFeedHero: Boolean = false
    
    // This will NOT be saved and gets recalculated each turn
    @Transient
    var cityStats = CityStats(this)
    
    fun clone(): CityInfo {
        val toReturn = CityInfo()
        // MUST include new field or it will reset to default!
        toReturn.autoFeedHero = autoFeedHero
        return toReturn
    }
}
```

### Common Pitfalls:
- Forgetting to add new fields to `clone()` methods
- Relying on object references that become invalid after cloning
- Using `@Transient` fields for persistent state
- Assuming objects remain the same instance between turns
