# Testing

## Purpose

Bisq tests focus on domain behavior where correctness matters and regressions are likely.
The default priority is fast, deterministic unit tests.

## General Principles

- When domain behavior changes, add or update tests.
- Prefer focused tests with explicit inputs and expected outputs.
- Keep tests easy to understand and maintain.
- If code is hard to test, consider refactoring into smaller pure functions/services.

## `@VisibleForTesting` Usage

Use `@VisibleForTesting` only when both conditions are true:

- the member would otherwise be `private`
- the member is directly required by tests

Do not use `@VisibleForTesting` when the member is part of normal production usage.

## UI Layer

- UI code is typically not unit-tested directly.
- If UI logic grows complex, move business logic into testable domain/service code.

### JavaFX tests and input robots

Ordinary `test`, `check`, `build`, and root `buildAll` runs do not run TestFX robot tests.
`buildAll` invokes the ordinary builds of the included projects; it does not invoke `uiTest`.
The desktop, desktop-automation, and desktop-ui-harness-app test workers use headless Monocle,
including tests which start JavaFX directly. No display or macOS Accessibility permission is needed.

Run the robot tests explicitly from the repository root:

```sh
./gradlew -p apps/desktop :desktop:uiTest
```

These tests use `@DesktopUiTest`. Ordinary desktop test tasks exclude its `ui` tag. The annotation
also requires the `bisq.uiTest=true` JVM property set by `uiTest`, so an IntelliJ JUnit run which
does not use Gradle's filters skips them. Its extension creates the TestFX robot only after JUnit
has checked that condition. Do not register `ApplicationExtension` directly: JUnit constructs
declarative extensions before checking execution conditions.

Delegate IDE test runs to Gradle to also get the headless JVM configuration for non-robot tests.
For an intentional native desktop robot run, use `-PuiTest.native=true` with `uiTest`. That opt-in
can move the real pointer, type into the focused window, and request macOS Accessibility permission.
The property does not change ordinary test tasks. A missing or incompatible Monocle dependency
must fail a headless run; it must not cause a fallback to native input.

Monocle isolates GUI input on a virtual screen. It is not a security sandbox for filesystem access,
network access, or processes started by test code. See the
[desktop test execution specification](../specifications/development/desktop-test-execution.md).

## Test Quality

- Tests must be deterministic and reproducible.
- Keep test runtime fast.
- Do not weaken assertions to make tests pass.
- Avoid coupling tests to execution order or external mutable state.

## Handling Failures

- Investigate root cause before changing tests.
- Update expected values only for intentional and correct behavior changes.

## Concurrency and Shared Resources

- Be mindful of parallel execution.
- Isolate or lock shared resources when required.
- Avoid race-prone and timing-sensitive test logic.

## Checking Logs After Manual or Network Tests

Some failures appear only in the log: a store skips an entry at load, a node rejects network data of a peer, or a
node drops a message. The screen shows no error, so a tester easily overlooks them. This matters most in tests with
nodes of different versions.

After such a test, scan the log files of every node:

```sh
python3 scripts/log_anomaly_scan.py <data directory> [<data directory> ...]
```

- A path can be a data directory, a log file, or a directory whose subdirectories are data directories.
- The report lists the findings of each node and of each application session, together with the version of the
  session. So a data directory which an old version and then a new version used shows the findings of each version.
- An allowlist removes lines which are expected and harmless, for example the warning of a node of version 2.1.13
  about the unknown feature of newer peers. The report still counts these lines. A line which has a harmless cause and
  also a harmful one stays a finding, and its description names the known cause.
- `--last-session` limits the report to the last start, `--include-warnings` adds all other `WARN` lines, and
  `--json` prints a report for scripts. The exit code is 1 when a finding of level `warning` or `error` remains.

The rules and the allowlist are at the top of the script. Add a rule when a new log line marks a silent failure, and
extend `scripts/test_log_anomaly_scan.py` with it. Run its tests with
`python3 -m unittest scripts/test_log_anomaly_scan.py`.
