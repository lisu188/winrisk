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
- all five Java AI strategies: Easy, Continent, Balanced, BorderGuard and Random
- Java-compatible AI strategy assignment by absolute seat index and persistence of strategy identity in native saves
- Java `Field.getScale()` behavior: graph-distance decay, owner-relative fog visibility and Skynet interactive-player weighting
- Java AI card-trade behavior: automatic trades are forced-only unless an explicit human trade action is made
- Java-compatible Skynet semantics, including the original two-player Neutral quirk described below
- viewer-aware fog-of-war with hidden owner/army/HQ data redacted before QML receives it
- asynchronous AI scheduling with one Java-equivalent strategy phase per Qt timer tick
- Qt `QAbstractListModel` presentation layer
- responsive Qt Quick desktop/tablet/phone v6 UI with mode/rule selection, mission/objective HUD and HQ badges
- native quick-save/load using versioned JSON schema v6
- migration of native schema-v2/v3/v4/v5 saves to v6
- current Java Gson `GameSketch` import for Classic, Secret Mission and Capital on the standard world map
- CTest engine, AI-strategy, fog-model and Java-import regression suites
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

## Java AI parity

The native engine preserves the five AI controllers exposed by Java `PlayerFactory`:

- **EasyAI:** reinforces the weakest scale position, repeatedly attacks sufficiently weaker adjacent targets, and moves armies from the strongest position toward the weakest border. Under Skynet it restricts a source's candidates to Java-"interactive" adjacent enemies when any exist.
- **ContinentAI:** keeps a continent goal until that continent is owned, chooses the unowned continent with the greatest current player share, attacks in average-proximity-to-goal order and reinforces owned goal fields. Its Java behavior can leave reinforcement points unused and still advance phase; the native parity implementation intentionally preserves that quirk.
- **BalancedAI:** concentrates reinforcement on the weakest border, makes one pass over scale-sorted border attacks using the Java strength threshold, then moves interior armies toward the weakest border.
- **BorderGuardAI:** concentrates reinforcement on the weakest border, attacks only when a border has more than twice the target's armies, and moves interior armies toward weak borders.
- **RandomAI:** distributes reinforcements randomly, makes one random border attack and attempts one random maneuver.

The common Java scale function is also reproduced. For a territory, armies are weighted by `1 / 2^graphDistance`; friendly and enemy weights are accumulated to produce `enemy / friendly`. With fog enabled, Java evaluates only the **owner of that territory's** visible fields (owned fields plus immediate neighbors), not a single global viewer.

Java's Skynet implementation has a non-obvious two-player quirk: Neutral is controlled by the unannotated `PlayerAI`, and Java `PlayerInterface.isInteractive()` therefore returns `true` for Neutral. Consequently EasyAI's Skynet target filter and `Field.getScale()` both treat Neutral as interactive alongside human players. The native compatibility implementation preserves this behavior rather than silently normalizing it.

`PlayerFactory.getDefaultAI(index)` uses the absolute player-seat index modulo the five AI strategies. The C++ setup uses the same rule. For example, with one human in seat 0, AI seats 1–4 are Continent, Balanced, BorderGuard and Random respectively.

AI strategy identity is persisted in native schema-v6 saves and reconstructed from Java `GameSketch.PlayerData.interfaceClass` when importing current Java saves. ContinentAI's goal itself is transient, matching Java's non-serialized AI-object state, so it is recalculated after load.

## Optional rules

The v6 engine currently preserves these recovered Java semantics:

- **Incremental cards:** trade value is `4 + completed trades`.
- **Expanded maneuver:** multiple friendly connected routes may be used in one maneuver phase.
- **Attack-card reroll:** a matching non-wild source/target card may improve the lowest attack die; the card is not consumed.
- **Commander die:** once per turn the lowest attack die becomes 6 after the card reroll.
- **Attack with all:** repeat battles until capture or until the source cannot continue attacking.
- **Fog of war:** a human viewer sees owned territories and immediate neighbors; hidden ownership, armies and HQ state are redacted in the Qt model. AI scale calculations separately preserve Java's owner-relative visibility behavior.
- **Skynet:** EasyAI prefers adjacent Java-interactive targets, and scale calculations double the weight of Java-interactive fields. In the Java implementation this includes human players and the two-player Neutral controller.

## Persistence

Native saves use JSON schema version 6 and contain the game mode, seven rule flags, stable player/territory/card IDs, AI strategy identity, Secret Mission objectives, Capital headquarters, deck/discard state, trade progression, commander-die state, maneuver state and complete native RNG state. They do not serialize C++ object layouts or pointers.

Native schema-v2 through schema-v5 saves remain loadable. Pre-v6 saves explicitly disable fog and Skynet because those fields did not exist in those schemas. Early native schema-v6 saves that predate `aiStrategy` remain readable and default a missing strategy field to Easy.

### Current Java JSON import

The loader imports the current Java Gson `GameSketch` format when the save uses the standard 42-territory world map. `GameSketch.ParamsData` stores the four `RulesOptions` booleans **flattened** alongside the other parameters; the converter imports those values plus `attackWithAll`, `fogOfWar` and `skynetMode`.

For current Java JSON saves the converter preserves:

- Classic, Secret Mission and Capital mode
- two-player Classic Neutral ownership
- player colors, human/AI classification, exact supported AI strategy class and reinforcements
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
- signed 64-bit `randomSeed` text without routing it through JSON-double precision

Unsupported active Java AI controller classes are rejected rather than silently substituted with a different strategy. Neutral's Java `PlayerAI` controller remains supported through the dedicated Neutral path.

The standard-world Java card symbols map directly to the native deck because both use Infantry/Cavalry/Artillery by territory index modulo 3.

A Java save does **not** contain the progressed internal state of `java.util.Random`; Java's own `Game.fromSketch()` recreates `Random` from the original `randomSeed`. The converter therefore preserves the serialized game state and derives the native RNG from the stored seed when available, but does not claim that future native dice rolls are bit-for-bit identical to Java's RNG stream.

Java also stores `maneuverUsed` without its source/destination route. The native engine represents that imported state as a consumed-fortification sentinel, matching Java's post-load behavior: no new standard fortification route may be started that turn.

## Remaining Java parity work

Remaining migration work is now concentrated in:

- historical, custom and procedural maps
- old Java ObjectStream save/map conversion
- replay/action log
- production signing/store packaging for Android and iOS

Once those parity gates pass, the legacy Java, Spring Boot, React, Gradle and abandoned libGDX migration code can be deleted.
