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

package bisq.protocol;

import bisq.contract.Contract;
import bisq.contract.Party;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.MessageListener;
import lombok.Getter;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines the protocol for executing a contract.
 */
public abstract class Protocol implements MessageListener {

    public interface Listener {
        void onStateChange(State state);
    }

    public interface State {
    }

    protected final NetworkService networkService;
    protected final NetworkIdWithKeyPair networkIdWithKeyPair;
    @Getter
    protected final Contract contract;
    protected final Party maker;
    protected final Set<Listener> listeners = ConcurrentHashMap.newKeySet();

    public Protocol(NetworkService networkService, NetworkIdWithKeyPair networkIdWithKeyPair, Contract contract) {
        this.networkService = networkService;
        this.networkIdWithKeyPair = networkIdWithKeyPair;
        this.contract = contract;
        maker = contract.getMaker();
    }

    public abstract CompletableFuture<Boolean> start();

    public Protocol addListener(Listener listener) {
        listeners.add(listener);
        return this;
    }

    protected void setState(State state) {
        listeners.forEach(e -> e.onStateChange(state));
    }
}
