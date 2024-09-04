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

import bisq.common.application.Service;
import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.wallets.core.model.Transaction;
import bisq.wallets.core.model.TransactionInfo;
import bisq.wallets.core.model.Utxo;
import bisq.wallets.json_rpc.RpcConfig;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface WalletService extends Service {

    CompletableFuture<Boolean> initializeWallet(RpcConfig rpcConfig, Optional<String> walletPassphrase);

    boolean isWalletReady();

    CompletableFuture<String> getUnusedAddress();

    ObservableSet<String> getWalletAddresses();

    CompletableFuture<ObservableSet<String>> requestWalletAddresses();

    CompletableFuture<List<? extends TransactionInfo>> listTransactions();

    CompletableFuture<List<? extends Utxo>> listUnspent();

    CompletableFuture<String> sendToAddress(Optional<String> passphrase, String address, double amount);

    CompletableFuture<Boolean> isWalletEncrypted();

    CompletableFuture<Coin> requestBalance();

    Observable<Coin> getBalance();

    ObservableSet<Transaction> getTransactions();

    CompletableFuture<ObservableSet<Transaction>> requestTransactions();
}
