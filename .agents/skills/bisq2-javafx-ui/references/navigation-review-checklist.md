# Navigation And Review Checklist

## Navigation Target Changes

1. Add new entries in `NavigationTarget`.
- Use parent relationship correctly.
- Set `allowPersistence=false` for transient overlay/wizard steps.

Example:
```java
MY_FEATURE(CONTENT),
MY_FEATURE_STEP(MY_FEATURE),
MY_TRANSIENT_STEP(OVERLAY, false),
```

2. Register controller creation in parent `NavigationController#createController(...)`.

3. Add tab/menu wiring in parent view where needed.

4. Decide controller caching policy (`useCaching()`).

## Navigation APIs

- `Navigation.navigateTo(target)`
- `Navigation.navigateTo(target, initData)` for init-with-data flows
- `Navigation.back()`

## Final Review Checklist

Architecture:
- [ ] View has no service/domain access.
- [ ] Controller mutates model state, not raw widget state.
- [ ] Model only has UI state and lightweight helper logic.
- [ ] User-facing strings are via `Res.get(...)`.

Lifecycle:
- [ ] All `Pin` unbound in `onDeactivate()`.
- [ ] All bindings/subscriptions cleaned in `onViewDetached()`.
- [ ] Event handlers removed/nullified.
- [ ] Schedulers/timelines/futures cleaned.

Threading:
- [ ] All UI mutations on JavaFX thread.
- [ ] Render-frame deferral for layout-dependent reads.
- [ ] Async staleness guards where concurrent refreshes can race.

Styling/UX:
- [ ] Existing CSS classes reused.
- [ ] Design tokens and spacing align with repo CSS.
- [ ] Animation uses `Transitions`/`ManagedDuration`.
- [ ] Keyboard flow deterministic and focus-safe.

Data and input:
- [ ] FxBindings/direct observers bridge domain to UI.
- [ ] Formatters used for amounts/prices/date/time.
- [ ] Validators and converters applied where appropriate.

## Key Source Paths

- `apps/desktop/desktop/src/main/java/bisq/desktop/common/view/`
- `apps/desktop/desktop/src/main/java/bisq/desktop/common/observable/FxBindings.java`
- `apps/desktop/desktop/src/main/java/bisq/desktop/common/threading/`
- `apps/desktop/desktop/src/main/java/bisq/desktop/common/utils/`
- `apps/desktop/desktop/src/main/java/bisq/desktop/common/converters/`
- `apps/desktop/desktop/src/main/java/bisq/desktop/components/controls/`
- `apps/desktop/desktop/src/main/java/bisq/desktop/components/table/`
- `apps/desktop/desktop/src/main/resources/css/`
- `presentation/src/main/java/bisq/presentation/formatters/`
- `common/src/main/java/bisq/common/observable/`
