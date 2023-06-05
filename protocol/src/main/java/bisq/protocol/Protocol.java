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

import bisq.common.proto.Proto;
import bisq.contract.Contract;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.offer.Offer;
import bisq.persistence.PersistenceClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Protocol<T extends Offer, M extends ProtocolModel<T>> implements MessageListener, Proto {
    protected final NetworkService networkService;
    private final PersistenceClient<ProtocolStore> persistenceClient;
    protected final NetworkIdWithKeyPair myNodeIdAndKeyPair;
    @Getter
    private final M protocolModel;

    public Protocol(NetworkService networkService,
                    PersistenceClient<ProtocolStore> persistenceClient,
                    M protocolModel,
                    NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        this.networkService = networkService;
        this.persistenceClient = persistenceClient;
        this.myNodeIdAndKeyPair = myNodeIdAndKeyPair;
        this.protocolModel = protocolModel;

        if (getState() == ProtocolModel.State.IDLE) {
            networkService.addMessageListener(this);
        }
        persistenceClient.persist();
    }

    protected abstract void onContinue();

    protected abstract NetworkId getPeersNetworkId();

    public Contract<T> getContract() {
        return protocolModel.getContract();
    }

    public String getId() {
        return getSwapOffer().getId();
    }

    private T getSwapOffer() {
        return getContract().getOffer();
    }

    protected void setState(ProtocolModel.State newState) {
        protocolModel.setStateAsObservable(newState);
        persistenceClient.persist();
    }

    protected ProtocolModel.State getState() {
        return protocolModel.getState();
    }

    protected void onProtocolCompleted() {
        networkService.removeMessageListener(this);
        protocolModel.setStateAsObservable(ProtocolModel.State.COMPLETED);
        persistenceClient.persist();
    }

    protected void onProtocolFailed() {
        networkService.removeMessageListener(this);
        protocolModel.setStateAsObservable(ProtocolModel.State.FAILED);
        persistenceClient.persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks
    ///////////////////////////////////////////////////////////////////////////////////////////////////

   /* protected void verifyExpectedMessage(NetworkMessage networkMessage) {
        checkArgument(protocolModel.getExpectedNextMessageClass().equals(networkMessage.getClass()),
                "Incorrect response message. Received " + networkMessage.getClass().getSimpleName() +
                        " but expected " + protocolModel.getExpectedNextMessageClass().getSimpleName());
    }*/

    protected void verifyPeer() {
        // throw if peer verification fails
    }

    protected void sendMessage(NetworkMessage networkMessage) {
        networkService.confidentialSend(networkMessage, getPeersNetworkId(), myNodeIdAndKeyPair)
                .whenComplete((resultMap, throwable) -> {
                    if (throwable == null) {
                        log.info("Sent successfully {} to {}", networkMessage.getClass().getSimpleName(), getPeersNetworkId());
                        persistenceClient.persist();
                    } else {
                        handleSendMessageError(throwable, networkMessage);
                    }
                });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Handle errors
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected void handleSendMessageError(Throwable exception, NetworkMessage networkMessage) {
        handleError(exception, "Error at sending " + networkMessage);
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

    protected void setExpectedNextMessageClass(Class<? extends NetworkMessage> value) {
        protocolModel.setExpectedNextMessageClass(value);
        persistenceClient.persist();
    }

    public boolean isPending() {
        return protocolModel.isPending();
    }

    public boolean isCompleted() {
        return protocolModel.isCompleted();
    }

    public boolean isFailed() {
        return protocolModel.isFailed();
    }
}