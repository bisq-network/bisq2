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
