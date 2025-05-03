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

package bisq.trade.mu_sig.messages.seller_as_maker;

import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.events.seller_as_maker.MuSigSellersCooperativeCloseTimeoutEvent;
import bisq.trade.mu_sig.grpc.GrpcStubMock;
import bisq.trade.mu_sig.grpc.SwapTxSignatureRequest;
import bisq.trade.mu_sig.grpc.SwapTxSignatureResponse;
import bisq.trade.mu_sig.messages.MuSigPaymentInitiatedMessage_E;
import bisq.trade.mu_sig.messages.MuSigPaymentReceivedMessage_F;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigPaymentInitiatedMessage_E_Handler extends TradeMessageHandler<MuSigTrade, MuSigPaymentInitiatedMessage_E>
        implements TradeMessageSender<MuSigTrade> {

    public MuSigPaymentInitiatedMessage_E_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        MuSigPaymentInitiatedMessage_E message = (MuSigPaymentInitiatedMessage_E) event;
        verifyMessage(message);

        MuSigTradeParty buyerAsTake = trade.getTaker();

        buyerAsTake.getPartialSignaturesMessage().getSwapTxInputPartialSignature()
                .ifPresent(swapTxInputPartialSignature -> {
                    SwapTxSignatureRequest swapTxSignatureRequest = new SwapTxSignatureRequest(trade.getId(), swapTxInputPartialSignature);
                    GrpcStubMock stub = new GrpcStubMock();
                    SwapTxSignatureResponse swapTxSignatureResponse = stub.signSwapTx(swapTxSignatureRequest);

                    //ClosureType.COOPERATIVE

                    MuSigPaymentReceivedMessage_F response = new MuSigPaymentReceivedMessage_F(StringUtils.createUid(),
                            trade.getId(),
                            trade.getProtocolVersion(),
                            trade.getMyIdentity().getNetworkId(),
                            trade.getPeer().getNetworkId(),
                            swapTxSignatureResponse);
                    sendMessage(response, serviceProvider, trade);

                    serviceProvider.getMuSigTradeService().startCooperativeCloseTimeout(trade, new MuSigSellersCooperativeCloseTimeoutEvent());
                });
    }

    @Override
    protected void verifyMessage(MuSigPaymentInitiatedMessage_E message) {
        super.verifyMessage(message);
    }

    private void commitToModel() {
    }
}
