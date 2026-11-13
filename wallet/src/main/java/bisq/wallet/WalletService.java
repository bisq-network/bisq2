/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.wallet;

import bisq.common.application.Service;
import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.timer.Scheduler;
import bisq.persistence.PersistenceService;
import bisq.wallet.protobuf.GetBalanceResponse;
import bisq.wallet.protobuf.GetSeedWordsResponse;
import bisq.wallet.protobuf.PubAddressInfo;
import bisq.wallet.protobuf.SendToAddressRequest;
import bisq.wallet.protobuf.SendToAddressResponse;
import bisq.wallet.receive_address.ReceiveAddressService;
import bisq.wallet.vo.Transaction;
import bisq.wallet.vo.Utxo;
import bisq.wallet.vo.AddressBalance;
import bisq.wallet.vo.BalanceBreakdown;
import bisq.wallet.receive_address.ReceiveAddressEntry;

import com.google.common.net.InetAddresses;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class WalletService implements Service {
    private static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofSeconds(30);

    private final WalletGrpcClient client;
    protected final ReceiveAddressService receiveAddressService;
    protected final Observable<Coin> balance = new Observable<>();
    protected final Observable<BalanceBreakdown> balanceBreakdown = new Observable<>();
    protected final ObservableSet<Transaction> transactions = new ObservableSet<>();
    protected final ObservableSet<String> walletAddresses = new ObservableSet<>();
    protected final ObservableSet<AddressBalance> addressBalances = new ObservableSet<>();
    protected final Config config;
    private final WalletStoreService walletStoreService;
    private final Observable<State> state = new Observable<>(State.NOT_SET_UP);
    private final Duration refreshInterval;
    private final AtomicInteger balanceSequence = new AtomicInteger();
    private final AtomicInteger transactionsSequence = new AtomicInteger();
    private int stateGeneration;
    private int appliedBalanceSequence;
    private int appliedTransactionsSequence;
    @Nullable
    private volatile Scheduler refreshScheduler;
    @Nullable
    private volatile CompletableFuture<?> pollInFlight;

    public enum State {
        NOT_SET_UP,
        LOCKED,
        OPEN,
        UNAVAILABLE
    }

    @Getter
    public static class Config {
        private final boolean enabled;
        private final String host;
        private final int port;
        private final boolean mock;

        public Config(boolean enabled, String host, int port, boolean mock) {
            String trimmedHost = host.trim();
            // The channel is plaintext, so the wallet password and the seed words would cross the network unencrypted
            checkArgument(mock || !enabled || isLoopback(trimmedHost),
                    "application.wallet.host must be localhost or a loopback IP address such as 127.0.0.1 or ::1 " +
                            "while the connection to the wallet daemon is plaintext, but is %s", host);
            this.enabled = enabled;
            this.host = trimmedHost;
            this.port = port;
            this.mock = mock;
        }

        private static boolean isLoopback(String host) {
            return host.equalsIgnoreCase("localhost")
                    || (InetAddresses.isInetAddress(host) && InetAddresses.forString(host).isLoopbackAddress());
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(
                    config.getBoolean("enabled"),
                    config.getString("host"),
                    config.getInt("port"),
                    config.hasPath("mock") && config.getBoolean("mock")
            );
        }
    }

    public WalletService(Config config,
                         PersistenceService persistenceService) {
        this(config, persistenceService, new WalletGrpcClient(config.host, config.port));
    }

    WalletService(Config config,
                  PersistenceService persistenceService,
                  WalletGrpcClient client) {
        this(config, persistenceService, client, DEFAULT_REFRESH_INTERVAL);
    }

    WalletService(Config config,
                  PersistenceService persistenceService,
                  WalletGrpcClient client,
                  Duration refreshInterval) {
        this.config = config;
        this.client = client;
        this.refreshInterval = refreshInterval;
        this.receiveAddressService = new ReceiveAddressService(persistenceService);
        this.walletStoreService = new WalletStoreService(persistenceService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (!config.enabled) {
            log.info("WalletService was disabled");
            return CompletableFuture.completedFuture(false);
        }
        return client.initialize();
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        stopPolling();
        return walletStoreService.persistIfUnsaved().thenCompose(saved -> client.shutdown());
    }

    // The periodic poll runs only while started; the desktop starts it with the MuSig setting, as the wallet UI exists
    // only then. Until the first probe the state keeps its initial value.
    public synchronized void startPolling() {
        if (refreshScheduler != null) {
            return;
        }
        refreshState();
        refreshScheduler = Scheduler.run(this::refreshWhileOpen)
                .host(this)
                .runnableName("refreshWallet")
                .periodically(refreshInterval.toMillis());
    }

    public synchronized void stopPolling() {
        if (refreshScheduler != null) {
            refreshScheduler.stop();
            refreshScheduler = null;
        }
    }

    public ReadOnlyObservable<State> getState() {
        return state;
    }

    // The daemon sees new blocks on its own poll, so the balance is polled here, and the transactions follow a
    // changed breakdown (an incoming payment is untrusted pending first, which leaves the spendable balance alone).
    // A failing call re-probes the state, which is how a restarted daemon shows up as LOCKED; while the daemon is
    // unreachable the tick probes, so it comes back without a user action. One poll in flight at a time.
    private void refreshWhileOpen() {
        CompletableFuture<?> previous = pollInFlight;
        if (previous != null && !previous.isDone()) {
            return;
        }
        State current = state.get();
        if (current == State.UNAVAILABLE) {
            pollInFlight = refreshState();
            return;
        }
        if (current != State.OPEN) {
            return;
        }
        Scheduler scheduler = refreshScheduler;
        BalanceBreakdown previousBreakdown = balanceBreakdown.get();
        pollInFlight = fetchBalance().whenComplete((newBalance, throwable) -> {
            // An answer which arrives after the poll was stopped triggers no further call.
            if (refreshScheduler != scheduler) {
                return;
            }
            if (throwable != null) {
                if (findState(throwable).isPresent()) {
                    refreshState();
                }
            } else if (!Objects.equals(balanceBreakdown.get(), previousBreakdown)) {
                requestTransactions();
            }
        });
    }

    // The daemon cannot say whether a wallet exists, so the persisted flag decides between NOT_SET_UP and the daemon
    // states. GetBalance needs an open wallet and has no side effect, which makes it the probe; IsWalletReady is also
    // false while an open wallet syncs, so it cannot tell LOCKED from OPEN.
    public CompletableFuture<Void> refreshState() {
        if (!walletStoreService.isWalletExists()) {
            applyState(State.NOT_SET_UP);
            return CompletableFuture.completedFuture(null);
        }
        int generation = nextProbeGeneration();
        return fetchBalance().handle((balance, throwable) -> {
            if (throwable == null) {
                applyProbeResult(generation, State.OPEN);
            } else {
                // An error the probe cannot classify keeps the state, except that an install with a wallet must not
                // be shown the setup page
                findState(throwable)
                        .or(() -> state.get() == State.NOT_SET_UP ? Optional.of(State.UNAVAILABLE) : Optional.empty())
                        .ifPresent(probed -> applyProbeResult(generation, probed));
            }
            return null;
        });
    }

    // Creates the wallet at the first call and opens it afterwards. Success is the only place the flag is set,
    // so an install never asks the daemon to open a wallet it did not create.
    public CompletableFuture<Void> openOrCreateWallet(String password) {
        return client.openOrCreateWallet(password)
                .whenComplete((response, throwable) -> {
                    if (throwable == null) {
                        walletStoreService.setWalletExists();
                        applyState(State.OPEN);
                        // The screens bound to the balance may already be attached, so give them a value right away
                        fetchBalance();
                    } else if (findState(throwable).isPresent()) {
                        refreshState();
                    }
                })
                .thenApply(response -> null);
    }

    public CompletableFuture<List<String>> getSeedWords(String password) {
        return tracked(client.getSeedWords(password)
                .thenApply(GetSeedWordsResponse::getSeedWordsList));
    }

    public CompletableFuture<ReceiveAddressEntry> getUnusedAddress() {
        return tracked(client.getUnusedAddress()
                .thenApply(response -> response.getAddress().getAddress())
                .thenApply(receiveAddressService::findOrAddReceiveAddressEntry));
    }

    public CompletableFuture<ReadOnlyObservableSet<String>> requestWalletAddresses() {
        return tracked(client.requestWalletAddresses()
                .thenApply(response -> {
                    walletAddresses.setAll(response.getAddressesList().stream()
                            .map(PubAddressInfo::getAddress)
                            .toList());
                    return walletAddresses;
                }));
    }

    public CompletableFuture<List<Transaction>> listTransactions() {
        return tracked(client.listTransactions()
                .thenApply(response -> {
                    try {
                        return ProtobufUtils.fromProtoStream(response.getTransactionsList(), Transaction::fromProto)
                                .toList();
                    } catch (UnresolvableProtobufException e) {
                        throw new CompletionException(e);
                    }
                }));
    }

    public CompletableFuture<List<Utxo>> listUtxos() {
        return tracked(client.listUtxos()
                .thenApply(response -> {
                    try {
                        return ProtobufUtils.fromProtoStream(response.getUtxosList(), Utxo::fromProto).toList();
                    } catch (UnresolvableProtobufException e) {
                        throw new CompletionException(e);
                    }
                }));
    }

    public CompletableFuture<ReadOnlyObservableSet<AddressBalance>> requestAddressBalances() {
        return listUtxos().thenApply(utxos -> {
            var items = utxos.stream()
                    .collect(Collectors.groupingBy(Utxo::getAddress))
                    .entrySet().stream()
                    .map(e -> {
                        String address = e.getKey();
                        var list = e.getValue();
                        long total = list.stream().mapToLong(Utxo::getAmount).sum();
                        int numUsage = list.size();
                        int numConfirmations = list.stream().mapToInt(Utxo::getNumConfirmations).min().orElse(0);
                        return new AddressBalance(address, total, numUsage, numConfirmations);
                    })
                    .toList();
            addressBalances.setAll(items);
            return addressBalances;
        });
    }

    // The daemon hands out a fresh unused address per call, but cycles through them once 50 are unused, so an
    // address can come back together with its stored note.
    public CompletableFuture<ReceiveAddressEntry> createReceiveAddress() {
        return getUnusedAddress();
    }

    public boolean updateReceiveAddress(ReceiveAddressEntry receiveAddressEntry,
                                        Optional<String> note) {
        return receiveAddressService.updateReceiveAddressEntry(receiveAddressEntry, note);
    }

    public CompletableFuture<String> sendToAddress(Optional<String> passphrase, String address, long amount) {
        return sendToAddress(passphrase, address, amount, OptionalLong.empty());
    }

    // Without a fee rate the daemon applies its default rate.
    public CompletableFuture<String> sendToAddress(Optional<String> passphrase,
                                                   String address,
                                                   long amount,
                                                   OptionalLong feeRatePerKwu) {
        // The daemon requires the passphrase; an absent one is sent empty and refused as a wrong password.
        var request = SendToAddressRequest.newBuilder()
                .setPassphrase(passphrase.orElse(""))
                .setAddress(PubAddressInfo.newBuilder().setAddress(address))
                .setAmount(amount);
        feeRatePerKwu.ifPresent(request::setFeeRatePerKwu);

        return tracked(client.sendToAddress(request.build())
                .thenApply(SendToAddressResponse::getTxId)
                .whenComplete((txId, throwable) -> {
                    if (throwable == null) {
                        requestBalance();
                        requestTransactions();
                    }
                }));
    }

    // The daemon's wallet always has a password.
    public CompletableFuture<Boolean> isWalletEncrypted() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Coin> requestBalance() {
        return tracked(fetchBalance());
    }

    // An answer older than the last applied one is dropped: with the poll and the dashboard both asking, a slow
    // answer can arrive after a newer one. The caller gets the balance the service holds afterwards.
    private CompletableFuture<Coin> fetchBalance() {
        int sequence = balanceSequence.incrementAndGet();
        return client.requestBalance()
                .thenApply(response -> applyBalance(sequence, response));
    }

    private synchronized Coin applyBalance(int sequence, GetBalanceResponse response) {
        Coin newBalance = Coin.asBtcFromValue(response.getBalance());
        if (sequence > appliedBalanceSequence) {
            appliedBalanceSequence = sequence;
            balance.set(newBalance);
            balanceBreakdown.set(new BalanceBreakdown(response.getBalance(),
                    response.getConfirmed(),
                    response.getTrustedPending(),
                    response.getUntrustedPending(),
                    response.getImmature()));
            return newBalance;
        }
        return Optional.ofNullable(balance.get()).orElse(newBalance);
    }

    public CompletableFuture<ReadOnlyObservableSet<Transaction>> requestTransactions() {
        int sequence = transactionsSequence.incrementAndGet();
        return tracked(client.listTransactions()
                .thenApply(response -> {
                    try {
                        applyTransactions(sequence, ProtobufUtils.fromProtoStream(response.getTransactionsList(),
                                        Transaction::fromProto)
                                .toList());
                    } catch (UnresolvableProtobufException e) {
                        throw new CompletionException(e);
                    }
                    return transactions;
                }));
    }

    private synchronized void applyTransactions(int sequence, List<Transaction> latest) {
        if (sequence > appliedTransactionsSequence) {
            appliedTransactionsSequence = sequence;
            transactions.setAll(latest);
        }
    }

    public ReadOnlyObservableSet<String> getWalletAddresses() {
        return walletAddresses;
    }

    public ReadOnlyObservable<Coin> getBalance() {
        return balance;
    }

    public ReadOnlyObservable<BalanceBreakdown> getBalanceBreakdown() {
        return balanceBreakdown;
    }

    public ReadOnlyObservableSet<Transaction> getTransactions() {
        return transactions;
    }

    public ReadOnlyObservableSet<AddressBalance> getAddressBalances() {
        return addressBalances;
    }

    public Optional<ReceiveAddressEntry> findReceiveAddressEntry(String receiveAddress) {
        return receiveAddressService.getReceiveAddressEntries().stream()
                .filter(entry -> entry.getAddress().equals(receiveAddress))
                .findFirst();
    }

    // A failure that means the daemon state changed (musigd restarted or went away) is answered with a new probe,
    // not with a direct state write, so a stale result of a call issued earlier cannot undo a later open.
    private <T> CompletableFuture<T> tracked(CompletableFuture<T> future) {
        return future.whenComplete((result, throwable) -> {
            if (throwable != null && findState(throwable).isPresent()) {
                refreshState();
            }
        });
    }

    private static Optional<State> findState(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        if (cause instanceof WalletException walletException) {
            return switch (walletException.getReason()) {
                case WALLET_NOT_OPEN -> Optional.of(State.LOCKED);
                case DAEMON_UNAVAILABLE, UNSUPPORTED_CALL, CLIENT_NOT_READY -> Optional.of(State.UNAVAILABLE);
                // A timeout says nothing about the wallet, the daemon may hold its lock for a sync
                default -> Optional.empty();
            };
        }
        return Optional.empty();
    }

    // A direct write and every new probe invalidate the probes still in flight, so only the newest answer counts.
    protected synchronized void applyState(State newState) {
        stateGeneration++;
        state.set(newState);
    }

    private synchronized int nextProbeGeneration() {
        return ++stateGeneration;
    }

    private synchronized void applyProbeResult(int generation, State newState) {
        if (generation == stateGeneration) {
            state.set(newState);
        }
    }
}
