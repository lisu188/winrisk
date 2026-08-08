# WinRisk

WinRisk is being migrated to a single-process **C++20 + Qt 6.11 Qt Quick** application. The native client contains the game engine directly: there is no Spring Boot server, REST API, WebSocket transport, React frontend, browser, or WebView in the new runtime.

## Current Qt milestone

The `qt-cpp` implementation already contains:

- pure C++20 engine with no Qt dependency
- standard 42-territory world topology and continent bonuses
- deterministic xoshiro-family RNG with serialized state
- 2–5 player setup
- human and AI players
- reinforcement phase
- Risk dice combat, captures and elimination
- connected-territory maneuver phase
- asynchronous one-action-at-a-time AI turns
- Qt `QAbstractListModel` presentation layer
- responsive Qt Quick desktop/tablet/phone UI
- native quick-save/load using versioned JSON
- import of current Java JSON saves for standard-map Classic games without neutral armies
- CTest engine tests
- Android-compatible Qt CMake target

The legacy Java/Spring/React source is intentionally still present on this migration branch as a behavior and format reference. It is not linked into the C++ application and will be removed only after parity work is complete.

## Requirements

- CMake 3.24+
- C++20 compiler
- Qt 6.11.x with Core, Gui, Qml, Quick and QuickControls2
- Ninja recommended

Qt 6.11 supports Windows, macOS, Linux, Android, iOS and WebAssembly. Android builds should use the NDK version supported by the installed Qt package.

## Desktop build

```bash
cmake --preset default
cmake --build --preset default
ctest --preset default
```

Run the generated `winrisk_app` executable from the configured build directory.

## Android build

Configure with the Qt for Android toolchain and Android SDK/NDK paths, then build the generated `apk` target:

```bash
qt-cmake -S . -B build/android -GNinja \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_SDK_ROOT="$ANDROID_SDK_ROOT" \
  -DANDROID_NDK_ROOT="$ANDROID_NDK_ROOT"
cmake --build build/android --target apk
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

The engine is command-driven and does not expose mutable state to QML. UI taps call engine operations such as reinforcement, attack, maneuver and phase progression. AI uses the same engine operations and is scheduled asynchronously from Qt so the UI thread is never blocked by a nested event loop.

## Persistence

New saves are versioned JSON and contain stable player/territory IDs plus the complete RNG state. They do not serialize C++ object layouts or pointers.

The loader can also recognize the current Java Gson `GameSketch` JSON representation for Classic games on the standard 42-territory world map. Capital, Secret Mission, two-player neutral-army saves, historical/custom maps, cards and remaining optional-rule state are deliberately rejected until those systems are migrated rather than silently loading them incorrectly.

## Remaining Java parity work

The C++ milestone is playable but does not yet cover every feature of the Java version. Remaining migration work includes:

- Risk cards and trade rules
- Secret Mission mode
- Capital mode
- official two-player neutral-army setup
- fog of war and remaining optional rules
- historical and procedural maps
- full Java save parity for those systems
- richer AI strategies
- replay/action log
- WebAssembly and iOS CI packaging

Once those parity gates pass, the legacy Java, Spring Boot, React, Gradle and libGDX migration code can be deleted.
