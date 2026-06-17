# Bisq Architecture Patterns

Architecture validation reference for Bisq PR reviews covering Gradle modules, JavaFX patterns, dependency injection, and design principles.

## Multi-Module Gradle Structure

### Module Boundaries

Bisq uses a multi-module Gradle structure. Changes should respect module boundaries and dependencies.

**Core Modules**:
- `core` - Core business logic, domain models
- `desktop` - JavaFX desktop UI application
- `common` - Shared utilities and common code
- `p2p` - P2P networking layer
- `assets` - Asset-specific implementations

**Validation Points**:
- [ ] Changes respect module dependencies (no circular dependencies)
- [ ] Core logic not in UI modules
- [ ] UI code not in core modules
- [ ] Proper module-info.java updates if adding dependencies

**Common Anti-Patterns**:
```java
// ❌ Desktop module calling P2P directly without abstraction
desktop/src/main/java/SomeView.java:
  import bisq.network.p2p.P2PService; // Should use service interface

// ✅ Desktop module using service interface
desktop/src/main/java/SomeView.java:
  import bisq.core.api.CoreApi; // Proper abstraction
```

## JavaFX UI Patterns

### MVVM Architecture

Bisq desktop uses Model-View-ViewModel (MVVM) pattern with JavaFX.

**Components**:
- **View**: FXML or code-based JavaFX UI (`.fxml`, `*View.java`)
- **ViewModel**: Presentation logic (`*ViewModel.java`, `*Model.java`)
- **Model**: Domain objects from core module

**Validation Points**:
- [ ] Views are thin - no business logic
- [ ] ViewModels contain presentation logic only
- [ ] Business logic delegated to services
- [ ] Proper observable property bindings
- [ ] UI updates on JavaFX Application Thread

**Correct Pattern**:
```java
// ✅ Proper MVVM separation
public class TradeView extends ActivatableView<VBox, TradeViewModel> {
    @Inject
    public TradeView(TradeViewModel viewModel) {
        super(viewModel);
    }

    @Override
    protected void activate() {
        // UI bindings only
        amountTextField.textProperty().bindBidirectional(viewModel.amountProperty());
    }
}

public class TradeViewModel extends ActivatableWithDataModel<TradeDataModel> {
    public void onConfirmTrade() {
        // Presentation logic
        if (validate()) {
            dataModel.confirmTrade(); // Delegate to model
        }
    }
}

public class TradeDataModel extends ActivatableDataModel {
    @Inject
    private TradeManager tradeManager; // Business logic service

    public void confirmTrade() {
        tradeManager.confirmTrade(trade); // Delegate to service
    }
}
```

**Anti-Patterns**:
```java
// ❌ Business logic in View
public class TradeView {
    public void onConfirmTrade() {
        // Direct business logic - WRONG
        tradeManager.confirmTrade(trade);
    }
}

// ❌ UI updates not on FX thread
public void updateUI() {
    label.setText(value); // May crash if called from non-FX thread
}

// ✅ Correct FX thread usage
public void updateUI() {
    Platform.runLater(() -> label.setText(value));
}
```

## Dependency Injection (Guice)

Bisq uses Google Guice for dependency injection.

**Validation Points**:
- [ ] Constructor injection preferred
- [ ] `@Inject` annotation on constructor
- [ ] Avoid field injection where possible
- [ ] Singleton services properly scoped
- [ ] Module bindings correct

**Correct Patterns**:
```java
// ✅ Constructor injection
public class TradeManager {
    private final P2PService p2pService;
    private final WalletService walletService;

    @Inject
    public TradeManager(P2PService p2pService, WalletService walletService) {
        this.p2pService = p2pService;
        this.walletService = walletService;
    }
}

// ✅ Module binding
public class TradeModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(TradeManager.class).in(Singleton.class);
    }
}
```

**Anti-Patterns**:
```java
// ❌ Field injection (harder to test)
public class TradeManager {
    @Inject private P2PService p2pService;
}

// ❌ Manual instantiation instead of DI
public class TradeView {
    private TradeManager tradeManager = new TradeManager(); // WRONG
}
```

## Service Layer Pattern

Business logic should be in service classes, not in UI or controllers.

**Service Characteristics**:
- Stateless or carefully managed state
- Reusable across different UI contexts
- Testable without UI
- Clear single responsibility

**Validation Points**:
- [ ] Business logic in services, not Views/ViewModels
- [ ] Services injected, not instantiated
- [ ] Proper error handling
- [ ] Thread safety for concurrent access

**Example Structure**:
```java
// Service layer
@Singleton
public class TradeProtocolManager {
    public void initiateTrade(Trade trade) {
        // Business logic here
    }
}

// ViewModel uses service
public class TradeViewModel {
    @Inject
    private TradeProtocolManager tradeProtocolManager;

    public void onStartTrade() {
        tradeProtocolManager.initiateTrade(trade);
    }
}
```

## Trade Protocol State Machine

Trade protocol uses state machine pattern for safety and clarity.

**Validation Points**:
- [ ] State transitions are valid
- [ ] Each state has clear entry/exit logic
- [ ] Rollback/error handling for failed transitions
- [ ] No state skipping
- [ ] Proper state persistence

**State Machine Pattern**:
```java
public enum TradeState {
    INIT,
    MAKER_SENT_PUBLISH_DEPOSIT_TX_REQUEST,
    TAKER_RECEIVED_PUBLISH_DEPOSIT_TX_REQUEST,
    TAKER_PUBLISHED_DEPOSIT_TX,
    // ... more states
}

// State transition validation
public void transitionTo(TradeState newState) {
    if (!isValidTransition(currentState, newState)) {
        throw new IllegalStateTransitionException();
    }
    currentState = newState;
    persist();
}
```

## DAO Governance Patterns

DAO-related code requires extra care for voting and governance logic.

**Validation Points**:
- [ ] Voting weight calculations correct
- [ ] Proposal validation thorough
- [ ] Bond lock/unlock logic safe
- [ ] No fund loss paths
- [ ] Backward compatibility maintained

**Critical Checks**:
```java
// Vote weight calculation
long voteWeight = stake * votingPower; // Check for overflow
if (voteWeight < stake) throw new ArithmeticException("Overflow");

// Proposal parameter bounds
if (proposalAmount > MAX_PROPOSAL_AMOUNT) {
    reject();
}

// Bond management
public void lockBond(long amount) {
    if (availableBalance < amount) throw new InsufficientFundsException();
    lockedBalance += amount;
    availableBalance -= amount;
    assert lockedBalance + availableBalance == totalBalance; // Invariant check
}
```

## P2P Networking Architecture

P2P layer has specific patterns for reliability and security.

**Validation Points**:
- [ ] Protocol versioning for backward compatibility
- [ ] Message serialization safe
- [ ] Connection management proper
- [ ] Peer discovery logic correct
- [ ] Bootstrap nodes handling

**Protocol Versioning**:
```java
// Version compatibility check
public boolean isCompatible(int peerVersion) {
    return peerVersion >= MIN_SUPPORTED_VERSION
        && peerVersion <= CURRENT_VERSION;
}

// Graceful handling of version differences
if (!isCompatible(peer.getVersion())) {
    if (peer.getVersion() < MIN_SUPPORTED_VERSION) {
        disconnect(peer, "Outdated version");
    } else {
        // Future version - log but allow
        log.warn("Peer has newer version: {}", peer.getVersion());
    }
}
```

## Code Quality Standards

**General Patterns**:
- [ ] Immutability preferred (final fields, immutable collections)
- [ ] Defensive copying for mutable data
- [ ] Null safety (Optional, @Nullable annotations)
- [ ] Proper exception handling (don't swallow exceptions)
- [ ] Logging at appropriate levels

**Immutability Example**:
```java
// ✅ Immutable class
public final class Trade {
    private final String tradeId;
    private final Coin amount;

    public Trade(String tradeId, Coin amount) {
        this.tradeId = requireNonNull(tradeId);
        this.amount = requireNonNull(amount);
    }

    // No setters, only getters
}

// ❌ Mutable shared state
public class TradeManager {
    public List<Trade> trades = new ArrayList<>(); // WRONG: public mutable
}
```

## Architecture Review Template

```markdown
## Bisq Architecture Review for PR #{number}

### Module Structure
- **Modules Modified**: {list}
- **Dependency Changes**: {describe}
- **Findings**:
  - Module boundaries: {✅|⚠️|❌} {details}
  - Circular dependencies: {✅ None|❌ Found} {details}

### UI Layer (JavaFX)
- **MVVM Compliance**: {✅|⚠️|❌}
  - Views thin: {details}
  - ViewModels appropriate: {details}
  - Business logic in services: {details}
- **Threading**: {✅|⚠️|❌}
  - FX thread usage correct: {details}

### Dependency Injection
- **Injection Pattern**: {✅|⚠️|❌}
  - Constructor injection: {details}
  - Proper scoping: {details}

### Trade Protocol
- **State Machine**: {✅|⚠️|❌}
  - Valid transitions: {details}
  - Error handling: {details}
  - Persistence: {details}

### DAO Governance
- **If DAO changes present**:
  - Voting logic: {✅|⚠️|❌} {details}
  - Proposal validation: {✅|⚠️|❌} {details}
  - Fund safety: {✅|⚠️|❌} {details}

### Code Quality
- **Immutability**: {✅|⚠️|❌}
- **Null safety**: {✅|⚠️|❌}
- **Exception handling**: {✅|⚠️|❌}

### Overall Architecture Assessment
- **Compliance Level**: {FULL|PARTIAL|NEEDS WORK}
- **Architectural Concerns**: {count}
- **Recommendation**: {specific guidance}
```
