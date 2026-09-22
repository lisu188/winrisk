# WinRisk

WinRisk is a personal full-stack engineering project built around a turn-based territory-control game. It combines a **pure Java game engine**, a **Spring Boot** server and a **React + TypeScript** single-page client.

The project is primarily an exercise in preserving a non-trivial domain model while evolving the delivery architecture: the original desktop application was separated from its UI, made headless-capable, exposed through a concurrent web service, and connected to a real-time browser client.

## Engineering highlights

- Java game engine kept independent from the web layer
- Spring Boot REST API with explicit validation and error mapping
- multiple concurrent game sessions with per-game synchronization
- monotonically versioned game state to handle reordered real-time messages
- STOMP/WebSocket updates for live AI turns
- React + TypeScript SPA built into the same deployable Spring Boot JAR
- deterministic headless simulations for testing and experimentation
- save/load support and multiple built-in/procedural maps
- JUnit test suites, integration tests and a JaCoCo coverage gate
- CI that builds the backend and frontend and smoke-tests the packaged application

## Architecture

```text
React + TypeScript SPA
        |
        | REST commands
        | STOMP/WebSocket state updates
        v
Spring Boot web layer
        |
        | synchronized game sessions
        v
Pure Java game engine
        |
        +-- rules and turn flow
        +-- AI players
        +-- maps and missions
        +-- serialization
        +-- headless simulation
```

The browser sends commands through REST. State changes are broadcast over `/topic/games/{id}` through the STOMP endpoint at `/ws`. The client rejects stale versions, allowing broadcasts to occur outside the game-session lock without making message arrival order part of the consistency model.

## Build and run

Requirements:

- Java 17+
- Node.js 22+ for building/serving the web client

```bash
./gradlew build
java -jar build/libs/WinRisk-1.0-SNAPSHOT.jar
```

Open `http://localhost:8080`.

For frontend development:

```bash
./gradlew bootRun
cd frontend
npm install
npm run dev
```

The Vite development server runs on port `5173` and proxies the API to the backend on port `8080`.

Use `-PskipFrontend` when a backend-only JAR is sufficient.

## Headless simulation

The same application can run AI-only games without starting the server:

```bash
java -jar build/libs/WinRisk-1.0-SNAPSHOT.jar --headless-play \
    --map=pangaea --ai-players=4 --seed=42 --mode=classic
```

Supported options include built-in/custom maps, game modes, deterministic seeds, turn limits and optional rules.

## Gameplay scope

The implementation supports solo play against AI opponents, multiple game modes, saved games, custom maps and several optional rules. Built-in boards include a classic world layout plus Pangaea, Laurasia, Gondwana and Rodinia, along with procedurally generated maps.

## Repository structure

- `src/main/java/com/winrisk/game` — domain model, rules, AI, maps, missions and serialization
- `src/main/java/com/winrisk/web` — Spring Boot API, sessions, WebSocket broadcasting and AI scheduling
- `frontend/` — React + TypeScript SPA built with Vite
- `src/test/java` — engine, service, controller and integration tests

## Quality gates

`./gradlew build` runs the automated test suite and JaCoCo verification. The CI pipeline also packages the SPA into the boot JAR, starts the resulting artifact and smoke-tests both the API and served frontend.

## Project status

Active personal project. The current web architecture replaced an earlier Swing client while retaining the underlying engine and save compatibility. The repository includes historical evolution in Git so architectural changes can be inspected rather than presented only as a finished snapshot.

## Naming

This is an unofficial personal software-engineering project and is not presented as an official implementation or product of any commercial board-game publisher.
