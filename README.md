# WinRisk

WinRisk is a web implementation of the classic world domination board game:
a **Spring Boot** server wrapping a pure-Java game engine, with a **React**
single-page client rendered on canvas. Play solo against 1–4 AI opponents;
the server hosts many concurrent games.

## Prerequisites
- Java 17 or newer
- Node.js 22 or newer (only for building/serving the web client)

## Build and run

```bash
./gradlew build                 # engine + web tests, coverage gate, boot jar with the SPA
java -jar build/libs/WinRisk-1.0-SNAPSHOT.jar
```

Open http://localhost:8080 — create a game in the lobby and play. Add
`--server.port=<port>` to change the port, or `-PskipFrontend` to the build to
produce a jar without the web client.

For frontend development run the API and the Vite dev server side by side:

```bash
./gradlew bootRun               # API on :8080
cd frontend && npm install && npm run dev    # UI on :5173, proxied to :8080
```

## Playing
- **REINFORCE**: click your territory to place one troop ("place all" toggle
  places everything); trade card sets from the panel when you hold three or
  more (forced at five).
- **ATTACK**: click your territory, then an adjacent enemy; dice results pop up
  as toasts and conquests resolve automatically.
- **MOVE**: click a source, then a connected territory, choose the troop count.
- End Phase hands over; AI turns animate live over a WebSocket.

Game modes: World Domination (classic), Secret Mission, Capital. Optional
rules: attack-with-all, fog of war, skynet, incremental card values, expanded
maneuver, attack card reroll, commander die.

## Maps
Built-in boards: the classic 42-territory **World** map plus four historical
supercontinents — **Pangaea**, **Laurasia**, **Gondwana**, **Rodinia** — and
procedurally generated random maps. Custom `.map` files placed in a `maps/`
directory next to the server appear in the lobby's map list.

## Saving
Games can be saved from the HUD and resumed from the lobby. Saves are JSON
files in a `saves/` directory next to the server; the format is unchanged from
earlier releases, so old saves still load.

## Headless simulation
The same jar runs AI-only simulations without starting the server:

```bash
java -jar build/libs/WinRisk-1.0-SNAPSHOT.jar --headless-play \
    --map=pangaea --ai-players=4 --seed=42 --mode=classic
```

Flags: `--map=<builtin or path>`, `--mode=<classic|secret|capital>`,
`--ai-players=<1..5>`, `--seed=<long>`, `--max-turns=<n>`, plus the rule
toggles (`--fog-of-war`, `--skynet`, `--attack-with-all`,
`--incremental-cards`, `--expanded-maneuver`, `--attack-card-reroll`,
`--commander-die`).

## REST API
The client speaks a small JSON API under `/api` (games CRUD, place / attack /
maneuver / end-phase / trade actions, saves, maps) and receives live state
frames on the STOMP WebSocket topic `/topic/games/{id}` via `/ws`. See
`com.winrisk.web.api.GameController` for the full surface.

## Repository structure
- `src/main/java/com/winrisk/game` – the game engine (rules, AI, maps, missions, serialization)
- `src/main/java/com/winrisk/web` – Spring Boot server: sessions, REST API, WebSocket push, AI stepper
- `frontend/` – React SPA (Vite + TypeScript), built into the boot jar
- `src/test/java` – JUnit suites for the engine and the web layer

## Notes
- The former Swing desktop client was removed in the web conversion (it lives
  in git history); the in-game map editor went with it. Custom maps can still
  be loaded from files.
- Interactive dice/occupation prompts and human setup placement use sensible
  defaults (max dice, max occupation, automatic setup); making them
  interactive over the WebSocket is a planned follow-up.

A coverage report is generated under `build/reports/jacoco`; the build fails
below the configured threshold.
