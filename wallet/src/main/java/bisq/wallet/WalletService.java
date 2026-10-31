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
import bisq.wallet.protobuf.IsWalletReadyResponse;
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
    protected final Observable<Boolean> walletInitialized = new Observable<>(false);
    protected final Config config;

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
        return client.shutdown();
    }

    public ReadOnlyObservable<Boolean> getWalletInitialized() {
        return walletInitialized;
    }

    /** Marks the wallet as initialized (i.e., setup wizard completed). */
    public void setWalletInitialized() {
        walletInitialized.set(true);
    }

    public CompletableFuture<Void> openOrCreateWallet(String password) {
        return client.openOrCreateWallet(password).thenApply(response -> null);
    }

    public CompletableFuture<List<String>> getSeedWords(String password) {
        return client.getSeedWords(password)
                .thenApply(GetSeedWordsResponse::getSeedWordsList);
    }

    public CompletableFuture<Boolean> isWalletReady() {
        return client.isWalletReady().thenApply(IsWalletReadyResponse::getReady);
    }

    public CompletableFuture<ReceiveAddressEntry> getUnusedAddress() {
        return client.getUnusedAddress()
                .thenApply(response -> response.getAddress().getAddress())
                .thenApply(receiveAddressService::findOrAddReceiveAddressEntry);
    }

    public CompletableFuture<ReadOnlyObservableSet<String>> requestWalletAddresses() {
        return client.requestWalletAddresses()
                .thenApply(response -> {
                    walletAddresses.setAll(response.getAddressesList().stream()
                            .map(PubAddressInfo::getAddress)
                            .toList());
                    return walletAddresses;
                });
    }

    public CompletableFuture<List<Transaction>> listTransactions() {
        return client.listTransactions()
                .thenApply(response -> {
                    try {
                        return ProtobufUtils.fromProtoStream(response.getTransactionsList(), Transaction::fromProto)
                                .toList();
                    } catch (UnresolvableProtobufException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    public CompletableFuture<List<Utxo>> listUtxos() {
        return client.listUtxos()
                .thenApply(response -> {
                    try {
                        return ProtobufUtils.fromProtoStream(response.getUtxosList(), Utxo::fromProto).toList();
                    } catch (UnresolvableProtobufException e) {
                        throw new CompletionException(e);
                    }
                });
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

        return client.sendToAddress(request.build())
                .thenApply(SendToAddressResponse::getTxId);
    }

    // The daemon's wallet always has a password.
    public CompletableFuture<Boolean> isWalletEncrypted() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Coin> requestBalance() {
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
        return client.listTransactions()
                .thenApply(response -> {
                    try {
                        transactions.setAll(ProtobufUtils.fromProtoStream(response.getTransactionsList(),
                                        Transaction::fromProto)
                                .toList());
                    } catch (UnresolvableProtobufException e) {
                        throw new CompletionException(e);
                    }
                    return transactions;
                });
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
}
