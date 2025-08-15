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

import bisq.common.monetary.Coin;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.wallet.vo.Transaction;
import bisq.wallet.vo.Utxo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MockWalletService extends WalletService {
    public MockWalletService(Config config) {
        super(config);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (!config.isEnabled()) {
            log.info("WalletService was disabled");
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    public void encryptWallet(String password) {
        log.info("encryptWallet");
    }

    public void decryptWallet(String password) {
        log.info("decryptWallet");
    }

    public CompletableFuture<List<String>> getSeedWords() {
        return CompletableFuture.completedFuture(List.of("car", "van", "lion",
                "water", "bero", "cycle",
                "love", "key", "system",
                "wife", "husband", "trade"));
    }

    public CompletableFuture<Boolean> isWalletReady() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<String> getUnusedAddress() {
        return CompletableFuture.completedFuture("39C7fxSzEACPjM72Z7xdPxhf7mKxJwvfMJ");
    }

    public CompletableFuture<ReadOnlyObservableSet<String>> requestWalletAddresses() {
        return CompletableFuture.completedFuture(walletAddresses);
    }

    public CompletableFuture<List<Transaction>> listTransactions() {
        return CompletableFuture.completedFuture(List.of());
    }

    public CompletableFuture<List<Utxo>> listUtxos() {
        return CompletableFuture.completedFuture(List.of());
    }

    public CompletableFuture<String> sendToAddress(Optional<String> passphrase, String address, long amount) {
        return CompletableFuture.completedFuture("69111c8de670d7a12b8c4db85c67485889b30335cdd3fd7f18924104e88e9fc3");
    }

    public CompletableFuture<Boolean> isWalletEncrypted() {
        return CompletableFuture.completedFuture(false);
    }

    public CompletableFuture<Coin> requestBalance() {
        return CompletableFuture.completedFuture(balance.get());
    }

    public CompletableFuture<ReadOnlyObservableSet<Transaction>> requestTransactions() {
        return CompletableFuture.completedFuture(transactions);
    }
}
