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

import bisq.common.application.DevMode;
import bisq.common.monetary.Coin;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.wallet.vo.Transaction;
import bisq.wallet.vo.TransactionInput;
import bisq.wallet.vo.TransactionOutput;
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
        if (DevMode.isDevMode() && DevMode.isDevModeWalletSetup()) {
            setWalletInitialized();
            log.info("Wallet initialized");
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
        return CompletableFuture.completedFuture(Coin.asBtcFromValue(2501234));
    }

    public CompletableFuture<ReadOnlyObservableSet<Transaction>> requestTransactions() {
        TransactionInput input1 = new TransactionInput(
                "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2",
                0,
                4294967295L,
                new byte[]{(byte)0x01, (byte)0x02},
                ""
        );
        TransactionOutput output1 = new TransactionOutput(
                100000L,
                "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
                new byte[]{(byte)0x76, (byte)0xa9}
        );
        Transaction tx1 = new Transaction(
                "69111c8de670d7a12b8c4db85c67485889b30335cdd3fd7f18924104e88e9fc3",
                List.of(input1),
                List.of(output1),
                0L,
                800000,
                new java.util.Date(System.currentTimeMillis() - 86400000L), // 1 day ago
                6,
                100000L,
                true
        );

        TransactionInput input2 = new TransactionInput(
                "f2e1d0c9b8a7z6y5x4w3v2u1t0s9r8q7p6o5n4m3l2k1j0i9h8g7f6e5d4c3b2a1",
                1,
                4294967294L,
                new byte[]{(byte)0x03, (byte)0x04},
                ""
        );
        TransactionOutput output2 = new TransactionOutput(
                2000000L,
                "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
                new byte[]{(byte)0x76, (byte)0xa9}
        );
        Transaction tx2 = new Transaction(
                "b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2a1",
                List.of(input2),
                List.of(output2),
                0L,
                800001,
                new java.util.Date(System.currentTimeMillis() - 43200000L), // 12 hours ago
                3,
                2000000L,
                false
        );

        TransactionInput input3 = new TransactionInput(
                "c3b2a1f2e1d0c9b8a7z6y5x4w3v2u1t0s9r8q7p6o5n4m3l2k1j0i9h8g7f6e5d4",
                0,
                4294967293L,
                new byte[]{(byte)0x05, (byte)0x06},
                ""
        );
        TransactionOutput output3 = new TransactionOutput(
                50000L,
                "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy",
                new byte[]{(byte)0x76, (byte)0xa9}
        );
        Transaction tx3 = new Transaction(
                "c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4",
                List.of(input3),
                List.of(output3),
                0L,
                800002,
                new java.util.Date(System.currentTimeMillis() - 172800000L), // 2 days ago
                0,
                50000L,
                false
        );

        TransactionInput input4 = new TransactionInput(
                "d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7z6y5x4w3",
                2,
                4294967292L,
                new byte[]{(byte)0x07, (byte)0x08},
                ""
        );
        TransactionOutput output4 = new TransactionOutput(
                750000L,
                "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kygt080",
                new byte[]{(byte)0x76, (byte)0xa9}
        );
        Transaction tx4 = new Transaction(
                "d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5",
                List.of(input4),
                List.of(output4),
                0L,
                800003,
                new java.util.Date(System.currentTimeMillis() - 3 * 3600 * 1000L), // 3 hours ago
                1,
                750000L,
                true
        );

        TransactionInput input5 = new TransactionInput(
                "e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7z6y5x4",
                0,
                4294967291L,
                new byte[]{(byte)0x09, (byte)0x0A},
                ""
        );
        TransactionOutput output5 = new TransactionOutput(
                150000L,
                "1BoatSLRHtKNngkdXEeobR76b53LETtpyT",
                new byte[]{(byte)0x76, (byte)0xa9}
        );
        Transaction tx5 = new Transaction(
                "e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6",
                List.of(input5),
                List.of(output5),
                0L,
                799999,
                new java.util.Date(System.currentTimeMillis() - 7L * 24L * 3600L * 1000L), // 7 days ago
                12,
                150000L,
                true
        );

        TransactionInput input6 = new TransactionInput(
                "f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7z6y5",
                1,
                4294967290L,
                new byte[]{(byte)0x0B, (byte)0x0C},
                ""
        );
        TransactionOutput output6 = new TransactionOutput(
                300000L,
                "3P1oKqQ1oQf2t1Zb1p7b6g9a4n5v8x7y6z",
                new byte[]{(byte)0x76, (byte)0xa9}
        );
        Transaction tx6 = new Transaction(
                "f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7",
                List.of(input6),
                List.of(output6),
                0L,
                800004,
                new java.util.Date(System.currentTimeMillis() - 30L * 60L * 1000L), // 30 minutes ago
                0,
                300000L,
                false
        );

        TransactionInput input7 = new TransactionInput(
                "a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7z6",
                0,
                4294967289L,
                new byte[]{(byte)0x0D, (byte)0x0E},
                ""
        );
        TransactionOutput output7 = new TransactionOutput(
                125000L,
                "1Ez69SnzzmePmZX3WpEzMKTrcBF2gpNQ55",
                new byte[]{(byte)0x76, (byte)0xa9}
        );
        Transaction tx7 = new Transaction(
                "a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8",
                List.of(input7),
                List.of(output7),
                0L,
                800005,
                new java.util.Date(System.currentTimeMillis() - 10L * 3600L * 1000L), // 10 hours ago
                2,
                125000L,
                true
        );

        transactions.clear();
        transactions.add(tx1);
        transactions.add(tx2);
        transactions.add(tx3);
        transactions.add(tx4);
        transactions.add(tx5);
        transactions.add(tx6);
        transactions.add(tx7);
        return CompletableFuture.completedFuture(transactions);
    }
}
