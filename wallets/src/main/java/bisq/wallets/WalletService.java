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

package bisq.wallets;

import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.wallets.bitcoind.BitcoinWallet;
import bisq.wallets.elementsd.LiquidWallet;
import bisq.wallets.exceptions.WalletNotInitializedException;
import bisq.wallets.model.Transaction;
import bisq.wallets.model.Utxo;
import bisq.wallets.stores.WalletStore;
import bisq.wallets.zmq.ZmqConnection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class WalletService implements PersistenceClient<WalletStore> {

    @Getter
    private final WalletStore persistableStore = new WalletStore();
    @Getter
    private final Persistence<WalletStore> persistence;
    private final Path walletsDataDir;
    private final Optional<WalletConfig> walletConfig;
    @Getter
    private Optional<Wallet> wallet = Optional.empty();

    private Optional<ZmqConnection> zmqConnection = Optional.empty();
    private final Set<String> utxoTxIds = new HashSet<>();

    @Getter
    private final Observable<Coin> observableBalanceAsCoin = new Observable<>(Coin.of(0, "BTC"));

    public WalletService(PersistenceService persistenceService,
                         Path walletsDataDir,
                         Optional<WalletConfig> walletConfig) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.walletsDataDir = walletsDataDir;
        this.walletConfig = walletConfig;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        if (walletConfig.isPresent()) {
            return loadOrCreateWallet(walletConfig.get(), Optional.empty());
        }

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> loadOrCreateWallet(WalletConfig walletConfig, Optional<String> walletPassphrase) {
        if (wallet.isEmpty()) {
            walletsDataDir.toFile().mkdirs();

            Wallet wallet = switch (walletConfig.getWalletBackend()) {
                case BITCOIND -> {
                    BitcoinWallet bitcoindWallet = WalletFactory.createBitcoinWallet(walletConfig,
                            walletsDataDir,
                            persistableStore.getBitcoinWalletStore());
                    bitcoindWallet.initialize(walletPassphrase);

                    ZmqConnection zmqConnection = bitcoindWallet.getZmqConnection();
                    ObservableSet<String> receiveAddresses = bitcoindWallet.getReceiveAddresses();
                    initializeZmqListeners(zmqConnection, receiveAddresses);

                    this.zmqConnection = Optional.of(zmqConnection);
                    yield bitcoindWallet;
                }
                case ELEMENTSD -> {
                    LiquidWallet liquidWallet = WalletFactory.createLiquidWallet(walletConfig,
                            walletsDataDir,
                            persistableStore.getLiquidWalletStore());
                    liquidWallet.initialize(walletPassphrase);

                    ZmqConnection zmqConnection = liquidWallet.getZmqConnection();
                    ObservableSet<String> receiveAddresses = liquidWallet.getReceiveAddresses();
                    initializeZmqListeners(zmqConnection, receiveAddresses);

                    this.zmqConnection = Optional.of(zmqConnection);
                    yield liquidWallet;
                }
            };

            this.wallet = Optional.of(wallet);
            log.info("Successfully created/loaded wallet at {}", walletsDataDir);

            updateBalance();
        }

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            zmqConnection.ifPresent(ZmqConnection::shutdown);
            wallet.ifPresent(Wallet::shutdown);
        });
    }

    private void initializeZmqListeners(ZmqConnection zmqConnection, ObservableSet<String> receiveAddresses) {
        // Update balance when new block gets mined
        zmqConnection.getListeners().registerNewBlockMinedListener(unused -> updateBalance());

        // Update balance if a UTXO is spent
        zmqConnection.getListeners().registerTransactionIdInInputListener(txId -> {
            if (utxoTxIds.contains(txId)) {
                updateBalance();
            }
        });

        // Update balance if a receive address is in tx output
        zmqConnection.getListeners().registerTxOutputAddressesListener(addresses -> {
            boolean receiveAddressInTxOutput = addresses.stream().anyMatch(receiveAddresses::contains);
            if (receiveAddressInTxOutput) {
                updateBalance();
            }
        });
    }

    public boolean isWalletReady() {
        return walletConfig.isPresent() || wallet.isPresent();
    }

    private void updateBalance() {
        CompletableFuture.runAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            double balance = wallet.getBalance();
            Coin coin = Coin.of(balance, "BTC");

            // Balance changed?
            if (!observableBalanceAsCoin.get().equals(coin)) {
                observableBalanceAsCoin.set(coin);

                listUnspent().thenAccept(utxos -> {
                    utxoTxIds.clear();
                    utxos.stream()
                            .map(Utxo::getTxId)
                            .forEach(utxoTxIds::add);
                });
            }
        });
    }

    public CompletableFuture<String> getNewAddress(String label) {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            String receiveAddress = wallet.getNewAddress(AddressType.BECH32, label);

            // getNewAddress updates the receive adresses set
            persist();

            return receiveAddress;
        });
    }

    public ObservableSet<String> getReceiveAddresses() {
        if (wallet.isEmpty()) {
            return new ObservableSet<>();
        }
        return wallet.get().getReceiveAddresses();
    }

    public CompletableFuture<String> signMessage(String address, String message) {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            return wallet.signMessage(address, message);
        });
    }

    public CompletableFuture<List<? extends Transaction>> listTransactions() {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            return wallet.listTransactions();
        });
    }

    public CompletableFuture<List<? extends Utxo>> listUnspent() {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            return wallet.listUnspent();
        });
    }

    public CompletableFuture<String> sendToAddress(String address, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            return wallet.sendToAddress(address, amount);
        });
    }

    private Wallet getWalletOrThrowException() {
        return wallet.orElseThrow(() -> new WalletNotInitializedException("Call WalletService.initialize(...) first."));
    }
}
