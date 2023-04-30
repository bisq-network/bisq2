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

package bisq.wallets.elementsd;

import bisq.common.observable.collection.ObservableArray;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.bitcoind.zmq.ZmqWallet;
import bisq.wallets.core.Wallet;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.model.Transaction;
import bisq.wallets.core.model.TransactionInfo;
import bisq.wallets.core.model.Utxo;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

// todo should we use the Wallet interface here? It is not a common wallet for the app but a trade protocol specific one
public class LiquidWallet implements Wallet, ZmqWallet {
    private final String walletName;

    private final ElementsdDaemon daemon;
    private final ElementsdWallet wallet;

    @Getter
    private final LiquidWalletStore liquidWalletStore;

    @Getter
    private final ZmqConnection zmqConnection;

    public LiquidWallet(String walletName,
                        ElementsdDaemon daemon,
                        ElementsdWallet wallet,
                        LiquidWalletStore liquidWalletStore,
                        ZmqConnection zmqConnection) {
        this.walletName = walletName;
        this.daemon = daemon;
        this.wallet = wallet;
        this.liquidWalletStore = liquidWalletStore;
        this.zmqConnection = zmqConnection;
    }

    @Override
    public void initialize(Optional<String> walletPassphrase) {
        daemon.createOrLoadWallet(walletName, walletPassphrase);
    }

    @Override
    public void shutdown() {
        daemon.unloadWallet(walletName);
    }

    @Override
    public double getBalance() {
        return wallet.getLBtcBalance();
    }

    @Override
    public String getUnusedAddress() {
        String newAddress = wallet.getNewAddress(AddressType.BECH32, "");
        liquidWalletStore.getWalletAddresses().add(newAddress);
        return newAddress;
    }

    @Override
    public ObservableArray<String> getWalletAddresses() {
        return liquidWalletStore.getWalletAddresses();
    }

    @Override
    public String signMessage(Optional<String> passphrase, String address, String message) {
        return wallet.signMessage(passphrase, address, message);
    }

    @Override
    public List<? extends TransactionInfo> listTransactions() {
        return wallet.listTransactions(1000);
    }

    @Override
    public List<Transaction> getTransactions() {
        //todo impl
        return null;
    }

    @Override
    public List<? extends Utxo> listUnspent() {
        return wallet.listUnspent();
    }

    @Override
    public String sendToAddress(Optional<String> passphrase, String address, double amount) {
        return wallet.sendLBtcToAddress(passphrase, address, amount);
    }
}
