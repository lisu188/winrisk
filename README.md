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

