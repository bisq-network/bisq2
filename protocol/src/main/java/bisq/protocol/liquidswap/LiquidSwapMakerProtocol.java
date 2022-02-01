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

import bisq.contract.Contract;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.persistence.PersistenceService;
import bisq.protocol.MakerProtocol;
import bisq.protocol.MakerProtocolStore;
import bisq.protocol.ProtocolStore;
import bisq.protocol.liquidswap.messages.LiquidSwapFinalizeTxRequest;
import bisq.protocol.liquidswap.messages.LiquidSwapTakeOfferRequest;
import bisq.protocol.liquidswap.messages.LiquidSwapTakeOfferResponse;
import bisq.protocol.messages.TakeOfferRequest;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class LiquidSwapMakerProtocol extends MakerProtocol<MakerProtocolStore, LiquidSwapTakeOfferRequest> {
    public static MakerProtocol<MakerProtocolStore, LiquidSwapTakeOfferRequest> getProtocol(NetworkService networkService,
                                                                                            PersistenceService persistenceService,
                                                                                            Contract contract,
                                                                                            NetworkIdWithKeyPair makerNetworkIdWithKeyPair) {
        return contract.getOffer().getDirection().isBuy() ?
                new LiquidSwapMakerAsBuyerProtocol(networkService, persistenceService, contract, makerNetworkIdWithKeyPair)
                : new LiquidSwapMakerAsSellerProtocol(networkService, persistenceService, contract, makerNetworkIdWithKeyPair);
    }

    public LiquidSwapMakerProtocol(NetworkService networkService,
                                   PersistenceService persistenceService,
                                   Contract contract,
                                   NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        super(networkService, persistenceService, contract, myNodeIdAndKeyPair);
    }

    @Override
    protected LiquidSwapTakeOfferRequest castTakeOfferRequest(TakeOfferRequest takeOfferRequest) {
        return (LiquidSwapTakeOfferRequest) takeOfferRequest;
    }

    @Override
    protected MakerProtocolStore createProtocolStore(Contract contract) {
        return new MakerProtocolStore(contract);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message) {
        if (message instanceof LiquidSwapFinalizeTxRequest liquidSwapFinalizeTxRequest) {
            onLiquidSwapFinalizeTxRequest(liquidSwapFinalizeTxRequest);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protocol
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onTakeOfferRequest(LiquidSwapTakeOfferRequest request) {
        try {
            verifyPeer();
            verifyLiquidSwapTakeOfferRequest(request);
            processLiquidSwapTakeOfferRequest(request);
            createAndSignTx();
            setupTxListener();
            sendLiquidSwapTakeOfferResponse();
            setState(ProtocolStore.State.PENDING);
        } catch (Throwable t) {
            handleError(t);
        }
    }

    private void onLiquidSwapFinalizeTxRequest(LiquidSwapFinalizeTxRequest request) {
        try {
            verifyExpectedMessage(request);
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
        checkArgument(getState() == ProtocolStore.State.PENDING);
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
        sendMessage(new LiquidSwapTakeOfferResponse(getContract()));
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