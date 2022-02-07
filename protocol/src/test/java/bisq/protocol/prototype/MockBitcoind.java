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

package bisq.protocol.prototype;

import bisq.wallets.*;
import bisq.wallets.model.Transaction;
import bisq.wallets.model.Utxo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MockBitcoind implements Chain, Wallet {
    @Override
    public void addListener(Listener listener) {
    }

    @Override
    public CompletableFuture<String> broadcast(String tx) {
        return CompletableFuture.completedFuture("broadcast.mock");
    }

    @Override
    public CompletableFuture<Boolean> isInMemPool(String tx) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void initialize(String walletPassphrase) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public double getBalance() {
        return 0.;
    }

    @Override
    public String getNewAddress(AddressType addressType, String label) {
        return null;
    }

    @Override
    public List<Transaction> listTransactions() {
        return null;
    }

    @Override
    public String signMessage(String address, String message) {
        return null;
    }

    @Override
    public List<Utxo> listUnspent() {
        return null;
    }

    @Override
    public String sendToAddress(String address, double amount) {
        return null;
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