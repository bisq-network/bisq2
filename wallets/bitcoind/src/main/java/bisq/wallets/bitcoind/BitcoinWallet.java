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
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.bitcoind.zmq.ZmqWallet;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.Wallet;
import bisq.wallets.core.exceptions.WalletInitializationFailedException;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.model.Transaction;
import bisq.wallets.core.model.Utxo;
import lombok.Getter;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class BitcoinWallet implements Wallet, ZmqWallet {

    private final BitcoindWallet wallet;

    @Getter
    private final ObservableSet<String> receiveAddresses;

    @Getter
    private final ZmqConnection zmqConnection;

    public BitcoinWallet(Path walletPath,
                         RpcConfig rpcConfig,
                         BitcoindDaemon daemon,
                         ObservableSet<String> receiveAddresses,
                         ZmqConnection zmqConnection) {
        this.receiveAddresses = receiveAddresses;
        this.zmqConnection = zmqConnection;

        try {
            wallet = new BitcoindWallet(daemon, rpcConfig, walletPath);
        } catch (MalformedURLException e) {
            throw new WalletInitializationFailedException("Couldn't initialize BitcoinWalletService", e);
        }
    }

    @Override
    public void initialize(Optional<String> walletPassphrase) {
        wallet.initialize(walletPassphrase);
    }

    @Override
    public void shutdown() {
        wallet.shutdown();
    }

    @Override
    public double getBalance() {
        return wallet.getBalance();
    }

    @Override
    public String getNewAddress() {
        String newAddress = wallet.getNewAddress(AddressType.BECH32, "");
        receiveAddresses.add(newAddress);
        return newAddress;
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
        return wallet.sendToAddress(passphrase, address, amount);
    }
}
