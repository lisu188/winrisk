# Agent Guidelines

This repository contains a Java 17 Gradle project for the WinRisk game. Use these
instructions when making changes anywhere in this repo.

## Development Practices
- Keep changes focused and well-structured; prefer small, coherent commits.
- Follow existing coding style and naming seen in the surrounding files.
- Avoid adding unnecessary dependencies.

## Testing Expectations
- Run the Gradle build before submitting changes: `./gradlew build`.
- Unit tests must maintain at least **80% code coverage**; the build already
  enforces this threshold via JaCoCo.
- Include new or updated tests for any behavior changes.

## Documentation
- Update relevant README or comments if you introduce new behaviors or tooling
  requirements.
