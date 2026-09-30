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
