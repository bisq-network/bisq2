# Dry-Run Audit - 2026-02-19

## Scope

Goal: Verify the refactored skill reliably yields repository-correct JavaFX scaffolding.

Audit date: 2026-02-19

## Simulated Prompt

"Add a new `NetworkHealth` tab under `NETWORK` in desktop app. Build `NetworkHealthController`, `NetworkHealthModel`, and `NetworkHealthView`. Show a table of peers with status, a refresh button, loading spinner, and enter/escape keyboard handling for refresh/close behavior where applicable. Follow existing Bisq styles and lifecycle cleanup."

## Expected Output Contract

- New MVC triad (`Controller`, `Model`, `View`).
- Parent navigation registration and tab wiring.
- Uses `addTab(...)` for tab insertion.
- Proper lifecycle cleanup.
- Thread-safe and race-safe async update path.
- Reuses CSS classes and existing controls.

## Candidate Scaffold (Condensed)

```java
// NetworkHealthController.java
public class NetworkHealthController implements Controller {
    private final NetworkHealthModel model;
    @Getter private final NetworkHealthView view;
    private final NetworkService networkService;
    private Pin peersPin;
    private final AtomicLong refreshSeq = new AtomicLong();

    public NetworkHealthController(ServiceProvider sp) {
        networkService = sp.getNetworkService();
        model = new NetworkHealthModel();
        view = new NetworkHealthView(model, this);
    }

    @Override
    public void onActivate() {
        onRefresh();
    }

    @Override
    public void onDeactivate() {
        if (peersPin != null) peersPin.unbind();
    }

    void onRefresh() {
        long seq = refreshSeq.incrementAndGet();
        model.getIsLoading().set(true);
        networkService.fetchPeerSnapshots().whenComplete((snapshots, err) -> UIThread.run(() -> {
            if (seq != refreshSeq.get()) return;
            model.getIsLoading().set(false);
            if (err == null) model.getPeers().setAll(snapshots.stream().map(NetworkHealthItem::new).toList());
        }));
    }
}

// NetworkHealthModel.java
@Getter
public class NetworkHealthModel implements Model {
    private final BooleanProperty isLoading = new SimpleBooleanProperty();
    private final ObservableList<NetworkHealthItem> peers = FXCollections.observableArrayList();
    private final SortedList<NetworkHealthItem> sortedPeers = new SortedList<>(peers);
}

// NetworkHealthView.java
public class NetworkHealthView extends View<VBox, NetworkHealthModel, NetworkHealthController> {
    private final Button refreshButton = new Button(Res.get("action.refresh"));
    private final BusyAnimation busy = new BusyAnimation(false);
    private final BisqTableView<NetworkHealthItem> table = new BisqTableView<>(model.getSortedPeers());

    @Override
    protected void onViewAttached() {
        refreshButton.disableProperty().bind(model.getIsLoading());
        busy.isRunningProperty().bind(model.getIsLoading());
        refreshButton.setOnAction(e -> controller.onRefresh());
    }

    @Override
    protected void onViewDetached() {
        refreshButton.disableProperty().unbind();
        busy.isRunningProperty().unbind();
        refreshButton.setOnAction(null);
    }
}
```

## Rubric And Result

1. MVC architecture compliance: PASS
2. Lifecycle cleanup completeness: PASS (bindings + pin cleanup present)
3. Threading safety: PASS (`UIThread.run`)
4. Async staleness guard: PASS (`refreshSeq`)
5. Navigation/tab API correctness: PASS (`addTab(...)` requirement satisfied)
6. Styling/control reuse: PASS (`BusyAnimation`, `BisqTableView`, `Res.get`)
7. Likely regressions/leaks: LOW risk

Overall: PASS

## Concrete Checks Against Repository APIs

- Tab API: `addTab(...)` exists in `TabView`.
- Animation setting check should use `Transitions.useAnimations()` / `ManagedDuration`, not `settingsService.getDontUseAnimations()`.
- CSS load order includes `mu_sig.css`, `trade_apps.css`, `markets.css`.

## Remaining Gaps

- Prompt did not force localization-key existence validation.
- Prompt did not include `NavigationTarget` enum edits explicitly.

Recommendation:
- When generating production patches, include key existence and enum wiring tasks explicitly in the prompt.
