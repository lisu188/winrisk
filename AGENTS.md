# Agent Guidelines

This repository contains a Java 17 Gradle project for the WinRisk game. Use these
instructions when making changes anywhere in this repo. AI/LLM agents can
accelerate WinRisk development by drafting code changes, tests, and documentation
while keeping the Java 17 Gradle + Swing stack consistent. They assist with
refactoring legacy game logic, generating JUnit 4 tests, reviewing UI updates
under `com.winrisk.gui`, and improving build/CI automation. Use agents to propose
changes quickly, but keep humans accountable for correctness, gameplay fidelity,
and release quality.

## Development Practices
- Keep changes focused and well-structured; prefer small, coherent commits.
- Follow existing coding style and naming seen in the surrounding files.
- Avoid adding unnecessary dependencies.
- Align with existing package structure (`com.winrisk.game`, `com.winrisk.gui`)
  and Swing UI patterns; mirror naming, formatting, and Gradle conventions
  found in surrounding files.
- Prefer minimal, coherent changes; avoid large refactors unless explicitly
  requested and accompanied by regression tests under `src/test/java`.
- Keep dependencies lean; suggest additions only when necessary and with
  justification.
- Preserve save-file compatibility and gameplay rules when modifying
  serialization or map data under `src/main/resources`.
- Document behavior or tooling changes in `README.md` or inline comments where
  relevant.

## Testing Expectations
- Run the Gradle build before submitting changes: `./gradlew build`.
- Unit tests must maintain at least **80% code coverage**; the build already
  enforces this threshold via JaCoCo.
- Include new or updated tests for any behavior changes.
- Treat LLM output as a draft: always review, run `./gradlew build`, and ensure
  coverage stays at or above the JaCoCo rule (currently 70%, aim for 80%+ on new
  code).

## Global Principles & Constraints
- Never expose credentials, tokens, or PII in prompts, code, logs, or generated
  assets.
- Follow repository authority: `build.gradle` defines dependencies and coverage
  rules; CI in `.github/workflows/gradle.yml` is the release gate; existing tests
  are the behavioral source of truth.
- Keep `AGENTS.md` aligned with `README.md`, `CONTRIBUTING.md`, and
  `CODEOWNERS` (if present) so guidance remains consistent across the project.

## Agent Catalogue
Below are recommended agents tailored to WinRisk. Combine them as needed for
complex tasks.

| Agent | Primary goal | Typical inputs | Outputs | Tools / context | When to use | Limitations |
| --- | --- | --- | --- | --- | --- | --- |
| Core Refactor Agent | Modernize or simplify game/GUI classes while preserving behavior. | Target classes (e.g., `src/main/java/com/winrisk/game/Play.java`, `com/winrisk/gui/GamePanel.java`), known bugs, performance notes. | Patch suggestions with rationale and impact notes. | Reads local files; may call `./gradlew test` for quick validation. | When reducing duplication, improving readability, or untangling game flow. | May miss subtle GUI threading issues; requires human check of event dispatch thread usage. |
| Test Authoring Agent | Add or strengthen JUnit 4 tests to raise confidence and coverage. | Class or package under test, current gaps, failure logs. | New/updated tests in `src/test/java`, fixtures using `TestUtil`. | Uses project test helpers and JaCoCo targets. | When adding features, fixing bugs, or guarding regressions. | Needs guidance on complex Swing interactions; snapshots not supported. |
| Documentation Agent | Improve README or inline comments for new behaviors or tooling. | Feature summary, changed APIs, instructions for players/builders. | Markdown/text updates aligned with repo style. | References `README.md`, `img/` assets. | When features change user workflow or build steps. | Should avoid inventing gameplay details; verify with code/tests. |
| API/Serialization Contract Agent | Safeguard persistence and data formats (missions, maps, save files). | Serialization logic (`src/main/java/com/winrisk/game`), resource schemas under `src/main/resources`, expected compatibility. | Contract notes, migration steps, and validation tests. | Reads file IO code and fixtures; can propose backward-compatible adapters. | When altering game state persistence or map parsing. | May overlook edge-case files; manual validation required. |
| Performance Review Agent | Identify hotspots in map loading, AI turns, or rendering. | Profiling notes, stack traces, large map scenarios. | Suggestions for caching, data structures, or rendering tweaks. | Considers `Play` loop, AI routines, Swing painting in `GraphicsSurface`. | When UI lags or AI turns are slow. | Estimates only; needs empirical profiling afterward. |
| Security/Secrets Scan Agent | Ensure no sensitive data or unsafe code enters the repo. | Diffs, added config files, resource changes. | Findings about secrets, unsafe deserialization, or dependency risks. | Scans for tokens, reviews Gson usage, and Gradle dependencies. | Before opening PRs or after adding third-party libraries. | Not a substitute for dedicated scanners; may miss nuanced CVEs. |
| Migration/Upgrade Agent | Plan and execute dependency or Gradle upgrades. | Target versions (e.g., Gradle, Gson), build logs, CI outcomes. | Step-by-step migration plan, code/build updates. | Uses `build.gradle`, `gradle/wrapper` files, CI config. | When bumping Java/Gradle or libraries, or adjusting JaCoCo thresholds. | Needs human oversight for compatibility testing across platforms. |
| UI/UX Review Agent | Evaluate Swing UI flows and layouts under `com.winrisk.gui`. | Screenshots, component hierarchy, usability issues. | Layout adjustments, event handling improvements, accessibility tips. | Reviews `GamePanel`, `StartGame`, `HostGameWindow` and related resources. | When modifying menus, map display, or dialogs. | No live rendering; suggestions need manual visual verification. |
| Build & CI Agent | Keep Gradle build, JaCoCo, and GitHub Actions healthy. | Build failures, CI logs (`.github/workflows/gradle.yml`). | Fixes for tasks, caching, distribution packaging. | Interprets Gradle output; edits build scripts and workflow files. | When CI breaks, coverage thresholds change, or distribution packaging is updated. | Might not detect platform-specific issues; verify on CI. |

## Standard Workflows (Playbooks)
### Refactor a Legacy Module
1. Run Core Refactor Agent with the target file paths (e.g.,
   `com/winrisk/game/Play.java`) and known pain points; request behavior-
   preserving changes and rationale.
2. Have Test Authoring Agent propose or update tests in `src/test/java`
   covering modified flows.
3. If serialization is touched, consult API/Serialization Contract Agent for
   backward compatibility notes.
4. Apply changes, run `./gradlew build`, and manually check gameplay-critical
   behaviors.

### Add a New UI Feature
1. Ask UI/UX Review Agent to sketch the change referencing `com/winrisk/gui`
   classes and any resources.
2. Use Core Refactor Agent to integrate the UI logic cleanly with existing
   controllers.
3. Invoke Test Authoring Agent for JUnit coverage (favor headless-friendly tests
   where possible).
4. Run `./gradlew build`; manually verify UI in a local run (`gradle run`).

### Fix a Failing Test or CI Break
1. Provide failing logs to Build & CI Agent to interpret Gradle or workflow
   errors.
2. If logic regressions are suspected, involve Core Refactor Agent to inspect
   affected classes.
3. Re-run `./gradlew test jacocoTestReport` to confirm fixes; ensure coverage
   gate passes.
4. Before merging, have Security/Secrets Scan Agent review diffs for leaks or
   risky patterns.

### Plan and Execute a Dependency Upgrade
1. Engage Migration/Upgrade Agent with target versions (e.g., new Gradle wrapper
   or Gson release) and current `build.gradle` snippet.
2. Apply suggested build and wrapper updates; adjust `.github/workflows/gradle.yml`
   if needed.
3. Run `./gradlew build distZip` locally; then let Build & CI Agent check for
   packaging implications.
4. Validate serialization compatibility via API/Serialization Contract Agent when
   dependencies touch parsing or IO.

### Improve Performance on Large Maps or AI Turns
1. Provide profiling notes to Performance Review Agent, focusing on `Play` loops
   and AI routines.
2. If changes alter data persisted to disk, consult API/Serialization Contract
   Agent for compatibility.
3. Have Test Authoring Agent add regression tests around map loading or AI
   timing constraints when feasible.
4. Run `./gradlew build` and manually test large-map scenarios.

## Safety, Quality & Review Checklist
- [ ] All applicable tests pass locally and in CI (`./gradlew build`), with
      JaCoCo coverage above the enforced threshold (target 80%+ even though 70%
      is configured).
- [ ] No secrets, tokens, or sensitive data introduced; configuration remains
      safe for public repos.
- [ ] Public APIs, serialization formats, and save-file compatibility preserved
      or explicitly migrated with tests.
- [ ] Changes follow existing Java style, package structure, and Gradle
      conventions; no unnecessary dependencies added.
- [ ] UI changes manually verified in `gradle run` and visually inspected when
      applicable.
- [ ] Documented any behavior or tooling updates in README or inline comments.
- [ ] LLM-suggested code reviewed by a human; discard hallucinated APIs or
      unsupported patterns.

## Documentation
- Update relevant README or comments if you introduce new behaviors or tooling
  requirements.
- Maintain this document whenever the tech stack, CI, coverage thresholds, or
  key workflows change; review at least once per release.
- To add a new agent, extend the catalogue table with its goal, inputs, outputs,
  and usage guidance rooted in actual repo paths or tools.
- Deprecate workflows that no longer apply by replacing them with current
  practices and noting superseded steps.
