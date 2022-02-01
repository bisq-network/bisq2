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
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public abstract class Protocol<T extends ProtocolStore<T>> implements MessageListener, Serializable, PersistenceClient<T> {
    public interface Listener {
        void onStateChanged(ProtocolStore.State state);
    }

/*    public interface State {
        int ordinal();

        String name();
    }*/

    protected final NetworkService networkService;
    protected final NetworkIdWithKeyPair myNodeIdAndKeyPair;
    @Getter
    private final T persistableStore;
    @Getter
    private final Persistence<T> persistence;

    protected final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public Protocol(NetworkService networkService,
                    PersistenceService persistenceService,
                    Contract contract,
                    NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        this.networkService = networkService;
        this.myNodeIdAndKeyPair = myNodeIdAndKeyPair;

        persistableStore = createProtocolStore(contract);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        networkService.addMessageListener(this);
        persist();
    }

    protected abstract void onContinue();

    protected abstract T createProtocolStore(Contract contract);

    protected abstract NetworkId getPeersNetworkId();

    public Contract getContract() {
        return persistableStore.getContract();
    }

    public String getId() {
        return getContract().getOffer().getId();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    protected void setState(ProtocolStore.State newState) {
        persistableStore.setState(newState);
        persist();
        runAsync(() -> listeners.forEach(e -> e.onStateChanged(persistableStore.getState())), ProtocolService.DISPATCHER);
    }

    protected ProtocolStore.State getState() {
        return persistableStore.getState();
    }

    protected void onProtocolCompleted() {
        networkService.removeMessageListener(this);
        persistableStore.setState(ProtocolStore.State.COMPLETED);
        persist();
    }

    protected void onProtocolFailed() {
        networkService.removeMessageListener(this);
        persistableStore.setState(ProtocolStore.State.FAILED);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected void verifyExpectedMessage(Message message) {
        checkArgument(persistableStore.getExpectedNextMessageClass().equals(message.getClass()),
                "Incorrect response message. Received " + message.getClass().getSimpleName() +
                        " but expected " + persistableStore.getExpectedNextMessageClass().getSimpleName());
    }

    protected void verifyPeer() {
        // throw if peer verification fails
    }

    protected void sendMessage(Message message) {
        networkService.sendMessage(message, getPeersNetworkId(), myNodeIdAndKeyPair)
                .whenComplete((resultMap, throwable) -> {
                    if (throwable == null) {
                        log.info("Sent successfully {} to {}", message.getClass().getSimpleName(), getPeersNetworkId());
                        persist();
                    } else {
                        handleSendMessageError(throwable, message);
                    }
                });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Handle errors
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected void handleSendMessageError(Throwable exception, Message message) {
        handleError(exception, "Error at sending " + message);
    }

    protected void handleError(Throwable exception) {
        handleError(exception, exception.getMessage());
    }

    protected void handleError(String errorMessage) {
        log.error(errorMessage);
        onProtocolFailed();
    }

    protected void handleError(Throwable exception, String errorMessage) {
        log.error(errorMessage);
        exception.printStackTrace();
        onProtocolFailed();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Delegates
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected void setExpectedNextMessageClass(Class<? extends Message> value) {
        persistableStore.setExpectedNextMessageClass(value);
        persist();
    }
}