# Civilopedia Additions - Game Mechanics

## New Tutorial Sections Added

### 1. Unit Production and Population
**Location**: Added after "Workers" section in Tutorials.json

**Content Overview**:
- Explains conscription system mechanics
- Details how unit production affects city population
- Covers the 30-unit threshold before population loss
- Mentions population protection (minimum 1 citizen)

**Key Points Covered**:
- Each unit produced increases conscription counter by +1
- Population decreases by 1 when counter reaches 30
- Counter resets after population loss
- Cities cannot lose all population through conscription

### 2. Hero Supply System
**Location**: Added after "Unit Production and Population" section

**Content Overview**:
- Explains hero food consumption mechanics
- Details supply management interface
- Covers notification system for supply status

**Key Points Covered**:
- Heroes consume food during campaigns outside cities
- Manual and automatic supply options available in cities
- Notification alerts for full/low supply status
- Importance of supply management for extended campaigns

## Player Benefits

### Strategic Understanding
- Players now understand the population cost of large armies
- Clear explanation of hero logistics system
- Better strategic planning around resource management

### Accessibility
- New players can learn core mod mechanics through Civilopedia
- Reduces learning curve for complex systems
- Provides in-game reference for mechanics

## Integration Notes

### File Modified
- `android/assets/jsons/Tutorials.json`
- Added two new tutorial entries
- Maintains existing tutorial structure and formatting

### Language Support
- Written in English (player's preferred language)
- Uses clear, concise explanations
- Follows existing tutorial style and length

## Future Considerations

### Potential Expansions
- Could add more detailed balance parameters (conscription rates)
- Might include examples with specific unit counts
- Could expand hero mechanics coverage (morale, combat bonuses)

### Translation Support
- Tutorial text will be available for translation
- Follows standard Unciv internationalization patterns
- Ready for community translation contributions

## Testing Notes

### Compilation Status
- ✅ Successfully compiles with gradlew compileKotlin
- ✅ JSON syntax validated
- ✅ Maintains file structure integrity

### Accessibility
- Available in-game through Civilopedia
- Searchable within tutorial system
- Properly categorized with other game mechanics tutorials