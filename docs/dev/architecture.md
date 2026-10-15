# Architecture

## Purpose

This document summarizes core architectural conventions used across Bisq modules.

## Dependency Policy

- Avoid adding dependencies unless clearly justified.
- Prefer plain Java and existing project modules over third-party libraries.
- Keep dependency footprint small to reduce risk for supply-chain attacks.

## Module Structure

Most domain areas are encapsulated in dedicated modules.
Each module typically exposes a root service that coordinates its domain logic and child services.

The root service should:

- create and wire child services
- manage child-service lifecycle
- provide the main module entry point

## Service Role

A service in Bisq is usually a singleton representing one domain concern.

A service typically:

- exposes domain operations for callers
- coordinates domain workflows
- manages persistence through dedicated store components
- implements the `Service` lifecycle contract

Services should encapsulate domain behavior and avoid leaking implementation details.

## Event-Driven Model

Bisq uses state-oriented event propagation.
State changes are published through observables, and consumers react by updating local model state.

### Core Flow

- Domain state is owned by services.
- Services expose observable state (`Observable`, `ObservableSet`, map observables).
- Controllers subscribe or bind to that state and map it into UI models.
- Views bind UI elements to model properties and call on-action methods on the controller.
- Controllers call service APIs to mutate domain state.
- The updated domain state flows back via the domain observers to the controller.

This creates unidirectional ownership with reactive propagation:
service state -> controller/model projection -> view rendering -> user action -> controller -> service mutation.


## Asynchronous and Threading Boundaries

- Use `CompletableFuture` for asynchronous workflows.
- Keep async boundaries explicit and easy to trace.
- UI state updates must run on the JavaFX UI thread.
- `FxBindings` and UI controllers bridge domain observables to JavaFX with `UIThread.run(...)` where needed.

This keeps non-UI services decoupled from JavaFX while preserving safe UI updates.
