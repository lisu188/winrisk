# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/winrisk/game` holds the UI-free game engine: rules (`rules/`), AIs (`ai/`), boards (`map/`), missions, serialization and the headless CLI (`HeadlessCli`).
- `src/main/java/com/winrisk/web` is the Spring Boot layer: `game/` (GameSession/GameService/AiStepper/GameViewMapper/SaveStore), `api/` (controller + DTOs), `config/` (WebSocket, scheduler).
- `frontend/` is the React SPA (Vite + TypeScript). It builds into the boot jar via the Gradle node plugin; `src/api/types.ts` mirrors the server DTOs and must stay in sync with `GameView`.
- `src/main/resources` stores the bundled `world.map` (load it via streams — file paths break inside the jar) and `application.properties`.
- `src/test/java` hosts JUnit 4 engine suites (run via the vintage engine) and JUnit 5 web suites.

## Build, Test, and Development Commands
- `./gradlew build` compiles, runs all tests, enforces the JaCoCo line-coverage gate and produces the boot jar with the SPA. Use `-PskipFrontend` to skip the npm build (test runs never need Node).
- `java -jar build/libs/WinRisk-*.jar` serves the game at :8080; `--headless-play` runs an AI simulation instead.
- `./gradlew bootRun` + `cd frontend && npm run dev` for UI development with the Vite proxy.
- Run `./gradlew build` before every push.

## Coding Style & Naming Conventions
- Java 17, 4-space indentation, brace-on-same-line, descriptive names mirroring gameplay concepts.
- The engine must stay UI- and web-free: no Spring or servlet imports under `com.winrisk.game`, no engine access outside a `GameSession` lock in the web layer.
- Never expose `GameSketch`/`MapSketch` over the API (they carry the background image and deck order); extend `GameView` instead, and update `frontend/src/api/types.ts` in the same change.
- Frontend: strict TypeScript, React function components with hooks, no new runtime dependencies without justification.

## Testing Guidelines
- Engine tests are JUnit 4 (`<Feature>Tests`), web tests JUnit 5; both run in one `test` task.
- Use seeded `Params` for anything that plays turns — unseeded AI matchups can stalemate forever.
- Keep line coverage well above the 70% gate; land new web code with its tests in the same commit.
- The WebSocket integration test (`GameFlowIntegrationTests`) is the only random-port test; keep `winrisk.ai-step-millis=1` in test properties.

## Commit & Pull Request Guidelines
- Imperative commit style with optional issue references, e.g. `Add trade endpoint validation (#22)`.
- PRs explain motivation, list verification steps, and attach screenshots for UI-affecting changes.
- Preserve save compatibility (`saves/*.save.json`, `.map` files); ship migrations and tests when a schema changes.

## Security & Configuration Tips
- Never commit secrets. `build.gradle` is the canonical record of dependencies and coverage thresholds.
- Saves and custom maps resolve strictly inside the configured `saves/` and `maps/` directories (name validation + containment checks in `SaveStore`) — keep it that way, and never add an endpoint that deserializes uploaded files (`JavaSerializer` has a Java-deserialization fallback).
- Keep this document aligned with `README.md` and the CI workflow.
