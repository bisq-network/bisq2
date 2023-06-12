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

package bisq.wallets.bitcoind;

import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.persistence.PersistableStore;
import bisq.persistence.PersistenceClient;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.bitcoind.zmq.ZmqWallet;
import bisq.wallets.core.Wallet;
import bisq.wallets.core.WalletService;
import bisq.wallets.core.exceptions.WalletNotInitializedException;
import bisq.wallets.core.model.Transaction;
import bisq.wallets.core.model.TransactionInfo;
import bisq.wallets.core.model.Utxo;
import bisq.wallets.json_rpc.RpcConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class AbstractBitcoindWalletService<T extends Wallet & ZmqWallet,
        S extends PersistableStore<S>> implements WalletService, PersistenceClient<S> {

    public static Optional<RpcConfig> getOptionalRegtestConfig(boolean isRegtest, int port) {
        if (isRegtest) {
            return Optional.of(RpcConfig.builder()
                    .hostname("localhost")
                    .port(port)
                    .user("bisq")
                    .password("bisq")
                    .build());
        } else {
            return Optional.empty();
        }
    }

    private final String currencyCode;
    protected final Optional<RpcConfig> optionalRpcConfig;
    protected final String walletName;
    @Getter
    protected final Observable<Coin> observableBalanceAsCoin;

    @Getter
    protected Optional<T> wallet = Optional.empty();
    protected Optional<ZmqConnection> zmqConnection = Optional.empty();
    protected final Set<String> utxoTxIds = new HashSet<>();
    @Getter
    protected final ObservableSet<String> walletAddresses = new ObservableSet<>();
    @Getter
    private final ObservableSet<Transaction> transactions = new ObservableSet<>();

    public AbstractBitcoindWalletService(String currencyCode,
                                         Optional<RpcConfig> optionalRpcConfig,
                                         String walletName) {
        this.currencyCode = currencyCode;
        this.optionalRpcConfig = optionalRpcConfig;
        this.walletName = walletName;

        observableBalanceAsCoin = new Observable<>(Coin.fromValue(0, currencyCode));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        boolean isSuccess = verifyRpcConfigAndCreateWallet(optionalRpcConfig);
        // No cmd line arguments passed, so try to use saved configuration
        if (!isSuccess) {
            Optional<RpcConfig> optionalRpcConfig = getRpcConfigFromPersistableStore();
            verifyRpcConfigAndCreateWallet(optionalRpcConfig);
        }

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.supplyAsync(() -> {
            zmqConnection.ifPresent(ZmqConnection::shutdown);
            wallet.ifPresent(Wallet::shutdown);
            return true;
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // WalletService
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initializeWallet(RpcConfig rpcConfig, Optional<String> walletPassphrase) {
        if (wallet.isEmpty()) {
            boolean isSuccess = verifyRpcConfigAndCreateWallet(Optional.of(rpcConfig));

            if (isSuccess) {
                // Persist valid Rpc credentials
                persistRpcConfig(rpcConfig);
            } else {
                return CompletableFuture.completedFuture(false);
            }
        }

        T wallet = createWallet(rpcConfig);
        wallet.initialize(walletPassphrase);

        ZmqConnection zmqConnection = wallet.getZmqConnection();
        walletAddresses.addAll(wallet.getWalletAddresses());
        initializeZmqListeners(zmqConnection, walletAddresses);

        this.zmqConnection = Optional.of(zmqConnection);
        log.info("Successfully created/loaded wallet at {}", walletName);

        updateBalance();

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public boolean isWalletReady() {
        return optionalRpcConfig.isPresent() || wallet.isPresent();
    }

    @Override
    public CompletableFuture<String> getUnusedAddress() {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            String receiveAddress = wallet.getUnusedAddress();

            // getNewAddress updates the receive adresses set
            persist();

            return receiveAddress;
        });
    }

    @Override
    public CompletableFuture<ObservableSet<String>> requestWalletAddresses() {
        if (wallet.isEmpty()) {
            return CompletableFuture.completedFuture(walletAddresses);
        } else {
            return CompletableFuture.supplyAsync(() -> {
                walletAddresses.addAll(wallet.get().getWalletAddresses());
                return walletAddresses;
            });
        }
    }

    @Override
    public CompletableFuture<List<? extends TransactionInfo>> listTransactions() {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            return wallet.listTransactions();
        });
    }

    @Override
    public CompletableFuture<List<? extends Utxo>> listUnspent() {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            return wallet.listUnspent();
        });
    }

    @Override
    public CompletableFuture<String> sendToAddress(Optional<String> passphrase, String address, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            return wallet.sendToAddress(passphrase, address, amount);
        });
    }

    @Override
    public CompletableFuture<ObservableSet<Transaction>> requestTransactions() {
        return CompletableFuture.supplyAsync(() -> {
            //todo implement 
            // transactions.addAll(getWalletOrThrowException().getTransactions());
            return transactions;
        });
    }

    @Override
    public CompletableFuture<Boolean> isWalletEncrypted() {
        //todo implement 
        return CompletableFuture.supplyAsync(() -> true);
    }

    protected void initializeZmqListeners(ZmqConnection zmqConnection, Set<String> walletAddresses) {
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
            boolean receiveAddressInTxOutput = addresses.stream().anyMatch(walletAddresses::contains);
            if (receiveAddressInTxOutput) {
                updateBalance();
            }
        });
    }

    protected abstract void persistRpcConfig(RpcConfig rpcConfig);

    protected Wallet getWalletOrThrowException() {
        return wallet.orElseThrow(() -> new WalletNotInitializedException("Call WalletService.initialize(...) first."));
    }

    protected abstract Optional<RpcConfig> getRpcConfigFromPersistableStore();

    protected abstract T createWallet(RpcConfig rpcConfig);

    private boolean verifyRpcConfigAndCreateWallet(Optional<RpcConfig> optionalRpcConfig) {
        if (optionalRpcConfig.isEmpty()) return false;

        RpcConfig rpcConfig = optionalRpcConfig.get();
        boolean isValidRpcConfig = BitcoindDaemon.verifyRpcConfig(rpcConfig);
        if (isValidRpcConfig) {
            T walletImpl = createWallet(rpcConfig);
            wallet = Optional.of(walletImpl);
            return true;
        }

        return false;
    }

    private void updateBalance() {
        CompletableFuture.runAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            double balance = wallet.getBalance();
            Coin coin = Coin.fromFaceValue(balance, currencyCode);

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
}
