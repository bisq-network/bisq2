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

package bisq.wallets.electrum;

import bisq.common.observable.ObservableSet;
import bisq.wallets.core.Wallet;
import bisq.wallets.core.model.Transaction;
import bisq.wallets.core.model.Utxo;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.electrum.rpc.responses.ElectrumOnChainHistoryResponse;
import bisq.wallets.json_rpc.JsonRpcResponse;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ElectrumWallet implements Wallet {
    private final Path walletPath;
    private final ElectrumDaemon daemon;

    @Getter
    private final ObservableSet<String> receiveAddresses;

    public ElectrumWallet(Path walletPath, ElectrumDaemon daemon, ObservableSet<String> receiveAddresses) {
        this.walletPath = walletPath;
        this.daemon = daemon;
        this.receiveAddresses = receiveAddresses;
    }

    @Override
    public void initialize(Optional<String> walletPassphrase) {
        String password = walletPassphrase.orElse("");
        if (!doesWalletExist(walletPath)) {
            daemon.create(password);
        }

        daemon.loadWallet(password);
    }

    @Override
    public void shutdown() {
        daemon.stop();
    }

    @Override
    public double getBalance() {
        return daemon.getBalance();
    }

    @Override
    public String getNewAddress() {
        return daemon.getUnusedAddress();
    }

    @Override
    public List<? extends Transaction> listTransactions() {
        ElectrumOnChainHistoryResponse onChainHistoryResponse = daemon.onChainHistory();
        return onChainHistoryResponse.getResult().getTransactions();
    }

    @Override
    public List<? extends Utxo> listUnspent() {
        return daemon.listUnspent()
                .stream()
                .map(JsonRpcResponse::getResult)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public String sendToAddress(Optional<String> passphrase, String address, double amount) {
        String password = passphrase.orElse("");
        String unsignedTx = daemon.payTo(password, address, amount);
        String signedTx = daemon.signTransaction(password, unsignedTx);
        return daemon.broadcast(signedTx);
    }

    @Override
    public String signMessage(Optional<String> passphrase, String address, String message) {
        String password = passphrase.orElse("");
        return daemon.signMessage(password, address, message);
    }

    private boolean doesWalletExist(Path walletPath) {
        return walletPath.toFile().exists();
    }
}
