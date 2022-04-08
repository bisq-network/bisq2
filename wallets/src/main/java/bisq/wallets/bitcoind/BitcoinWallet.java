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

import bisq.common.observable.ObservableSet;
import bisq.wallets.AddressType;
import bisq.wallets.Wallet;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.exceptions.WalletInitializationFailedException;
import bisq.wallets.model.Transaction;
import bisq.wallets.model.Utxo;
import bisq.wallets.rpc.RpcClient;
import bisq.wallets.rpc.RpcClientFactory;
import bisq.wallets.rpc.RpcConfig;
import bisq.wallets.stores.BitcoinWalletStore;
import bisq.wallets.zmq.ZmqConnection;
import lombok.Getter;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class BitcoinWallet implements Wallet {
    private final Path walletPath;

    private final BitcoindDaemon daemon;
    private final BitcoindWallet wallet;

    @Getter
    private final BitcoinWalletStore bitcoinWalletStore;

    @Getter
    private final ZmqConnection zmqConnection;

    public BitcoinWallet(Path walletPath,
                         RpcConfig rpcConfig,
                         BitcoindDaemon daemon,
                         BitcoinWalletStore bitcoinWalletStore,
                         ZmqConnection zmqConnection) {
        this.walletPath = walletPath;
        this.daemon = daemon;
        this.bitcoinWalletStore = bitcoinWalletStore;
        this.zmqConnection = zmqConnection;

        try {
            RpcClient rpcClient = RpcClientFactory.create(rpcConfig);
            wallet = new BitcoindWallet(rpcClient);
        } catch (MalformedURLException e) {
            throw new WalletInitializationFailedException("Couldn't initialize WalletService", e);
        }
    }

    @Override
    public void initialize(Optional<String> walletPassphrase) {
        daemon.createOrLoadWallet(walletPath, walletPassphrase);
        wallet.walletPassphrase(walletPassphrase, BitcoindWallet.DEFAULT_WALLET_TIMEOUT);
    }

    @Override
    public void shutdown() {
        daemon.unloadWallet(walletPath);
    }

    @Override
    public double getBalance() {
        return wallet.getBalance();
    }

    @Override
    public String getNewAddress(AddressType addressType, String label) {
        String newAddress = wallet.getNewAddress(addressType, label);
        bitcoinWalletStore.getReceiveAddresses().add(newAddress);
        return newAddress;
    }

    @Override
    public ObservableSet<String> getReceiveAddresses() {
        return bitcoinWalletStore.getReceiveAddresses();
    }

    @Override
    public String signMessage(String address, String message) {
        return wallet.signMessage(address, message);
    }

    @Override
    public List<? extends Transaction> listTransactions() {
        return wallet.listTransactions(1000);
    }

    @Override
    public List<? extends Utxo> listUnspent() {
        return wallet.listUnspent();
    }

    @Override
    public String sendToAddress(String address, double amount) {
        return wallet.sendToAddress(address, amount);
    }
}
