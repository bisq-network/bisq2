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

package bisq.protocol.bisq_easy;

import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.persistence.PersistenceClient;
import bisq.protocol.ProtocolModel;
import bisq.protocol.ProtocolStore;
import bisq.protocol.TakerProtocol;
import bisq.protocol.poc.liquid_swap.messages.LiquidSwapFinalizeTxRequest;
import bisq.protocol.poc.liquid_swap.messages.LiquidSwapTakeOfferResponse;
import bisq.protocol.poc.messages.ProtocolMessage;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class BisqEasyTakerProtocol extends TakerProtocol<BisqEasyOffer, BisqEasyTakerProtocolModel> {


    public static BisqEasyTakerProtocol getProtocol(NetworkService networkService,
                                                    PersistenceClient<ProtocolStore> persistenceClient,
                                                    BisqEasyTakerProtocolModel protocolModel,
                                                    NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        return protocolModel.getContract().getOffer().getDirection().isSell() ?
                new BisqEasyTakerAsBuyerProtocol(networkService, persistenceClient, protocolModel, myNodeIdAndKeyPair) :
                new BisqEasyTakerAsSellerProtocol(networkService, persistenceClient, protocolModel, myNodeIdAndKeyPair);
    }


    protected BisqEasyTakerProtocol(NetworkService networkService,
                                    PersistenceClient<ProtocolStore> persistenceClient,
                                    BisqEasyTakerProtocolModel protocolModel,
                                    NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        super(networkService, persistenceClient, protocolModel, myNodeIdAndKeyPair);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof ProtocolMessage) {
            if (((ProtocolMessage) networkMessage).getOfferId().equals(getId())) {
                if (networkMessage instanceof LiquidSwapTakeOfferResponse) {
                    onTakeOfferResponse((LiquidSwapTakeOfferResponse) networkMessage);
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
        // sendMessage(new LiquidSwapTakeOfferRequest(getContract()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks - onTakeOfferResponse
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void verifyLiquidSwapTakeOfferResponse(LiquidSwapTakeOfferResponse response) {
        //   verifyExpectedMessage(response);
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