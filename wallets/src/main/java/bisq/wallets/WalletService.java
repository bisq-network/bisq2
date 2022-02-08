package bisq.wallets;

import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.wallets.bitcoind.BitcoindWallet;
import bisq.wallets.bitcoind.rpc.RpcConfig;
import bisq.wallets.exceptions.WalletNotInitializedException;
import bisq.wallets.model.Transaction;
import bisq.wallets.model.Utxo;
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

    public CompletableFuture<Void> initialize(Path walletsDataDir, RpcConfig rpcConfig, String walletPassphrase) {
        return CompletableFuture.runAsync(() -> {
            walletsDataDir.toFile().mkdirs();
            Path bitcoindDataDir = walletsDataDir.resolve("bitcoind"); // directory name for bitcoind wallet

            var bitcoindWallet = new BitcoindWallet(bitcoindDataDir, rpcConfig);
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

    public CompletableFuture<List<Transaction>> listTransactions() {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            return wallet.listTransactions();
        });
    }

    public CompletableFuture<List<Utxo>> listUnspent() {
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
