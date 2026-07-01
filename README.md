# WinRisk

WinRisk is a lightweight Java implementation of the classic world domination board game.  
It is built with **Gradle** and uses **Swing** for the graphical interface.

## Prerequisites
- Java 17 or newer
- Gradle 8 (or use the provided `gradlew` wrapper)

## Build
Compile sources and run the unit tests:

```bash
gradle build
```

A coverage report is generated under `build/reports/jacoco`. The build fails
if the coverage ratio is below the configured threshold.

## Run
Launch the game directly from Gradle while developing:

```bash
gradle run
```

To create a distributable archive:

```bash
gradle distZip
unzip build/distributions/WinRisk-*.zip
./WinRisk-*/bin/WinRisk
```

## Maps
Several boards ship with the game and can be selected in the host window:

- **World** – the classic 42-territory world board.
- **Pangaea** – the Permian–Triassic supercontinent; cratons and terranes in six regions wrapped around the Tethys Sea.
- **Laurasia** – the northern supercontinent, from Laurentia across Baltica and Siberia to East Asia.
- **Gondwana** – the southern supercontinent of South America, Africa, Arabia, India, Antarctica and Australia.
- **Rodinia** – the Neoproterozoic supercontinent with Laurentia at its core.

Custom maps created in the editor can be placed in a `maps/` folder next to the
application and appear in the map list automatically. Headless play accepts a
built-in name or a file path via `--map=` (e.g. `--map=gondwana`).

## Saving and loading games
A game in progress can be saved and resumed. Closing the game window offers to
save the current game to a file; the start menu's **LOAD GAME** option restores
a previously saved game and continues play from where it left off. Saved games
capture the full board, every player's territories, armies, cards, secret
missions and capitals, the risk-card deck, and the current turn and phase. Saved
games are stored with the same JSON format used for map files.

## Tests
Execute tests and produce coverage data separately with:

```bash
gradle test jacocoTestReport
```

## Repository Structure
- `src/main/java` – application sources
- `src/main/resources` – maps and images used by the game
- `src/test/java` – JUnit tests
- `img/` – example screenshots

