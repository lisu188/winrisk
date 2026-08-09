# WinRisk

WinRisk is being migrated to a single-process **C++20 + Qt 6 Qt Quick** application. The native client contains the game engine directly: there is no Spring Boot server, REST API, WebSocket transport, React frontend, browser, or WebView in the new runtime.

## Current Qt milestone

The `qt-cpp` implementation already contains:

- pure C++20 engine with no Qt dependency
- standard 42-territory / 83-edge world topology and continent bonuses
- deterministic xoshiro-family native RNG with serialized state
- Classic mode for 2–5 real players
- official two-player Classic Neutral setup with 14 territories and 40 initial armies per real/neutral cluster
- Secret Mission mode for 3–5 players with Java mission semantics
- Capital mode for 3–5 players with original-HQ victory semantics
- Java-compatible highest-die starting player and map-order claim setup for 3–5 player Classic/Capital games
- territory, fortified-territory, continent and elimination mission victory checks
- Capital HQ assignment, HQ-card exclusion, board markers and capture-all-HQs victory checks
- reinforcement, Risk dice combat, capture/elimination and connected fortification
- deterministic 44-card Risk deck, conquest awards, elimination transfer and Java-compatible wildcard sets
- progressive card trades, forced trades and immediate post-elimination trades
- all seven recovered Java options: incremental cards, expanded maneuver, attack-card reroll, commander die, attack-with-all, fog-of-war and Skynet
- Java-compatible Skynet target filtering: an AI source with an adjacent human enemy targets a human rather than an AI/Neutral target from that source
- viewer-aware fog-of-war with hidden owner/army/HQ data redacted before QML receives it
- asynchronous one-action-at-a-time AI turns
- Qt `QAbstractListModel` presentation layer
- responsive Qt Quick desktop/tablet/phone v6 UI with mode/rule selection, mission/objective HUD and HQ badges
- native quick-save/load using versioned JSON schema v6
- migration of native schema-v2/v3/v4/v5 saves to v6
- current Java Gson `GameSketch` import for Classic, Secret Mission and Capital on the standard world map
- CTest engine, fog-model and Java-import regression suites
- CI definitions for Windows, Linux, macOS, Android, iOS and WebAssembly

The legacy Java/Spring/React source remains on this migration branch only as a behavior and format reference. It is not linked into the C++ application and will be removed after the remaining parity gates pass.

## Requirements

- CMake 3.24+
- C++20 compiler
- Qt 6.10+ with Core, Gui, Qml, Quick and QuickControls2
- Ninja recommended

Linux, macOS, Android, iOS and WebAssembly CI use Qt 6.11.1. Windows currently targets Qt 6.10.3; the application does not depend on 6.11-only APIs.

## Desktop build

```bash
cmake --preset default
cmake --build --preset default
ctest --preset default
```

The root CMake project builds the authoritative v6 engine/controller/QML shell. The standalone `v6/` project builds the same source set:

```bash
cmake -S v6 -B build/v6 -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=ON
cmake --build build/v6 --parallel
ctest --test-dir build/v6 --output-on-failure
```

## Android build

```bash
qt-cmake -S v6 -B build/android -GNinja \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_TESTING=OFF \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_SDK_ROOT="$ANDROID_SDK_ROOT" \
  -DANDROID_NDK_ROOT="$ANDROID_NDK_ROOT"
cmake --build build/android --target winrisk_app
"$QT_HOST_PATH/bin/androiddeployqt" \
  --input "$PWD/build/android/android-winrisk_app-deployment-settings.json" \
  --output "$PWD/build/android/android-build" \
  --apk "$PWD/WinRisk-android-arm64.apk"
```

The application is native Qt/C++; it does not require a WinRisk server or network connection.

## Architecture

```text
QML / Qt Quick
      |
AppController + BoardModel
      |
Pure C++ GameEngine
```

The engine is command-driven and does not expose mutable state to QML. UI actions call engine operations for reinforcement, cards, attack, maneuver and phase progression. AI uses the same operations and is scheduled asynchronously from Qt.

## Optional rules

The v6 engine currently preserves these recovered Java semantics:

- **Incremental cards:** trade value is `4 + completed trades`.
- **Expanded maneuver:** multiple friendly connected routes may be used in one maneuver phase.
- **Attack-card reroll:** a matching non-wild source/target card may improve the lowest attack die; the card is not consumed.
- **Commander die:** once per turn the lowest attack die becomes 6 after the card reroll.
- **Attack with all:** repeat battles until capture or until the source cannot continue attacking.
- **Fog of war:** a human viewer sees owned territories and immediate neighbors; hidden ownership, armies and HQ state are redacted in the Qt model.
- **Skynet:** when an AI attack source has at least one adjacent human enemy, targets from that source are restricted to humans. This preserves the explicit Java `EasyAI` Skynet branch. Full strategy-specific AI parity is separate work because the Java strategy classes also use scale/scoring logic not yet reproduced by the native AI.

## Persistence

Native saves use JSON schema version 6 and contain the game mode, seven rule flags, stable player/territory/card IDs, Secret Mission objectives, Capital headquarters, deck/discard state, trade progression, commander-die state, maneuver state and complete native RNG state. They do not serialize C++ object layouts or pointers.

Native schema-v2 through schema-v5 saves remain loadable. Pre-v6 saves explicitly disable fog and Skynet because those fields did not exist in those schemas.

### Current Java JSON import

The loader imports the current Java Gson `GameSketch` format when the save uses the standard 42-territory world map. `GameSketch.ParamsData` stores the four `RulesOptions` booleans **flattened** alongside the other parameters; the converter imports those values plus `attackWithAll`, `fogOfWar` and `skynetMode`.

For current Java JSON saves the converter preserves:

- Classic, Secret Mission and Capital mode
- two-player Classic Neutral ownership
- player colors, human/AI classification and reinforcements
- territory owners and armies
- Secret Mission specifications and elimination targets
- Capital headquarters
- all seven rule flags
- commander-die and maneuver-used state
- trade count and conquest-card state for the current player
- player card hands
- draw pile and discard pile
- Java draw order (`GameSketch.drawPile` is next-card-first and is reversed for the native back-of-vector draw convention)
- the two wild cards as distinct native card IDs

The standard-world Java card symbols map directly to the native deck because both use Infantry/Cavalry/Artillery by territory index modulo 3.

A Java save does **not** contain the progressed internal state of `java.util.Random`; Java's own `Game.fromSketch()` recreates `Random` from the original `randomSeed`. The converter therefore preserves the serialized game state and derives the native RNG from the stored seed when available, but does not claim that future native dice rolls are bit-for-bit identical to Java's RNG stream.

Java also stores `maneuverUsed` without its source/destination route. The native engine represents that imported state as a consumed-fortification sentinel, matching Java's post-load behavior: no new standard fortification route may be started that turn.

## Remaining Java parity work

Remaining migration work is now concentrated in:

- richer strategy-specific AI parity beyond the explicit Skynet target rule
- historical, custom and procedural maps
- old Java ObjectStream save/map conversion
- replay/action log
- production signing/store packaging for Android and iOS

Once those parity gates pass, the legacy Java, Spring Boot, React, Gradle and abandoned libGDX migration code can be deleted.
