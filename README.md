# WinRisk

WinRisk is a Java implementation of the classic world-domination board game built with **libGDX**. The game engine, AI and UI run in the same process on desktop and Android. There is no Spring Boot server, REST API, WebSocket layer, React frontend or WebView in the runtime.

## Requirements

- Java 17 or newer
- Android SDK 36 for Android builds

## Desktop

```bash
./gradlew run
```

The desktop launcher uses libGDX's LWJGL3 backend.

## Android

```bash
./gradlew :android:assembleDebug
```

The APK is produced at:

```text
android/build/outputs/apk/debug/android-debug.apk
```

The Android application is fully local and does not require a WinRisk server or network connection.

## Build and test

```bash
./gradlew build
./gradlew test
```

`build` runs the engine tests, compiles the desktop client and builds the Android debug APK.

## Playing

- **REINFORCE**: tap your territory to place one army. Enable `Place all` to place all available reinforcements at once.
- **ATTACK**: tap one of your territories with at least two armies, then tap an adjacent enemy territory.
- **MOVE**: tap a source territory and then a connected owned territory. Each destination tap moves one army.
- **Trade cards**: during reinforcement, the button trades the first legal set.
- **End phase**: advances the game. AI turns are stepped visibly by the client.
- **Save / Load**: uses a local `saves/quick.save.json` save.

The rules engine still supports Classic, Secret Mission and Capital modes, the built-in historical maps, random maps and the existing optional rules. The initial libGDX setup screen currently starts a Classic World game with one human and three AI players.

## Architecture

- `core/` – libGDX application, Scene2D UI and board renderer
- `lwjgl3/` – desktop launcher
- `android/` – Android launcher and APK packaging
- `src/main/java/com/winrisk/game/` – shared rules engine, AI, maps, missions and serialization
- `src/main/resources/` – built-in map resources
- `src/test/java/com/winrisk/game/` – engine tests

The previous Spring Boot server under `src/main/java/com/winrisk/web/` and React frontend under `frontend/` are retained only as migration history. They are excluded from the active Gradle build.

## Platform portability

The game engine no longer depends on `java.awt.Color`. Player and continent colors use a small platform-neutral ARGB value type, allowing the same engine to run directly on Android. JSON saves continue to store the same ARGB integer representation used previously.

## Current UI scope

The libGDX client provides the complete turn loop needed for local play: reinforcement, attacks, maneuvering, card trading, AI turns, game-over detection and local save/load. It currently renders the board from map coordinates and connections rather than reproducing every visual detail of the former React client. Further UI work can be done entirely in the shared libGDX `core` module and automatically applies to both desktop and Android.
