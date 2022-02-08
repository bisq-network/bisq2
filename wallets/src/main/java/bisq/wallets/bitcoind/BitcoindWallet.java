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

import bisq.wallets.*;
import bisq.wallets.bitcoind.responses.ListTransactionsResponseEntry;
import bisq.wallets.bitcoind.responses.ListUnspentResponseEntry;
import bisq.wallets.bitcoind.rpc.*;
import bisq.wallets.exceptions.InvalidRpcCredentialsException;
import bisq.wallets.exceptions.RpcCallFailureException;
import bisq.wallets.exceptions.WalletInitializationFailedException;
import bisq.wallets.model.Transaction;
import bisq.wallets.model.Utxo;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BitcoindWallet implements Wallet {
    private final Path walletPath;

    private final BitcoindChainBackend chainBackend;
    private final BitcoindWalletBackend walletBackend;

    public BitcoindWallet(Path walletPath, RpcConfig rpcConfig) {
        this.walletPath = walletPath;

        try {
            var chainRpcClient = new RpcClient(rpcConfig);
            chainBackend = new BitcoindChainBackend(chainRpcClient);

            var walletRpcConfig = new WalletRpcConfig(rpcConfig, walletPath);
            var walletRpcClient = new RpcClient(walletRpcConfig);
            walletBackend = new BitcoindWalletBackend(walletRpcClient);
        } catch (MalformedURLException e) {
            throw new WalletInitializationFailedException("Couldn't initialize WalletService", e);
        }
    }

    @Override
    public void initialize(String walletPassphrase) {
        try {
            chainBackend.createOrLoadWallet(walletPath, walletPassphrase, false, false);
            walletBackend.walletPassphrase(walletPassphrase, BitcoindWalletBackend.DEFAULT_WALLET_TIMEOUT);
        } catch (RpcCallFailureException e) {
            throw new InvalidRpcCredentialsException(e);
        }
    }

    @Override
    public void shutdown() {
        chainBackend.unloadWallet(walletPath);
    }

    @Override
    public double getBalance() {
        return walletBackend.getBalance();
    }

    @Override
    public String getNewAddress(AddressType addressType, String label) {
        return walletBackend.getNewAddress(addressType, label);
    }

    @Override
    public String signMessage(String address, String message) {
        return walletBackend.signMessage(address, message);
    }

    @Override
    public List<Transaction> listTransactions() {
        return walletBackend.listTransactions(1000)
                .stream()
                .map(ListTransactionsResponseEntry::toTransaction)
                .toList();
    }

    @Override
    public List<Utxo> listUnspent() {
        return walletBackend.listUnspent()
                .stream()
                .map(ListUnspentResponseEntry::toUtxo)
                .toList();
    }

    @Override
    public String sendToAddress(String address, double amount) {
        return walletBackend.sendToAddress(address, amount);
    }

    @Override
    public CompletableFuture<String> getUtxos() {
        return null;
    }

    @Override
    public CompletableFuture<String> sign(String tx) {
        return null;
    }
}
