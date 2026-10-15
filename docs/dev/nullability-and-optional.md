# Nullability and Optional

## Purpose

This document defines how Bisq models absence and nullable state.

## Core Convention

- Values are non-null by default.
- If absence is a valid state, represent it explicitly with `Optional`.
- Exception: in established `Observable<T>` signal/reset flows, emitting `null` can be intentional; observers must handle it explicitly.

## Why This Convention

Nullable values are a common source of runtime bugs and defensive boilerplate.
Explicit optionality improves type-level readability and caller-callee contracts.

Compared with implicit nullability, explicit optionality helps with:

- reducing defensive null checks
- making API contracts explicit
- preserving stronger non-null invariants

## Relationship to Java Guidance

Java guidance often recommends `Optional` primarily for return types.
In Bisq, we extend explicit optionality as a broader project convention where absence is part of the model.

## Interaction with `@Nullable`

`@Nullable` and `@NonNull` annotations can help, but they are weaker than explicit types.
Use `@Nullable` primarily where framework APIs require nullable interaction (for example JavaFX integration points).

## Practical Limitations

This model still requires discipline:

- Java does not prevent assigning `null` to an `Optional` reference.
- Static analysis and code review are still required.

## UI Considerations

UI frameworks may require nullable state transitions.
In those cases:

- use `@Nullable` where required
- keep non-null-by-default elsewhere
- isolate nullable boundary code as much as possible

## Conceptual Alignment

The convention is similar to null-safe languages:

- non-null by default
- explicit modeling when absence is allowed

## Related Documents

- [Code Guidelines](code-guidelines.md)
- [Testing](testing.md)
