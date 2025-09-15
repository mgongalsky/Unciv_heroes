# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Героические цивилизации (Heroic Civilizations) is a fork of the open-source Unciv game, designed as a hybrid between Civilization and Heroes of Might and Magic games. It's written in Kotlin using LibGDX framework for cross-platform gaming (Android, Desktop, iOS). The game aims to combine strategic depth from Civilization games with tactical combat and adventure elements from Heroes games.

This is a Russian-language project with the main goal of merging the economic depth of Civilization with the tactical combat and adventure map exploration of Heroes games.

## Key Development Commands

### Build and Run Commands

```bash
# Build the desktop version and run it
./gradlew desktop:run

# Build and run in debug mode
./gradlew desktop:debug

# Build JAR distribution (desktop)
./gradlew desktop:dist

# Build Android APK
./gradlew android:assembleDebug
./gradlew android:assembleRelease

# Run tests
./gradlew tests:test

# Run a specific test class
./gradlew tests:test --tests "ClassName"

# Build all platforms
./gradlew build
```

### Android Development

```bash
# Install and launch app on connected Android device
./gradlew android:run

# Copy Android native libraries (required before packaging)
./gradlew android:copyAndroidNatives

# Texture packing for Android
./gradlew android:texturePacker
```

### Package Distribution

```bash
# Create platform-specific packages
./gradlew desktop:packrLinux64
./gradlew desktop:packrWindows64
./gradlew desktop:packrMacOS

# Create zip distributions
./gradlew desktop:zipLinux64
./gradlew desktop:zipWindows64
./gradlew desktop:zipMacOS
```

## Code Architecture

### Module Structure

The project is organized into several Gradle modules:

- **`core`** - Main game logic, UI, and business logic (platform-agnostic)
- **`desktop`** - Desktop launcher and platform-specific code (LWJGL3)
- **`android`** - Android launcher and platform-specific code
- **`ios`** - iOS launcher (RoboVM)
- **`server`** - Server-side multiplayer functionality (Ktor)
- **`tests`** - Unit tests and game logic tests
- **`buildSrc`** - Build configuration and custom Gradle tasks
- **`devtools`** - Development tools and utilities

### Core Architecture (`core` module)

The core game logic is organized under `com.unciv`:

#### Game Flow Architecture
- **`UncivGame`** - Main game class, handles screen management and game lifecycle
- **`GameInfo`** - Central game state container with all civilizations, map, and game parameters
- **`GameStarter`** - Handles new game creation and initialization
- **`MainMenuScreen`** - Entry point for the user interface

#### Key Logic Systems
- **`logic/civilization/`** - Civilization management, AI players, diplomacy
- **`logic/battle/`** - Combat system, battle mechanics, damage calculation
- **`logic/army/`** - Army management system (custom addition for heroes mechanics)
- **`logic/city/`** - City management, buildings, population
- **`logic/map/`** - Tile system, map generation, terrain features
- **`logic/automation/`** - AI automation for cities, units, and civilizations

#### UI Architecture
- **`ui/worldscreen/`** - Main game world view and UI components
- **`ui/utils/`** - Common UI utilities and base screen classes
- **`ui/popup/`** - Dialog and popup management
- **BaseScreen** - Base class for all game screens with common functionality

#### Data Models
- **`models/ruleset/`** - Game rules, technologies, units, buildings
- **`models/metadata/`** - Game settings, parameters, save data
- **`models/stats/`** - Statistics tracking and calculation

### Key Game Mechanics

This project extends base Unciv with heroes-style mechanics:

1. **Supply System** - Armies require food and provisions from cities
2. **Settlement Mechanics** - Heroes can found cities but are consumed in the process  
3. **Army Management** - Enhanced unit groups with hero leadership
4. **Tactical Combat** - Zone of control and advanced battle mechanics

### Build System

- **Gradle 7.3.1** with Kotlin DSL
- **Kotlin 1.7.21** as primary language
- **LibGDX 1.11.0** for cross-platform game framework
- **Android API 32** with minimum SDK 21
- **RoboVM 2.3.1** for iOS builds

### Important Configuration Files

- **`BuildConfig.kt`** - Version numbers, build constants
- **`gradle.properties`** - Gradle build settings and optimizations
- **`settings.gradle.kts`** - Multi-module project structure
- **Android keystores** - Debug and release signing keys included

### Development Workflow

The game supports hot-reload development through LibGDX's asset management. Most game content is data-driven through JSON rulesets in the `android/assets` folder.

### Testing Strategy

Tests are located in the `tests` module and use JUnit 4 with Mockito. The test environment uses LibGDX's headless backend for UI-free testing of game logic.

### Multiplayer Architecture

The game supports online multiplayer through:
- **Dropbox** integration for save file synchronization
- **Custom server** option (`server` module with Ktor)
- **UncivXYZ server** as default multiplayer backend

### Asset Pipeline

- **Texture packing** - Images are automatically packed into texture atlases
- **I18n support** - Full internationalization with Russian as primary language
- **Moddable content** - Game rules, units, and civilizations are data-driven