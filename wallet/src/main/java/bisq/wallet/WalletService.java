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
import bisq.persistence.PersistenceService;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Slf4j
public class WalletService implements Service {
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
    private int stateGeneration;

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
            this.enabled = enabled;
            this.host = host;
            this.port = port;
            this.mock = mock;
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
        this.config = config;
        this.client = client;
        this.receiveAddressService = new ReceiveAddressService(persistenceService);
        this.walletStoreService = new WalletStoreService(persistenceService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (!config.enabled) {
            log.info("WalletService was disabled");
            return CompletableFuture.completedFuture(false);
        }
        return client.initialize().thenApply(result -> {
            refreshState();
            return result;
        });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return client.shutdown();
    }

    public ReadOnlyObservable<State> getState() {
        return state;
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
                .thenApply(SendToAddressResponse::getTxId));
    }

    // The daemon's wallet always has a password.
    public CompletableFuture<Boolean> isWalletEncrypted() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Coin> requestBalance() {
        return tracked(fetchBalance());
    }

    private CompletableFuture<Coin> fetchBalance() {
        return client.requestBalance()
                .thenApply(response -> {
                    var newBalance = Coin.asBtcFromValue(response.getBalance());
                    balance.set(newBalance);
                    balanceBreakdown.set(new BalanceBreakdown(response.getBalance(),
                            response.getConfirmed(),
                            response.getTrustedPending(),
                            response.getUntrustedPending(),
                            response.getImmature()));
                    return newBalance;
                });
    }

    public CompletableFuture<ReadOnlyObservableSet<Transaction>> requestTransactions() {
        return tracked(client.listTransactions()
                .thenApply(response -> {
                    try {
                        transactions.setAll(ProtobufUtils.fromProtoStream(response.getTransactionsList(),
                                        Transaction::fromProto)
                                .toList());
                    } catch (UnresolvableProtobufException e) {
                        throw new CompletionException(e);
                    }
                    return transactions;
                }));
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
