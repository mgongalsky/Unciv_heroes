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