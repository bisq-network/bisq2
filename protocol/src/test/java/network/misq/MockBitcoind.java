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

package network.misq;

import network.misq.wallets.Chain;
import network.misq.wallets.Wallet;

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
    public CompletableFuture<String> getUtxos() {
        return CompletableFuture.completedFuture("getUtxos.mock");
    }

    @Override
    public CompletableFuture<String> sign(String tx) {
        return CompletableFuture.completedFuture("sign.mock");
    }
}