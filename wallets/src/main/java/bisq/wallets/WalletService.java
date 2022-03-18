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
import bisq.wallets.bitcoind.BitcoinWallet;
import bisq.wallets.elementsd.LiquidWallet;
import bisq.wallets.exceptions.WalletNotInitializedException;
import bisq.wallets.model.Transaction;
import bisq.wallets.model.Utxo;
import bisq.wallets.rpc.RpcConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class WalletService {
    private static final String LOCALHOST = "127.0.0.1";

    private final Optional<WalletConfig> walletConfig;
    @Getter
    private Optional<Wallet> wallet = Optional.empty();
    @Getter
    private final Observable<Coin> observableBalanceAsCoin = new Observable<>(Coin.of(0, "BTC"));

    public WalletService(Optional<WalletConfig> walletConfig) {
        this.walletConfig = walletConfig;
    }

    public CompletableFuture<Void> tryAutoInitialization() {
        if (walletConfig.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return initialize(walletConfig.get(), Optional.empty());
    }

    public CompletableFuture<Void> initialize(WalletConfig walletConfig, Optional<String> walletPassphrase) {
        if (wallet.isPresent()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            Path walletsDataDir = walletConfig.getWalletsDataDirPath();
            walletsDataDir.toFile().mkdirs();

            Wallet wallet = switch (walletConfig.getWalletBackend()) {
                case BITCOIND -> {
                    var bitcoindWallet = createBitcoinWallet(walletConfig, walletsDataDir);
                    bitcoindWallet.initialize(walletPassphrase);
                    yield bitcoindWallet;
                }
                case ELEMENTSD -> {
                    var liquidWallet = createLiquidWallet(walletConfig, walletsDataDir);
                    liquidWallet.initialize(walletPassphrase);
                    yield liquidWallet;
                }
            };

            this.wallet = Optional.of(wallet);
            log.info("Successfully created wallet at {}", walletsDataDir);
        }).thenRun(this::getBalance);
    }

    private BitcoinWallet createBitcoinWallet(WalletConfig walletConfig, Path walletsDataDir) {
        Path bitcoindDataDir = walletsDataDir.resolve("bitcoind"); // directory name for bitcoind wallet
        RpcConfig rpcConfig = createRpcConfigFromWalletConfig(walletConfig, bitcoindDataDir);
        return new BitcoinWallet(bitcoindDataDir, rpcConfig);
    }

    private LiquidWallet createLiquidWallet(WalletConfig walletConfig, Path walletsDataDir) {
        Path bitcoindDataDir = walletsDataDir.resolve("elementsd"); // directory name for bitcoind wallet
        RpcConfig rpcConfig = createRpcConfigFromWalletConfig(walletConfig, bitcoindDataDir);
        return new LiquidWallet(bitcoindDataDir, rpcConfig);
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> wallet.ifPresent(Wallet::shutdown));
    }

    public boolean isWalletReady() {
        return walletConfig.isPresent() || wallet.isPresent();
    }

    public CompletableFuture<Long> getBalance() {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            double walletBalance = wallet.getBalance();
            Coin coin = Coin.of(walletBalance, "BTC");
            observableBalanceAsCoin.set(coin);
            return coin.getValue();
        });
    }

    public CompletableFuture<String> getNewAddress(String label) {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            return wallet.getNewAddress(AddressType.BECH32, label);
        });
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

    private RpcConfig createRpcConfigFromWalletConfig(WalletConfig walletConfig, Path walletPath) {
        String hostname = walletConfig.getHostname().orElse(LOCALHOST);
        int port = walletConfig.getPort()
                .orElseGet(() -> walletConfig.getWalletBackend() == WalletBackend.BITCOIND ? 18443 : 7040);

        return new RpcConfig.Builder()
                .networkType(NetworkType.REGTEST)
                .hostname(hostname)
                .port(port)
                .user(walletConfig.getUser())
                .password(walletConfig.getPassword())
                .walletPath(walletPath)
                .build();
    }

    private Wallet getWalletOrThrowException() {
        return wallet.orElseThrow(() -> new WalletNotInitializedException("Call WalletService.initialize(...) first."));
    }
}
