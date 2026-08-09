# WinRisk Qt v6

This directory is the authoritative standalone build entry for the schema-v6 Qt/C++ migration.

It builds:

- `src/engine/GameEngineCore.cpp`
- canonical world, mission, Capital, two-player Neutral and combat-rule modules
- `src/qt/AppControllerV6.cpp`
- `src/app/main_v6.cpp`
- the `MainV6` / `MainMenuV6` / `GameScreenV6` / `BoardPaneV6` QML shell
- `tests/engine_tests_v6.cpp`

## Desktop

```bash
cmake -S v6 -B build/v6 -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=ON
cmake --build build/v6 --parallel
ctest --test-dir build/v6 --output-on-failure
```

## Fog of war

Fog is viewer-aware presentation state, not destructive engine state. A human viewer sees every territory they own plus every territory adjacent to one they own. Hidden territories expose no owner, army count or headquarters identity through the Qt board model.

When the current player is AI, the board remains scoped to the last human viewer. In a multi-human game, the viewer changes when the next human turn becomes active.

## Save schema v6

Schema v6 stores:

- game mode
- six optional rules, including fog of war
- commander-die usage
- standard maneuver route lock
- player/Neutral state
- Secret Mission objectives
- original Capital headquarters
- territory state
- deck/discard/trade progression
- full native RNG state

Native v2-v5 saves remain loadable with newly introduced fields defaulted where they did not exist.

Current Java Gson Classic/world imports preserve two-player Neutral ownership and supported rule/fog flags. Full Java card-hand/deck migration, Secret Mission/Capital Java-save conversion and ObjectStream compatibility remain separate parity work.
