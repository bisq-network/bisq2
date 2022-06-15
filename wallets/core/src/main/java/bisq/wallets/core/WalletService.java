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

package bisq.wallets.core;

import bisq.common.observable.ObservableSet;
import bisq.wallets.core.model.Transaction;
import bisq.wallets.core.model.Utxo;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface WalletService {
    CompletableFuture<Boolean> initialize();

    CompletableFuture<Boolean> initializeWallet(RpcConfig rpcConfig, Optional<String> walletPassphrase);

    CompletableFuture<Void> shutdown();

    boolean isWalletReady();

    CompletableFuture<String> getNewAddress();

    ObservableSet<String> getReceiveAddresses();

    CompletableFuture<String> signMessage(Optional<String> passphrase, String address, String message);

    CompletableFuture<List<? extends Transaction>> listTransactions();

    CompletableFuture<List<? extends Utxo>> listUnspent();

    CompletableFuture<String> sendToAddress(Optional<String> passphrase, String address, double amount);
}
