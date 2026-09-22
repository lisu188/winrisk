# WinRisk Android client

This module is a small Android WebView client for the existing WinRisk web application. The game engine and Spring Boot server stay unchanged; the APK connects to a running WinRisk server and renders the existing React/canvas UI.

## Requirements

- JDK 17
- Android SDK 36
- Gradle 8.13

## Run the WinRisk server

From the repository root:

```bash
./gradlew bootRun --args='--server.address=0.0.0.0'
```

For the Android emulator use:

```text
http://10.0.2.2:8080
```

For a physical Android device use the development computer's LAN address, for example:

```text
http://192.168.1.10:8080
```

The phone and server must be reachable from each other and the host firewall must allow TCP port 8080.

For anything outside a trusted LAN, expose WinRisk through HTTPS rather than plain HTTP.

## Build

```bash
gradle -p android :app:assembleDebug
```

The APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Client behavior

- The first launch asks for the WinRisk server URL.
- The selected URL is persisted.
- The action-bar `Server` item changes the endpoint.
- `Reload` refreshes the current WinRisk page.
- HTTP is permitted so development servers on a LAN work; HTTPS is preferred for non-local deployments.
- Navigation outside the configured server origin is handed to the system browser.
- File and content access are disabled in the WebView.
