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

import bisq.wallets.AddressType;
import bisq.wallets.Wallet;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import bisq.wallets.exceptions.WalletInitializationFailedException;
import bisq.wallets.model.Transaction;
import bisq.wallets.model.Utxo;
import bisq.wallets.rpc.RpcClient;
import bisq.wallets.rpc.RpcClientFactory;
import bisq.wallets.rpc.RpcConfig;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class LiquidWallet implements Wallet {
    private final Path walletPath;

    private final ElementsdDaemon daemon;
    private final ElementsdWallet wallet;

    public LiquidWallet(Path walletPath, RpcConfig rpcConfig) {
        this.walletPath = walletPath;

        try {
            RpcClient rpcClient = RpcClientFactory.create(rpcConfig);
            daemon = new ElementsdDaemon(rpcClient);
            wallet = new ElementsdWallet(rpcClient);
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
        return wallet.getLBtcBalance();
    }

    @Override
    public String getNewAddress(AddressType addressType, String label) {
        return wallet.getNewAddress(addressType, label);
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
        return wallet.sendLBtcToAddress(address, amount);
    }
}
