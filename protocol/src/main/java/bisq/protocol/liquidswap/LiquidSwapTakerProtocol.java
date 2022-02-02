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

package bisq.protocol.liquidswap;

import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.persistence.PersistenceClient;
import bisq.protocol.ProtocolModel;
import bisq.protocol.ProtocolStore;
import bisq.protocol.TakerProtocol;
import bisq.protocol.TakerProtocolModel;
import bisq.protocol.liquidswap.messages.LiquidSwapFinalizeTxRequest;
import bisq.protocol.liquidswap.messages.LiquidSwapTakeOfferRequest;
import bisq.protocol.liquidswap.messages.LiquidSwapTakeOfferResponse;
import bisq.protocol.messages.ProtocolMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class LiquidSwapTakerProtocol extends TakerProtocol<TakerProtocolModel> {


    public static TakerProtocol<TakerProtocolModel> getProtocol(NetworkService networkService,
                                                                PersistenceClient<ProtocolStore> persistenceClient,
                                                                TakerProtocolModel protocolModel,
                                                                NetworkIdWithKeyPair makerNetworkIdWithKeyPair) {
        return protocolModel.getContract().getOffer().getDirection().mirror().isBuy() ?
                new LiquidSwapTakerAsBuyerProtocol(networkService, persistenceClient, protocolModel, makerNetworkIdWithKeyPair)
                : new LiquidSwapTakerAsSellerProtocol(networkService, persistenceClient, protocolModel, makerNetworkIdWithKeyPair);
    }


    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public LiquidSwapTakerProtocol(NetworkService networkService,
                                   PersistenceClient<ProtocolStore> persistenceClient,
                                   TakerProtocolModel protocolModel,
                                   NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        super(networkService, persistenceClient, protocolModel, myNodeIdAndKeyPair);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message) {
        if (message instanceof ProtocolMessage protocolMessage) {
            if (protocolMessage.getOfferId().equals(getId())) {
                if (message instanceof LiquidSwapTakeOfferResponse liquidSwapTakeOfferResponse) {
                    onTakeOfferResponse(liquidSwapTakeOfferResponse);
                }
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protocol
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void takeOffer() {
        try {
            verifyPeer();
            createInputsAndChange();
            sendLiquidSwapTakeOfferRequest();
            setState(ProtocolModel.State.PENDING);
        } catch (Throwable t) {
            handleError(t);
        }
    }

    private void onTakeOfferResponse(LiquidSwapTakeOfferResponse response) {
        verifyPeer();
        verifyLiquidSwapTakeOfferResponse(response);
        processLiquidSwapTakeOfferResponse(response);
        createAndSignFinalizedTx();
        publishTx();
        publishTradeStatistics();
        sendLiquidSwapFinalizeTxRequest();
        onProtocolCompleted();
    }

    @Override
    protected void onContinue() {
        checkArgument(getState() == ProtocolModel.State.PENDING);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks - takeOffer
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void createInputsAndChange() {
        log.info("createInputsAndChange");
    }

    private void sendLiquidSwapTakeOfferRequest() {
        log.info("sendLiquidSwapTakeOfferRequest");
        setExpectedNextMessageClass(LiquidSwapTakeOfferResponse.class);
        sendMessage(new LiquidSwapTakeOfferRequest(getContract()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks - onTakeOfferResponse
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void verifyLiquidSwapTakeOfferResponse(LiquidSwapTakeOfferResponse response) {
        verifyExpectedMessage(response);
        log.info("verifyLiquidSwapTakeOfferResponse");
    }

    private void processLiquidSwapTakeOfferResponse(LiquidSwapTakeOfferResponse response) {
        log.info("processLiquidSwapTakeOfferResponse");
    }

    private void createAndSignFinalizedTx() {
        log.info("createAndSignFinalizedTx");
    }

    private void publishTx() {
        log.info("publishTx");
    }

    private void publishTradeStatistics() {
        log.info("publishTradeStatistics");
    }

    private void sendLiquidSwapFinalizeTxRequest() {
        log.info("sendLiquidSwapFinalizeTxRequest");
        sendMessage(new LiquidSwapFinalizeTxRequest(getId()));
    }
}