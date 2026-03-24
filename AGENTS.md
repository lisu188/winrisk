# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java` contains production sources for both game logic and Swing UI components, e.g., `com.winrisk.game.Play` and `com.winrisk.gui.GamePanel`.
- `src/main/resources` stores maps, sprites, and serialized defaults; keep filenames stable because save data references these assets.
- `src/test/java` hosts JUnit 4 suites such as `com/winrisk/gui/StartGameLaunchTests.java` alongside helpers in `TestUtil`.
- `img/` holds documentation screenshots, while build logic lives in `build.gradle` and the Gradle wrapper under `gradle/`.

## Build, Test, and Development Commands
- `./gradlew build` compiles, runs all tests, and enforces the JaCoCo coverage gate.
- `./gradlew test jacocoTestReport` regenerates `build/reports/jacoco/test/html/index.html` for local coverage review.
- `./gradlew run` launches the Swing client with the default configuration; pass `-Djava.awt.headless=false` when debugging UI locally.
- `./gradlew distZip` packages a distributable in `build/distributions/WinRisk-*.zip`; unzip and run `bin/WinRisk`.

## Coding Style & Naming Conventions
- Target Java 17, use 4-space indentation, brace-on-same-line formatting, and descriptive class names that mirror gameplay concepts (`StartGame`, `HostGameWindow`).
- Keep packages under `com.winrisk.game` or `com.winrisk.gui`, and align new resources with parallel folders in `src/main/resources`.
- Favor immutability, reuse helpers like `GameSurface`, and avoid adding dependencies unless justified in `build.gradle`.
- UI labels, map identifiers, and other player-facing strings should remain in resource files to preserve localization and save compatibility.

## Testing Guidelines
- Tests rely on JUnit 4 (`org.junit.Test`, `org.junit.Assert.*`) with suites named `<Feature>Tests`, e.g., `PlayTests`.
- Use `TestUtil` for shared fixtures, and stub Swing interactions by forcing `System.setProperty("java.awt.headless","true")` as shown in `GuiTests`.
- Maintain ≥80% line coverage even though the JaCoCo gate is 70%; document coverage gaps when unavoidable.
- Run `./gradlew build` or, at minimum, `./gradlew test jacocoTestReport` before every push.

## Commit & Pull Request Guidelines
- Follow the existing imperative commit style with optional issue references, e.g., `Refactor StartGame launch flow (#22)`.
- Pull requests should explain the motivation, list verification steps, and attach screenshots for GUI-affecting changes.
- Note resource or serialization updates in `README.md` or inline comments, and mention any follow-up tasks in the PR description.

## Security & Configuration Tips
- Never commit secrets or API tokens; treat `build.gradle` as the canonical record of dependencies and coverage thresholds.
- Preserve map/save compatibility in `src/main/resources`; ship migrations and tests whenever the schema changes.
- Keep this document aligned with `README.md` and CI workflows so contributors have a single source of guidance.
