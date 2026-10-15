# Code Guidelines

## Purpose

This document defines coding conventions used across Bisq modules.
It complements:

- [Architecture](architecture.md)
- [MVC Pattern](mvc-pattern.md)
- [Observable Framework](observable-framework.md)
- [Testing](testing.md)
- [Nullability and Optional](nullability-and-optional.md)

## General Principles

- Follow existing architecture and patterns. Do not introduce new paradigms without strong reason.
- Prefer immutability in domain models.
- Use Lombok annotations for `@Getter`, `@Setter`, `@ToString`, and `@EqualsAndHashCode` where appropriate.
- Use the narrowest visibility scope possible.
- Prefer project utilities from `bisq.common.util` when appropriate, especially `StringUtils` and `ByteArrayUtils`.
- Prefer descriptive names. Avoid one-letter variables except simple loop indices.
- Organize imports and run formatter before committing to reduce diff noise.
- Follow default IntelliJ IDEA formatting settings.

## Code Style

### Braces and Control Flow

- Use K&R brace style.
- Always use braces, including single-line branches.
- Do not use `final` for local variables or method parameters; use it for class fields.
- Prefer `final` fields and avoid nullable values where possible.

### Ternary Operators

- Keep short ternaries on one line.
- Use multi-line formatting for long expressions.
- Avoid nested ternaries.

## Nullability

- Values are non-null by default.
- If absence is valid, model it explicitly.
- Use `@Nullable` only where nullable APIs are unavoidable (common in JavaFX integration).

See [Nullability and Optional](nullability-and-optional.md) for the detailed rationale and conventions.

## Readability and Maintainability

- Prefer self-explanatory naming over extensive documentation.
- Split complex methods into smaller focused methods.
- Avoid boilerplate Javadoc; use it when API context is non-trivial.
- Put each parameter on its own line when parameter lists are long.
- Break fluent chains per `.` for readability, except very short chains.

## Class and Data Structure

- Declare static fields first, then instance fields.
- Within each group, place `final` fields before non-final fields.
- Place inner classes, records, and enums near the top (after fields) or at file end.
- Avoid inner classes that are used outside the enclosing class; extract top-level classes instead.
- Always use `@Override` for overriding methods.
- Use records for simple value objects only.
- Use `@ToString` and `@EqualsAndHashCode` for value objects, and `callSuper = true` when needed.
- In large classes, group methods with separator blocks and keep two blank lines before each separator.

Format:

```java
/* --------------------------------------------------------------------- */
// Group Name
/* --------------------------------------------------------------------- */
```

## JavaFX and UI

- In UI classes, group fields of the same type on a single line to reduce vertical noise.
- Follow the [MVC Pattern](mvc-pattern.md).
- Use `bind()` for simple bindings.
- Use `EasyBind.subscribe()` for logic-heavy reactive handling.
- Prefer `EasyBind.subscribe()` over raw JavaFX listeners.

### Subscription and Observer Lifecycle

If an MVC controller has multiple subscriptions or pins, use this pattern:

```java
private final Set<Subscription> subscriptions = new HashSet<>();
private final Set<Pin> pins = new HashSet<>();

@Override
public void onDeactivate() {
    subscriptions.forEach(Subscription::unsubscribe);
    subscriptions.clear();
    pins.forEach(Pin::unbind);
    pins.clear();
}
```

If an MVC view has multiple subscriptions, use this pattern:

```java
private final Set<Subscription> subscriptions = new HashSet<>();

@Override
public void onViewDetached() {
    subscriptions.forEach(Subscription::unsubscribe);
    subscriptions.clear();
}
```

## Visibility and Test Hooks

- Keep classes and methods as non-public as possible.
- `@VisibleForTesting` is only justified when:
  - the member would otherwise be `private`, and
  - it is directly required by tests.
- If a member is used in normal production flow, do not annotate it with `@VisibleForTesting`.
