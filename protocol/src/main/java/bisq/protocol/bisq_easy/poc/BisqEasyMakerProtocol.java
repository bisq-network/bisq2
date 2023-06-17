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

package bisq.protocol.bisq_easy.poc;

import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.persistence.PersistenceClient;
import bisq.protocol.MakerProtocol;
import bisq.protocol.ProtocolModel;
import bisq.protocol.ProtocolStore;
import bisq.protocol.poc.liquid_swap.messages.LiquidSwapFinalizeTxRequest;
import bisq.protocol.poc.liquid_swap.messages.LiquidSwapTakeOfferRequest;
import bisq.protocol.poc.messages.ProtocolMessage;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class BisqEasyMakerProtocol extends MakerProtocol<BisqEasyOffer, BisqEasyMakerProtocolModel> {
    public static BisqEasyMakerProtocol getProtocol(NetworkService networkService,
                                                    PersistenceClient<ProtocolStore> persistenceClient,
                                                    BisqEasyMakerProtocolModel protocolModel,
                                                    NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        return protocolModel.getContract().getOffer().getDirection().isBuy() ?
                new BisqEasyMakerAsBuyerProtocol(networkService, persistenceClient, protocolModel, myNodeIdAndKeyPair) :
                new BisqEasyMakerAsSellerProtocol(networkService, persistenceClient, protocolModel, myNodeIdAndKeyPair);
    }

    public BisqEasyMakerProtocol(NetworkService networkService,
                                 PersistenceClient<ProtocolStore> persistenceClient,
                                 BisqEasyMakerProtocolModel protocolModel,
                                 NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        super(networkService, persistenceClient, protocolModel, myNodeIdAndKeyPair);
    }

/*    @Override
    protected LiquidSwapTakeOfferRequest castTakeOfferRequest(TakeOfferRequest takeOfferRequest) {
        return (LiquidSwapTakeOfferRequest) takeOfferRequest;
    }*/


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof ProtocolMessage) {
            if (((ProtocolMessage) networkMessage).getOfferId().equals(getId())) {
                if (networkMessage instanceof LiquidSwapFinalizeTxRequest) {
                    onLiquidSwapFinalizeTxRequest((LiquidSwapFinalizeTxRequest) networkMessage);
                }
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protocol
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // @Override
    public void onTakeOfferRequest(LiquidSwapTakeOfferRequest request) {
        try {
            verifyPeer();
            verifyLiquidSwapTakeOfferRequest(request);
            processLiquidSwapTakeOfferRequest(request);
            createAndSignTx();
            setupTxListener();
            sendLiquidSwapTakeOfferResponse();
            setState(ProtocolModel.State.PENDING);
        } catch (Throwable t) {
            handleError(t);
        }
    }

    private void onLiquidSwapFinalizeTxRequest(LiquidSwapFinalizeTxRequest request) {
        try {
            //  verifyExpectedMessage(request);
            verifyPeer();
            verifyLiquidSwapFinalizeTxRequest(request);
            processLiquidSwapFinalizeTxRequest(request);
            maybePublishTx();
            onProtocolCompleted();
        } catch (Throwable t) {
            handleError(t);
        }
    }


    @Override
    protected void onContinue() {
        checkArgument(getState() == ProtocolModel.State.PENDING);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks - onTakeOfferRequest
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void verifyLiquidSwapTakeOfferRequest(LiquidSwapTakeOfferRequest request) {
        log.info("verifyTakeOfferRequest");
        checkArgument((getContract().equals(request.getContract())), "Peers contract does not match ours.");
    }

    private void processLiquidSwapTakeOfferRequest(LiquidSwapTakeOfferRequest request) {
        log.info("processLiquidSwapTakeOfferRequest");
    }

    private void createAndSignTx() {
        log.info("createAndSignTx");
    }

    private void setupTxListener() {
        log.info("setupTxListener");
    }

    private void sendLiquidSwapTakeOfferResponse() {
        log.info("sendLiquidSwapTakeOfferResponse");
        setExpectedNextMessageClass(LiquidSwapFinalizeTxRequest.class);
        // sendMessage(new LiquidSwapTakeOfferResponse(getContract()));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Tasks - onLiquidSwapFinalizeTxRequest
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void verifyLiquidSwapFinalizeTxRequest(LiquidSwapFinalizeTxRequest request) {
        log.info("verifyLiquidSwapFinalizeTxRequest");
    }

    private void processLiquidSwapFinalizeTxRequest(LiquidSwapFinalizeTxRequest request) {
        log.info("processLiquidSwapFinalizeTxRequest");
    }

    private void maybePublishTx() {
        log.info("maybePublishTx");
    }
}