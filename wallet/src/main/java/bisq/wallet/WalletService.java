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
import bisq.wallet.protobuf.GetSeedWordsResponse;
import bisq.wallet.protobuf.GetUnusedAddressResponse;
import bisq.wallet.protobuf.IsWalletEncryptedResponse;
import bisq.wallet.protobuf.IsWalletReadyResponse;
import bisq.wallet.protobuf.SendToAddressRequest;
import bisq.wallet.protobuf.SendToAddressResponse;
import bisq.wallet.vo.Transaction;
import bisq.wallet.vo.Utxo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class WalletService implements Service {
    protected final Observable<Coin> balance = new Observable<>();
    protected final ObservableSet<Transaction> transactions = new ObservableSet<>();
    protected final ObservableSet<String> walletAddresses = new ObservableSet<>();
    protected final Observable<Boolean> walletInitialized = new Observable<>(false);
    protected final Config config;
    private final WalletGrpcClient client;

    @Getter
    public static class Config {
        private final boolean enabled;
        private final String host;
        private final int port;

        public Config(boolean enabled, String host, int port) {
            this.enabled = enabled;
            this.host = host;
            this.port = port;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(
                    config.getBoolean("enabled"),
                    config.getString("host"),
                    config.getInt("port")
            );
        }
    }

    public WalletService(Config config) {
        this.config = config;
        client = new WalletGrpcClient(config.host, config.port);
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

    public void encryptWallet(String password) {
        client.encryptWallet(password);
    }

    public void decryptWallet(String password) {
        client.decryptWallet(password);
    }

    public CompletableFuture<List<String>> getSeedWords() {
        return client.getSeedWords()
                .thenApply(GetSeedWordsResponse::getSeedWordsList);
    }

    public CompletableFuture<Boolean> isWalletReady() {
        return client.isWalletReady().thenApply(IsWalletReadyResponse::getReady);
    }

    public CompletableFuture<String> getUnusedAddress() {
        return client.getUnusedAddress()
                .thenApply(GetUnusedAddressResponse::getAddress);
    }

    public CompletableFuture<ReadOnlyObservableSet<String>> requestWalletAddresses() {
        return client.requestWalletAddresses()
                .thenApply(response -> {
                    walletAddresses.setAll(response.getAddressesList());
                    return walletAddresses;
                });
    }

    public CompletableFuture<List<Transaction>> listTransactions() {
        return client.listTransactions()
                .thenApply(response -> response.getTransactionsList().stream()
                        .map(Transaction::fromProto)
                        .toList());
    }

    public CompletableFuture<List<Utxo>> listUtxos() {
        return client.listUtxos()
                .thenApply(response -> response.getUtxosList().stream()
                        .map(Utxo::fromProto)
                        .toList());
    }

    public CompletableFuture<String> sendToAddress(Optional<String> passphrase, String address, long amount) {
        var builder = SendToAddressRequest.newBuilder();
        passphrase.ifPresent(builder::setPassphrase);
        builder.setAddress(address);
        builder.setAmount(amount);
        var request = builder.build();

        return client.sendToAddress(request)
                .thenApply(SendToAddressResponse::getTxId);
    }

    public CompletableFuture<Boolean> isWalletEncrypted() {
        return client.isWalletEncrypted()
                .thenApply(IsWalletEncryptedResponse::getEncrypted);
    }

    public CompletableFuture<Coin> requestBalance() {
        return client.requestBalance()
                .thenApply(response -> {
                    var newBalance = Coin.asBtcFromValue(response.getBalance());
                    balance.set(newBalance);
                    return newBalance;
                });
    }

    public CompletableFuture<ReadOnlyObservableSet<Transaction>> requestTransactions() {
        return client.listTransactions()
                .thenApply(response -> {
                    transactions.setAll(response.getTransactionsList().stream()
                            .map(Transaction::fromProto)
                            .toList());
                    return transactions;
                });
    }


    public ReadOnlyObservableSet<String> getWalletAddresses() {
        return walletAddresses;
    }

    public ReadOnlyObservable<Coin> getBalance() {
        return balance;
    }

    public ReadOnlyObservableSet<Transaction> getTransactions() {
        return transactions;
    }
}
