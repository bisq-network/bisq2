package bisq.wallets;

import bisq.wallets.bitcoind.BitcoindWallet;
import bisq.wallets.bitcoind.rpc.RpcConfig;
import bisq.wallets.exceptions.WalletNotInitializedException;
import bisq.wallets.model.Transaction;
import bisq.wallets.model.Utxo;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

public class WalletService {
    public interface Listener {
        void onBalanceChanged(double newBalance);
    }

    private Optional<Wallet> wallet = Optional.empty();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public CompletableFuture<Void> initialize(Path walletsDataDir, RpcConfig rpcConfig, String walletPassphrase) {
        return CompletableFuture.runAsync(() -> {
            walletsDataDir.toFile().mkdirs();
            Path bitcoindDataDir = walletsDataDir.resolve("bitcoind");

            var bitcoindWallet = new BitcoindWallet(bitcoindDataDir, rpcConfig);
            bitcoindWallet.initialize(walletPassphrase);

            wallet = Optional.of(bitcoindWallet);
        }).thenRun(this::getBalance);
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> wallet.ifPresent(Wallet::shutdown));
    }

    public CompletableFuture<Double> getBalance() {
        return CompletableFuture.supplyAsync(() -> {
            Wallet wallet = getWalletOrThrowException();
            double balance = wallet.getBalance();

            listeners.forEach(listener -> listener.onBalanceChanged(balance));
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

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private Wallet getWalletOrThrowException() {
        return wallet.orElseThrow(() -> new WalletNotInitializedException("Call WalletService.initialize(...) first."));
    }
}
