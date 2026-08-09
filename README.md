# WinRisk

WinRisk is being migrated to a single-process **C++20 + Qt 6 Qt Quick** application. The native client contains the game engine directly: there is no Spring Boot server, REST API, WebSocket transport, React frontend, browser, or WebView in the new runtime.

## Current Qt milestone

The `qt-cpp` implementation already contains:

- pure C++20 engine with no Qt dependency
- standard 42-territory / 83-edge world topology and continent bonuses
- deterministic xoshiro-family RNG with serialized state
- Classic mode for 2–5 real players
- official two-player Classic Neutral setup with 14 territories and 40 initial armies per real/neutral cluster
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
- Java optional rules: incremental card values, expanded maneuver, attack-card reroll, commander die, attack-with-all, fog-of-war and Skynet
- Java-compatible Skynet targeting: an AI source with an adjacent human enemy targets a human rather than an AI/neutral target from that source
- viewer-aware fog-of-war with hidden owner/army/HQ data redacted before QML receives it
- asynchronous one-action-at-a-time AI turns, including card trading and HQ-aware attack scoring
- Qt `QAbstractListModel` presentation layer
- responsive Qt Quick desktop/tablet/phone v6 UI with mode/rule selection, mission/objective HUD and HQ badges
- native quick-save/load using versioned JSON schema v6
- schema-v6 persistence of modes, rules, missions, original headquarters, maneuver lock, card state and RNG state
- migration of native schema-v2/v3/v4/v5 saves to v6
- import of current Java JSON Classic/world saves including Neutral, fog and supported rule flags
- CTest engine and fog-model regression tests
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

The root CMake project now builds the authoritative v6 engine/controller/QML shell. The standalone `v6/` project builds the same source set:

```bash
cmake -S v6 -B build/v6 -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=ON
cmake --build build/v6 --parallel
ctest --test-dir build/v6 --output-on-failure
```

## Android build

Configure with the Qt for Android toolchain and Android SDK/NDK paths, build the native target, then package it with `androiddeployqt`:

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

Game modes, rules, missions and Capital headquarters are pure C++ state and are serialized as explicit value types rather than QML state.

## Optional rules

The v6 engine preserves the Java rule semantics currently recovered from the reference implementation:

- **Incremental cards:** trade value is `4 + completed trades`.
- **Expanded maneuver:** multiple friendly connected routes may be used in one maneuver phase.
- **Attack-card reroll:** a matching non-wild source/target card may improve the lowest attack die; the card is not consumed.
- **Commander die:** once per turn the lowest attack die becomes 6 after the card reroll.
- **Attack with all:** repeat battles until capture or until the source cannot continue attacking.
- **Fog of war:** a human viewer sees owned territories and every territory immediately adjacent to an owned territory; hidden ownership, armies and HQ state are redacted in the Qt model.
- **Skynet:** when an AI attacking territory has at least one adjacent human enemy, its target candidates for that source are restricted to humans. This mirrors the Java `EasyAI` Skynet branch rather than introducing a new AI class.

## Persistence

New saves use JSON schema version 6 and contain the game mode, seven rule flags, stable player/territory/card IDs, Secret Mission objectives, Capital headquarters, deck/discard state, trade progression, commander-die state, maneuver-route lock and complete RNG state. They do not serialize C++ object layouts or pointers.

Native schema-v2 through schema-v5 saves remain loadable. Pre-v6 saves explicitly disable fog and Skynet because those fields did not exist in those native schemas.

The loader can recognize the current Java Gson `GameSketch` representation for Classic games on the standard 42-territory world map. It imports Java top-level `attackWithAll`, `fogOfWar` and `skynetMode`, plus the nested `rulesOptions` flags. Java Classic imports currently reconstruct a usable standard deck deterministically; exact historical Java player hands/draw/discard ordering is still pending rather than being claimed as lossless.

## Remaining Java parity work

The C++ milestone is playable but does not yet cover every feature of the Java version. Remaining migration work includes:

- richer strategy-specific AI parity beyond the recovered Skynet target preference
- historical and procedural maps
- exact Java save conversion for Secret Mission, Capital and legacy card hands/draw/discard state
- old Java ObjectStream save/map conversion
- replay/action log
- production signing/store packaging for Android and iOS

Once those parity gates pass, the legacy Java, Spring Boot, React, Gradle and abandoned libGDX migration code can be deleted.
