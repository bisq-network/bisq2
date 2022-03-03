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
    @Getter
    private Optional<Wallet> wallet = Optional.empty();
    @Getter
    private final Observable<Coin> observableBalanceAsCoin = new Observable<>(Coin.of(0, "BTC"));

    public CompletableFuture<Void> initialize(Path walletsDataDir, WalletConfig walletConfig, String walletPassphrase) {
        return CompletableFuture.runAsync(() -> {
            walletsDataDir.toFile().mkdirs();

            Path bitcoindDataDir = walletsDataDir.resolve("bitcoind"); // directory name for bitcoind wallet
            RpcConfig rpcConfig = createRpcConfigFromWalletConfig(walletConfig, bitcoindDataDir);
            var bitcoindWallet = new BitcoinWallet(bitcoindDataDir, rpcConfig);
            bitcoindWallet.initialize(walletPassphrase);

            wallet = Optional.of(bitcoindWallet);
            log.info("Successfully created wallet at {}", walletsDataDir);
        }).thenRun(this::getBalance);
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> wallet.ifPresent(Wallet::shutdown));
    }

    public CompletableFuture<Long> getBalance() {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            double walletBalance = wallet.getBalance();
            Coin coin = Coin.of(walletBalance, "BTC");
            observableBalanceAsCoin.set(coin);
            long balance = coin.getValue();
            return balance;
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
        return new RpcConfig.Builder()
                .networkType(NetworkType.REGTEST)
                .hostname(walletConfig.getHostname())
                .port(walletConfig.getPort())
                .user(walletConfig.getUser())
                .password(walletConfig.getPassword())
                .walletPath(walletPath)
                .build();
    }

    private Wallet getWalletOrThrowException() {
        return wallet.orElseThrow(() -> new WalletNotInitializedException("Call WalletService.initialize(...) first."));
    }
}
