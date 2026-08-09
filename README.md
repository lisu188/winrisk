# WinRisk

WinRisk is being migrated to a single-process **C++20 + Qt 6 Qt Quick** application. The native client contains the game engine directly: there is no Spring Boot server, REST API, WebSocket transport, React frontend, browser, or WebView in the new runtime.

## Current Qt milestone

The `qt-cpp` implementation already contains:

- pure C++20 engine with no Qt dependency
- standard 42-territory world topology and continent bonuses
- deterministic xoshiro-family RNG with serialized state
- Classic mode for 2–5 players
- official two-player Classic setup with a non-turn-taking Neutral cluster: 14 territories and 40 initial armies per cluster
- Neutral excluded from starting-player rolls, turns and Classic victory counts while remaining attackable/defensible territory ownership
- Secret Mission mode for 3–5 players with the Java mission deck semantics
- Capital mode for 3–5 players with original-HQ victory semantics
- Java-compatible highest-die starting player and map-order claim setup for 3–5 player Classic/Capital games
- deterministic mission assignment excluding self-elimination objectives
- territory, fortified-territory, continent and elimination mission victory checks
- Capital HQ assignment, HQ-card exclusion, board markers and capture-all-HQs victory checks
- human and AI players
- reinforcement phase
- Risk dice combat, captures and elimination
- connected-territory maneuver phase
- deterministic 44-card Risk deck with Infantry, Cavalry, Artillery and Wild cards
- card awards after conquest, elimination card transfer and Java-compatible wildcard sets
- progressive card trade values, forced reinforcement trades and immediate elimination trades
- asynchronous one-action-at-a-time AI turns, including card trading and HQ-aware attack scoring
- Qt `QAbstractListModel` presentation layer
- responsive Qt Quick desktop/tablet/phone UI with mode selection, mission/objective HUD and HQ badges
- native quick-save/load using versioned JSON schema v4
- schema-v4 persistence of mode, missions, original headquarters and Neutral state
- migration of native schema-v2/v3 saves to v4
- import of current Java JSON saves for standard-map Classic games, including two-player Neutral ownership
- CTest engine tests that run in Debug and Release builds
- native CI targets for Windows, Linux, macOS, Android, iOS and WebAssembly

The legacy Java/Spring/React source is intentionally still present on this migration branch as a behavior and format reference. It is not linked into the C++ application and will be removed only after parity work is complete.

## Requirements

- CMake 3.24+
- C++20 compiler
- Qt 6.10+ with Core, Gui, Qml, Quick and QuickControls2
- Ninja recommended

Linux, macOS, Android, iOS and WebAssembly CI use Qt 6.11.1. Windows CI currently uses Qt 6.10.3 while the public Qt 6.11 Windows package tooling catches up; the application does not rely on 6.11-only APIs.

## Desktop build

```bash
cmake --preset default
cmake --build --preset default
ctest --preset default
```

Run the generated `winrisk_app` executable from the configured build directory.

## Android build

Configure with the Qt for Android toolchain and Android SDK/NDK paths, build the native target, then package it with `androiddeployqt`:

```bash
qt-cmake -S . -B build/android -GNinja \
  -DCMAKE_BUILD_TYPE=Release \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_SDK_ROOT="$ANDROID_SDK_ROOT" \
  -DANDROID_NDK_ROOT="$ANDROID_NDK_ROOT"
cmake --build build/android --target winrisk_app
"$QT_HOST_PATH/bin/androiddeployqt" \
  --input "$PWD/build/android/android-winrisk_app-deployment-settings.json" \
  --output "$PWD/build/android/android-build" \
  --apk "$PWD/WinRisk-android-arm64.apk"
```

The application is native Qt/C++; it does not need a WinRisk server or network connection.

## Architecture

```text
QML / Qt Quick
      |
AppController + BoardModel
      |
Pure C++ GameEngine
```

The engine is command-driven and does not expose mutable state to QML. UI taps call engine operations such as reinforcement, card trading, attack, maneuver and phase progression. AI uses the same engine operations and is scheduled asynchronously from Qt so the UI thread is never blocked by a nested event loop.

Game modes, missions, Capital headquarters and two-player Neutral ownership are pure C++ state and are serialized as explicit value types rather than QML state.

## Persistence

New saves use JSON schema version 4 and contain the game mode, stable player/territory/card IDs, Neutral flags, Secret Mission objectives, Capital headquarters, deck/discard state, trade progression and the complete RNG state. They do not serialize C++ object layouts or pointers.

Native schema-v2 and schema-v3 saves remain loadable as Classic games. Schema-v2 saves reconstruct the deterministic card system while preserving their stored gameplay RNG state.

The loader can also recognize the current Java Gson `GameSketch` JSON representation for Classic games on the standard 42-territory world map, including Java two-player Neutral saves. Java Capital/Secret Mission saves, historical/custom maps and remaining optional-rule state are deliberately rejected until those formats are migrated rather than silently loading them incorrectly. Java card hands are not yet imported from legacy saves.

## Remaining Java parity work

The C++ milestone is playable but does not yet cover every feature of the Java version. Remaining migration work includes:

- fog of war and remaining optional rules
- historical and procedural maps
- full Java save parity for Secret Mission/Capital, legacy cards and old Java ObjectStream saves
- richer mission-aware and mode-aware AI strategies
- replay/action log
- production signing/store packaging for Android and iOS

Once those parity gates pass, the legacy Java, Spring Boot, React, Gradle and libGDX migration code can be deleted.
