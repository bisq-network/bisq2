# MVC Pattern

## Purpose

Bisq 2 uses a classical MVC variant adapted for JavaFX (bindings, reactive updates, lifecycle hooks).

## Scope and Rationale

Bisq 1 relied heavily on MVVM, where responsibilities often became blurred and views grew too large.
Bisq 2 prefers stricter MVC responsibilities and composition of smaller MVC components.

Domain logic does not belong to the MVC triad.
Domain behavior belongs to services in domain modules.

## Core MVC Classes

### Controller

The controller is the entry point for its MVC unit and creates model and view.
It is the only class exposed to other parts of the application.

Controller responsibilities:

- orchestration and UI behavior logic
- interaction with services and child controllers
- mapping service state into model state

Controller constraints:

- do not call view methods for behavior
- update model state and let the view react through bindings/subscriptions

### Model

The model stores UI state (properties and observable collections).
It does not contain domain logic.
It does not know view or controller.

### View

The view renders UI and binds UI elements to model properties.
It forwards user interactions to controller handler methods (typically `on...` methods).

View constraints:

- no domain logic
- no direct model mutation through setters for behavior flow
- only lightweight layout/presentation logic

## View Graph Construction

Controllers compose the view graph by creating child controllers and attaching child views.
This usually happens via navigation controllers.

Overlay/pop-up flows with MVC follow the same principle using overlay navigation targets.
Lightweight popovers that do not need full MVC may stay outside this pattern.

## MVC Components

To keep screens maintainable, large views should be decomposed into smaller components.

For compact components, inner classes (`Model`, `View`, `Controller`) are acceptable when they are private to the component.
The outer component acts as the public interface.

Guidelines:

- keep inner MVC classes private
- expose only component-level API from the outer class
- avoid unnecessary boilerplate in inner private MVC classes

## Lifecycle Management

Lifecycle hooks:

- Controller: `onActivate()` / `onDeactivate()`
- View: `onViewAttached()` / `onViewDetached()`

Rules:

- register listeners/subscriptions/bindings in activate/attach hooks
- unregister/unbind/unsubscribe in deactivate/detach hooks
- clear event handlers in `onViewDetached()` when UI nodes can remain cached

Even when leaks seem unlikely, always clean up resources explicitly.

## Observer and Binding Patterns

- For JavaFX property binding, use JavaFX bindings.
- Prefer `EasyBind` over raw listeners in UI code.
- For non-UI modules, use `bisq.common.observable` and `FxBindings` bridges where needed.

Example:

```java
Pin selectedUserProfilePin = FxBindings.bind(model.selectedUserProfile)
        .to(chatUserService.getSelectedUserProfile());

Pin userProfilesPin = FxBindings.<ChatUserIdentity, ListItem>bind(model.userProfiles)
        .map(ListItem::new)
        .to(chatUserService.getUserProfiles());
```

## Navigation Model

Navigation uses hierarchical targets.
Resolving a target updates the full parent chain.

Example chain:

`ROOT -> PRIMARY_STAGE -> MAIN -> CONTENT -> SETTINGS -> NETWORK_INFO`

Navigation controllers:

- declare which target subtree they manage
- lazily create child controllers in `createController(...)`
- are cached by default

With caching enabled:

- constructors typically run once
- `onActivate()` / `onDeactivate()` run on attach/detach

Caching behavior can be overridden when required.

## Related Documents

- [Code Guidelines](code-guidelines.md)
- [Observable Framework](observable-framework.md)
