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

package bisq.wallet.mock;

import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.wallet.WalletException;
import bisq.wallet.WalletService;
import bisq.wallet.vo.Transaction;
import bisq.wallet.vo.TransactionInfo;
import bisq.wallet.vo.Utxo;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * This is just a temporary mock implementation for facilitation wallet UI development.
 * The Bitcoin and Electrum wallet implementations are not maintained, and we will use the BDK wallet for MuSig, thus
 * work to get those working again would be wasted effort.
 */
@Slf4j
public class MockWalletService implements WalletService {
    private String encryptionPassword = "";
    private boolean shouldFailSeedWords = true;
    private List<String> seedWords = null;

    @Override
    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
                }, CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS))
                .thenRun(() -> {
                    future.complete(true);
                });
        return future;
    }

    @Override
    public boolean isWalletReady() {
        return true;
    }

    @Override
    public CompletableFuture<String> getUnusedAddress() {
        return CompletableFuture.completedFuture("Not implemented yet");
    }

    @Override
    public ObservableSet<String> getWalletAddresses() {
        return new ObservableSet<>(Set.of("mock address 1", "mock address 2"));
    }

    @Override
    public CompletableFuture<ObservableSet<String>> requestWalletAddresses() {
        return CompletableFuture.completedFuture(new ObservableSet<>(Set.of("mock address 1", "mock address 2")));
    }

    @Override
    public CompletableFuture<List<? extends TransactionInfo>> listTransactions() {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<List<? extends Utxo>> listUnspent() {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<String> sendToAddress(Optional<String> passphrase, String address, double amount) {
        return CompletableFuture.completedFuture("Not implemented yet");
    }

    @Override
    public CompletableFuture<Boolean> isWalletEncrypted() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Coin> requestBalance() {
        return CompletableFuture.completedFuture(Coin.asBtcFromValue(100000));
    }

    @Override
    public Observable<Coin> getBalance() {
        return new Observable<>(Coin.asBtcFromValue(100000));
    }

    @Override
    public ObservableSet<Transaction> getTransactions() {
        return new ObservableSet<>(Set.of());
    }

    @Override
    public CompletableFuture<ObservableSet<Transaction>> requestTransactions() {
        return CompletableFuture.completedFuture(new ObservableSet<>(Set.of()));
    }

    @Override
    public void encryptWallet(String password) {
        log.debug("setEncryptionPassword: REDACTED");
        encryptionPassword = password;
    }

    @Override
    public CompletableFuture<List<String>> getSeedWords() {
        if (shouldFailSeedWords) {
            shouldFailSeedWords = false;
            return CompletableFuture.failedFuture(new WalletException("Simulated failure: cannot fetch seed words"));
        }

        // shouldFailSeedWords = true;
        if (seedWords != null) {
            return CompletableFuture.completedFuture(seedWords);
        }

        return CompletableFuture.supplyAsync(() -> {
                    List<String> words = Arrays.asList("car", "van", "lion", "water", "bero", "cycle",
                            "love", "key", "system", "wife", "husband", "trade");
                    seedWords = words;
                    return words;
                },
                CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS)
        );
    }
}
