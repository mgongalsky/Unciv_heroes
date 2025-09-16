# Unit Production and Population Mechanics Analysis

## Key Findings from Code Analysis

### Conscription System (Lines 122-125, 330-342 in CityInfo.kt)

**Core Variables:**
```kotlin
var conscriptionRate = 30        // Amount of soldiers per 1 population
var currConscription = 0         // Current conscription accumulator
```

**Production Logic:**
```kotlin
fun addBuiltUnits(name: String, amountToAdd: Int): Boolean {
    val oldCurrConscription = currConscription
    currConscription += amountToAdd
    val populationToReduce = currConscription / conscriptionRate
    currConscription %= conscriptionRate
    if (population.population - populationToReduce >= 1) {
        population.addPopulation(-populationToReduce)
        garrisonInfo.addUnits(name, amountToAdd)
        return true
    } else {
        currConscription = oldCurrConscription
        return false
    }
}
```

### How Unit Production Affects Population

**Accumulator System:**
1. Each unit produced adds +1 to `currConscription`
2. When `currConscription >= conscriptionRate` (30), population decreases by 1
3. `currConscription` resets to remainder (`currConscription % conscriptionRate`)

**Example Scenarios:**
- **Scenario 1**: City produces 25 units
  - `currConscription = 25` (no population loss yet)
  - Population unchanged
  
- **Scenario 2**: City produces 30 units
  - `currConscription = 30`
  - `populationToReduce = 30 / 30 = 1`
  - Population decreases by 1
  - `currConscription = 0`

- **Scenario 3**: City produces 35 units
  - `currConscription = 35` 
  - `populationToReduce = 35 / 30 = 1`
  - Population decreases by 1
  - `currConscription = 5` (remainder)

### Population Requirements

**Safety Check:**
```kotlin
if (population.population - populationToReduce >= 1) {
    // Proceed with unit production
} else {
    // Reject production, restore old conscription value
    return false
}
```

**Key Rule**: Cities must maintain at least 1 population at all times.

### Garrison System Integration

**Garrison Storage:**
- Produced units are added to `garrisonInfo` (ArmyInfo object)
- Garrison has predefined starting troops (see constructor lines 175-186):
```kotlin
garrisonInfo = ArmyInfo(
    civInfo,
    "Archer" to 35,
    "Peasant" to 20
)
```

**Initial Garrison Composition** (lines 183-186):
```kotlin
garrison.add(TroopInfo(30, "Archer"))
garrison.add(TroopInfo(20, "Spearman")) 
garrison.add(TroopInfo(10, "Horseman"))
garrison.add(TroopInfo(15, "Swordsman"))
```

### Population and Army Relationship

**City Foundation** (line 232):
```kotlin
population.setPopulation(
    ruleset.eras[startingEra]!!.settlerPopulation + 
    foundingUnit.calculateArmyPopulation()
)
```

**Key Insight**: Starting city population = base era population + founding unit's army size

## Balance Implications

### Current Balance Parameters

1. **Conscription Rate**: 30 units per 1 population
   - Relatively high threshold before population impact
   - Allows significant military buildup without demographic consequences

2. **Population Protection**: Minimum 1 population always maintained
   - Prevents cities from being completely depopulated
   - Creates natural limit on military production

3. **Accumulator System**: Gradual impact rather than per-unit cost
   - Small unit production has no demographic impact
   - Large military buildups eventually reduce population

### Potential Balance Adjustments

**To increase population impact:**
- Reduce `conscriptionRate` (e.g., from 30 to 20 or 15)
- Makes each population "worth" fewer military units

**To decrease population impact:**  
- Increase `conscriptionRate` (e.g., to 40 or 50)
- Allows larger military without demographic consequences

**Dynamic conscription rates:**
- Different unit types could have different conscription costs
- Elite units might "cost" more population than basic units
- Could be implemented by modifying `addBuiltUnits()` to accept cost parameter

## Food System Integration

### Current Food Mechanics
- Cities consume food for population maintenance (2 food per citizen per turn)
- Garrison units consume food from city production
- Heroes can be supplied from city food reserves via auto-supply system

### Population-Military-Food Triangle
1. **Population** → produces food and allows military conscription
2. **Military Units** → consume food for maintenance  
3. **Food Production** → supports both population growth and military upkeep

**Balance Tension**: More military units = more food consumption but fewer citizens to produce food (if population decreases due to conscription)

## Recommendations for Balance Tuning

### Short-term Adjustments
1. **Monitor conscription rate**: Current rate of 30 might be too permissive
2. **Consider unit-specific costs**: Different units having different population impacts
3. **Food consumption scaling**: Ensure military food costs scale appropriately with production

### Long-term Enhancements  
1. **Population recovery**: Mechanism for cities to recover population over time
2. **Economic vs military focus**: Buildings/policies that modify conscription efficiency
3. **Veteran units**: Units that survive battles might become more food-efficient

## Code Locations for Balance Modifications

**Key Files:**
- `CityInfo.kt` lines 122-125: Conscription rate variables
- `CityInfo.kt` lines 330-342: Unit production logic  
- `ArmyInfo.kt`: Food consumption calculations
- `PopulationManager.kt`: Population management and growth

**Key Variables to Adjust:**
- `conscriptionRate`: Units per population (currently 30)
- Food consumption rates in `calculateFoodMaintenance()`
- Population growth rates and food requirements