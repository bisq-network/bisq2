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

import bisq.common.observable.ObservableSet;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.bitcoind.zmq.ZmqWallet;
import bisq.wallets.core.Wallet;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.model.Transaction;
import bisq.wallets.core.model.Utxo;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class LiquidWallet implements Wallet, ZmqWallet {
    private final Path walletPath;

    private final ElementsdDaemon daemon;
    private final ElementsdWallet wallet;

    @Getter
    private final LiquidWalletStore liquidWalletStore;

    @Getter
    private final ZmqConnection zmqConnection;

    public LiquidWallet(Path walletPath,
                        ElementsdDaemon daemon,
                        ElementsdWallet wallet,
                        LiquidWalletStore liquidWalletStore,
                        ZmqConnection zmqConnection) {
        this.walletPath = walletPath;
        this.daemon = daemon;
        this.wallet = wallet;
        this.liquidWalletStore = liquidWalletStore;
        this.zmqConnection = zmqConnection;
    }

    @Override
    public void initialize(Optional<String> walletPassphrase) {
        daemon.createOrLoadWallet(walletPath, walletPassphrase);
    }

    @Override
    public void shutdown() {
        daemon.unloadWallet(walletPath);
    }

    @Override
    public double getBalance() {
        return wallet.getLBtcBalance();
    }

    @Override
    public String getNewAddress() {
        String newAddress = wallet.getNewAddress(AddressType.BECH32, "");
        liquidWalletStore.getReceiveAddresses().add(newAddress);
        return newAddress;
    }

    @Override
    public ObservableSet<String> getReceiveAddresses() {
        return liquidWalletStore.getReceiveAddresses();
    }

    @Override
    public String signMessage(Optional<String> passphrase, String address, String message) {
        return wallet.signMessage(passphrase, address, message);
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
    public String sendToAddress(Optional<String> passphrase, String address, double amount) {
        return wallet.sendLBtcToAddress(passphrase, address, amount);
    }
}
