# Observable Framework (`bisq.common.observable`)

## Purpose

`bisq.common.observable` is the non-UI observer framework used in services and domain modules.

It provides primitives for:

- single values (`Observable<S>`)
- collections (`ObservableSet<S>`, `ObservableArray<S>`)
- maps (`ObservableHashMap<K, V>`)

Use `FxBindings` in desktop modules to bridge these observables to JavaFX properties and collections.

## Package Layout

Core:

- `bisq.common.observable.Observable`
- `bisq.common.observable.ReadOnlyObservable`
- `bisq.common.observable.Pin`

Collections:

- `bisq.common.observable.collection.ObservableCollection`
- `bisq.common.observable.collection.ObservableSet`
- `bisq.common.observable.collection.ObservableArray`
- `bisq.common.observable.collection.CollectionObserver`

Maps:

- `bisq.common.observable.map.ObservableHashMap`
- `bisq.common.observable.map.HashMapObserver`

## Core Concepts

### `Pin` and Lifecycle

Observer registrations return a `Pin`.
Store it and call `unbind()` when owner lifecycle ends.

```java
Pin pin = observable.addObserver(value -> {
    // react to updates
});

// later, e.g. onDeactivate
pin.unbind();
```

This follows [MVC lifecycle conventions](mvc-pattern.md).

### Immediate Callback on Registration

Observers are invoked immediately on registration:

- `Observable.addObserver(...)` receives current value
- `ObservableCollection.addObserver(...)` receives current content via `onAllAdded(...)`
- `ObservableHashMap.addObserver(...)` receives current map content via `putAll(...)`

This avoids missing initial synchronization.

### Read-Only Exposure Pattern

Keep mutation private to owner classes and expose read-only types.

```java
private final Observable<State> state = new Observable<>();

public ReadOnlyObservable<State> stateObservable() {
    return state;
}
```

For snapshot-style data, also prefer unmodifiable views (`getUnmodifiableList()`, `getUnmodifiableSet()`, `getUnmodifiableMap()`).

## Value Observable: `Observable<S>`

### Semantics

- `set(value)` returns `true` only when value changed.
- Equality checks are null-safe and use `.equals(...)` semantics.
- Equal old/new values do not trigger callbacks.
- Observer exceptions are caught and logged so one observer does not block others.

### Typical Usage

Use `Observable<Optional<T>>` when absence is valid:

```java
private final Observable<Optional<TradeAmount>> limit = new Observable<>(Optional.empty());
```

## Collection Observables

### Types

- `ObservableSet<S>` uses a concurrent set (`ConcurrentHashMap.newKeySet()`).
- `ObservableArray<S>` uses a synchronized list (`Collections.synchronizedList(new ArrayList<>())`).

Both inherit from `ObservableCollection<S>` and notify `CollectionObserver<S>`.

### `CollectionObserver<S>` Callbacks

- `onAdded(S element)`
- `onRemoved(Object element)`
- `onCleared()`
- helper defaults: `onAllAdded`, `onAllRemoved`, `onAllSet`

### Operations and Notifications

- `add` / `addAll` -> `onAdded` / `onAllAdded`
- `remove` / `removeAll` -> `onRemoved` / `onAllRemoved`
- `clear` -> `onCleared`
- `setAll(values)` -> `onAllSet(values)`
- `retainAll` is intentionally unsupported

### Threading Notes

Observer storage is thread-safe (`CopyOnWriteArrayList`), but underlying collection semantics still apply:

- `ObservableSet`: concurrent operations supported
- `ObservableArray`: synchronize compound iteration operations

## Map Observables

`ObservableHashMap<K, V>` is backed by `ConcurrentHashMap`.

### `HashMapObserver<K, V>` Callbacks

- `put(K key, V value)`
- `remove(Object key)`
- `clear()`
- `putAll(...)`

### Notification Behavior

- `put` notifies with `put(key, value)`
- `putAll` notifies with `putAll(map)`
- `remove` notifies only when key existed
- `clear` notifies every time
- `computeIfAbsent` notifies only when a new value is created

Unlike `Observable` and `ObservableCollection`, map observer callbacks are not internally wrapped with try/catch.

## Bridging to JavaFX (`FxBindings`)

Use `FxBindings` to map service/domain observables into JavaFX state.

```java
Pin selectedUserProfilePin = FxBindings.bind(model.selectedUserProfile)
        .to(chatUserService.getSelectedUserProfile());

Pin userProfilesPin = FxBindings.<ChatUserIdentity, ListItem>bind(model.userProfiles)
        .map(ListItem::new)
        .to(chatUserService.getUserProfiles());
```

`FxBindings` schedules updates on the JavaFX thread (`UIThread.run(...)`).

## Best Practices

- Keep pin ownership explicit and clean them up on deactivate/shutdown.
- Expose read-only observable interfaces from services/domain objects.
- Keep observer callbacks small and side-effect aware.
- Use `addObserver(Runnable)` only when change details are irrelevant.
- Keep non-UI modules decoupled from JavaFX.

## References

- `common/src/main/java/bisq/common/observable`
- `common/src/main/java/bisq/common/observable/collection`
- `common/src/main/java/bisq/common/observable/map`
- `apps/desktop/desktop/src/main/java/bisq/desktop/common/observable/FxBindings.java`
