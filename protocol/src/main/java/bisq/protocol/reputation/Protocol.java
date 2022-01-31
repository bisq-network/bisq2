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

package bisq.protocol.reputation;

import bisq.contract.Contract;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.persistence.PersistenceService;
import bisq.protocol.ProtocolService;
import bisq.protocol.reputation.messages.TakeOfferRequest;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.runAsync;

public abstract class Protocol implements MessageListener, Serializable {

    //todo move to maker protocol 
    public abstract void onTakeOfferRequest(TakeOfferRequest takeOfferRequest);

    public interface Listener {
        void onStateChanged(State state);
    }

    interface State {
        int ordinal();

        String name();
    }


    protected final NetworkService networkService;
    protected final PersistenceService persistenceService;
    protected final Contract contract;
    protected final NetworkIdWithKeyPair myNodeIdAndKeyPair;
    protected final AtomicReference<State> state = new AtomicReference<>();
    protected final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public Protocol(NetworkService networkService,
                    PersistenceService persistenceService,
                    Contract contract,
                    NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        this.networkService = networkService;
        this.persistenceService = persistenceService;
        this.contract = contract;
        this.myNodeIdAndKeyPair = myNodeIdAndKeyPair;
        networkService.addMessageListener(this);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
    public String getId() {
        return contract.getOffer().getId();
    }
    protected void setState(State newState) {
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        runAsync(() -> listeners.forEach(e -> e.onStateChanged(newState)), ProtocolService.DISPATCHER);
    }
}