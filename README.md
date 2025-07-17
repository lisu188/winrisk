# WinRisk

A simple strategy game project built with Gradle.

## Building the project

Use Gradle to compile and package the application:

```bash
gradle jar distZip
```

This creates a runnable JAR in `build/libs` and a zipped distribution in `build/distributions`.

## Running the game

Run the packaged JAR with:

```bash
java -jar build/libs/WinRisk-1.0-SNAPSHOT.jar
```

Ensure a graphical environment is available since the game uses Swing.
