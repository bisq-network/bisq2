# Architecture And Lifecycle

## MVC Contract In Bisq2

Triad:
- `XxxController`: behavior, service access, event handlers.
- `XxxModel`: JavaFX properties for presentation state.
- `XxxView`: visual tree, bindings, event delegation.

Repository-specific rules:
- Controller should not directly mutate view widget state; set model and let bindings render.
- Passing `getView().getRoot()` for composition/utilities is acceptable and used widely.
- Model may contain lightweight helper methods (`reset`, derived flags), but no service/domain calls.
- View must not call services directly.

## Base Framework Types

- `Controller`: `apps/desktop/desktop/src/main/java/bisq/desktop/common/view/Controller.java`
- `View`: `apps/desktop/desktop/src/main/java/bisq/desktop/common/view/View.java`
- `NavigationController`: `apps/desktop/desktop/src/main/java/bisq/desktop/common/view/NavigationController.java`
- `TabController`: `apps/desktop/desktop/src/main/java/bisq/desktop/common/view/TabController.java`
- `NavigationView`: `apps/desktop/desktop/src/main/java/bisq/desktop/common/view/NavigationView.java`
- `TabView`: `apps/desktop/desktop/src/main/java/bisq/desktop/common/view/TabView.java`
- `FillStageView` marker: `apps/desktop/desktop/src/main/java/bisq/desktop/common/view/FillStageView.java`

## Lifecycle Order (Critical)

From `View` internals:
- Attach:
  - `controller.onActivateInternal()`
  - `view.onViewAttachedInternal()`
- Detach:
  - `controller.onDeactivateInternal()`
  - `view.onViewDetachedInternal()`

Implication:
- Controller can initialize model before view binds.
- Detach cleanup must handle partial attachment safety.

## Caching Semantics

- Default: `Controller.useCaching() == true`.
- `NavigationController` caches child controllers by `NavigationTarget`.
- Use `useCaching() == false` for transient flows:
  - overlays/wizards
  - `InitWithDataController` flows
  - one-shot states where stale state is risky

If non-cached, expect recreation on next navigation.

## Cleanup Responsibilities

In `Controller.onDeactivate()`:
- `Pin.unbind()` for domain observers/FxBindings.
- Remove scene/application event handlers.
- Stop/clear controller-owned schedulers/timelines/futures.

In `View.onViewDetached()`:
- `unbind()` and `unbindBidirectional()` JavaFX bindings.
- `Subscription.unsubscribe()` for EasyBind.
- Null event handlers (`setOnAction(null)`, etc.).
- Dispose transient child controls/popups if allocated in view.

## Threading And Render Frames

- Any UI mutation from async/background code: `UIThread.run(...)`.
- Layout-dependent work (sizes/bounds/scroll position): `UIThread.runOnNextRenderFrame(...)`.
- `FxBindings` marshals to UI thread; avoid wrapping again unless needed.

## Keyboard And Focus Contract

Prefer centralized helpers:
- `KeyHandlerUtil.handleEscapeKeyEvent(...)`
- `KeyHandlerUtil.handleEnterKeyEventWithTextInputFocusCheck(...)`

Register key handlers on activate; always remove on deactivate.
