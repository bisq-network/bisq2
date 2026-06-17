# Component Patterns

## Leaf MVC Triad (Default)

Controller:
- Construct model/view.
- Bind domain observables in `onActivate()`.
- Unbind pins in `onDeactivate()`.
- Expose user handlers (`onXxx`) called by view.

Model:
- JavaFX properties (`StringProperty`, `BooleanProperty`, `ObjectProperty`, lists).

View:
- Build node tree and style classes in constructor.
- Bind in `onViewAttached()`.
- Fully unbind/unsubscribe/unset handlers in `onViewDetached()`.

## Navigation Container

Use `NavigationController` when a host selects child views by `NavigationTarget`.

Implement:
- `createController(NavigationTarget)` switch.
- `NavigationModel#getDefaultNavigationTarget()`.

## Tab Views

Use `TabController` + `TabView`.

Important:
- Add tabs with `addTab(...)` (actual API), not `createTab(...)`.
- Let framework manage selected tab button + marker transitions.

## Domain To UI Binding

Preferred patterns:

1) `FxBindings` mapping pipelines
```java
Pin p = FxBindings.bind(model.getItems())
        .filter(...)
        .map(...)
        .to(service.getObservableSet());
```

2) Direct observers when no pipeline needed
```java
Pin p = service.getFlag().addObserver(v -> UIThread.run(() -> model.getFlag().set(v)));
```

3) JavaFX direct bindings in view
```java
label.textProperty().bind(model.getTitle());
field.textProperty().bindBidirectional(model.getInput());
```

## Forms, Validation, Converters

- Use `MaterialTextField` and validator classes in `components/controls/validator`.
- Validate on submit and surface inline errors.
- Prefer typed bidirectional bindings with converters for numeric/network fields:
```java
textField.textProperty().bindBidirectional(model.getValue(), model.getValueConverter());
```

## Table/List Patterns

- Use `BisqTableView` and `BisqTableColumn.Builder`.
- Keep raw domain objects out of view cells when presentation mapping is needed; map into list items.
- Set comparator on sorted list in controller/model.

## Async Race Guard (Expert)

Avoid stale completion writes:
```java
private final AtomicLong seq = new AtomicLong();

void refresh() {
    long s = seq.incrementAndGet();
    service.load().whenComplete((result, err) -> UIThread.run(() -> {
        if (s != seq.get()) return;
        if (err == null) model.getItems().setAll(result);
    }));
}
```

## Anti-Patterns To Avoid

- View calling services.
- Missing unbind/unsubscribe on detach.
- UI changes from background threads.
- Hiding nodes with `setVisible(false)` only when layout gap should collapse.
- Manual string parsing for typed settings when converter exists.
