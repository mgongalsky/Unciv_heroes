# Architecture Discoveries & Key Insights

## UI System Patterns

### ExpanderTab Pattern
**Discovery**: Unciv has a well-established pattern for collapsible UI sections that should be followed consistently.

**Pattern Structure**:
1. Create separate `Table` class for the content (e.g., `HeroFeedingTable`)
2. Implement `asExpander()` method that returns `ExpanderTab`
3. Use persistence ID for state saving: `"ParentClass.SectionName"`
4. Parent class calls `addSectionName()` method that adds the expander

**Example Implementation**:
```kotlin
class HeroFeedingTable(val cityScreen: CityScreen) : Table(BaseScreen.skin) {
    fun asExpander(onChange: (() -> Unit)?): ExpanderTab {
        return ExpanderTab(
            title = "{Hero Feeding}",
            fontSize = Constants.defaultFontSize,
            persistenceID = "CityStatsTable.HeroFeeding",
            startsOutOpened = true,
            onChange = onChange
        ) {
            it.add(this)
            update()
        }
    }
}
```

**Key Files Using This Pattern**:
- `CitizenManagementTable.kt`
- `SpecialistAllocationTable.kt`  
- `CityReligionInfoTable.kt`
- `HeroFeedingTable.kt` (newly added)

## Notification System Architecture

### Core Components
1. **Icon** - Visual representation (from `NotificationIcon` object)
2. **Text** - Message content (supports translation keys in brackets)
3. **Action** - What happens when clicked (implements `NotificationAction`)

### Notification Action Pattern
**Key Requirement**: All actions must be serializable (`IsPartOfGameInfoSerialization`)

**Method Signature**: `addNotification(text: String, action: NotificationAction?, vararg icons: String)`

**Custom Action Example**:
```kotlin
data class HeroAction(val heroPosition: Vector2 = Vector2.Zero): NotificationAction, IsPartOfGameInfoSerialization {
    override fun execute(worldScreen: WorldScreen) {
        worldScreen.mapHolder.setCenterPosition(heroPosition, selectUnit = true)
        val heroTile = worldScreen.gameInfo.tileMap[heroPosition]
        val hero = heroTile.militaryUnit
        if (hero != null && hero.civInfo == worldScreen.viewingCiv) {
            worldScreen.bottomUnitTable.tileSelected(heroTile)
            worldScreen.shouldUpdate = true
        }
    }
}
```

**Available Icons** (from `NotificationIcon`):
- `Death` - For urgent/critical warnings
- `Food` - For food-related notifications
- `Gold`, `Science`, `Culture`, etc. - For resource notifications
- `War`, `Diplomacy` - For conflict/diplomacy
- `Question` - For uncertain situations

## Turn Processing Architecture  

### Turn Order
1. `GameInfo.nextTurn()` - Overall game turn coordination
2. `CivilizationInfo.startTurn()` - Per-civilization processing
3. `CityInfo.startTurn()` - Per-city processing (from civilization)
4. `MapUnit.startTurn()` - Per-unit processing (from civilization)

**Key Insight**: `MapUnit.startTurn()` is the ideal place for per-unit per-turn logic like food warnings.

### Unit State Classification
- **In City**: `currentTile.isCityCenter() == true` - Units in cities
- **In Campaign**: `currentTile.isCityCenter() == false` - Units in the field

**Important**: Only units "in campaign" should trigger food warnings.

## Food System Architecture

### Food Calculation
- **Consumption**: `army.calculateFoodMaintenance(isInCity: Boolean)`  
- **Current Reserves**: `unit.getCurrentFood()`
- **Capacity**: `unit.basicFoodCapacity` + bonuses

### Auto-Feed Logic Location
**Cities**: `CityInfo.autoFeedVisitingHero()` called from `CityInfo.startTurn()`
- Only affects heroes visiting the city
- Transfers surplus city food to hero up to capacity
- Respects food capacity bonuses from buildings

### Food Warning Logic
**Units**: `MapUnit.checkHeroFoodWarning()` called from `MapUnit.startTurn()`
- Calculates: `daysRemaining = currentFood / dailyConsumption`
- Warning threshold: ≤ 3 days remaining
- Only for units outside cities

## State Persistence Patterns

### Persistent Fields
- Must be declared as regular `var` fields (not `@Transient`)
- Must be added to `clone()` methods
- Automatically serialized/deserialized

**Example** (from `CityInfo`):
```kotlin
var autoFeedHero: Boolean = false  // This persists

@Transient
var cityStats = CityStats(this)  // This gets recalculated each turn
```

### Clone Method Pattern
When adding new persistent fields, MUST update clone methods:
```kotlin
fun clone(): CityInfo {
    val toReturn = CityInfo()
    // ... other fields ...
    toReturn.autoFeedHero = autoFeedHero  // REQUIRED!
    return toReturn
}
```

## Unit Selection Patterns

### Correct Unit Selection
**Wrong**: `worldScreen.bottomUnitTable.selectedUnit = unit` (not allowed)
**Right**: `worldScreen.bottomUnitTable.tileSelected(tile)` (proper method)

**Explanation**: The UI system expects tile selection which then determines unit selection, not direct unit assignment.

## Debugging Strategies

### Effective Debug Approach
1. Add strategic `println()` statements to understand flow
2. Run actual game to see real behavior vs expected behavior  
3. Check console output during gameplay
4. Remove debug output once issue is resolved

**Key Debug Points**:
- Turn processing methods (`startTurn()`, `endTurn()`)
- UI update methods (`update()`, event handlers)
- Calculation methods (food consumption, capacity, etc.)

### Common Debug Locations
- `MapUnit.startTurn()` - For per-unit logic
- `CityInfo.startTurn()` - For per-city logic  
- UI event handlers - For user interaction logic
- Notification sending - To confirm notifications are created

## Error Patterns to Avoid

### 1. Wrong Notification Signatures
**Problem**: Using wrong parameter order in `addNotification()`
**Solution**: Always use `addNotification(text, action, ...icons)`

### 2. Percentage Calculation Errors
**Problem**: Using non-existent methods like `.toPercent()`
**Solution**: Manual calculation `(1f + percentage / 100f)`

### 3. UI Direct Access
**Problem**: Trying to directly modify UI components
**Solution**: Use proper UI methods and patterns

### 4. State Persistence Issues  
**Problem**: Forgetting to add new fields to `clone()` methods
**Solution**: Always update clone methods when adding persistent state