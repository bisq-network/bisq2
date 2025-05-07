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

package bisq.trade.mu_sig.messages.network.handler.buyer_as_taker;

import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import bisq.trade.mu_sig.messages.grpc.SwapTxSignatureResponse;
import bisq.trade.mu_sig.messages.network.MuSigCooperativeClosureMessage_G;
import bisq.trade.mu_sig.messages.network.MuSigPaymentReceivedMessage_F;
import bisq.trade.protobuf.CloseTradeRequest;
import bisq.trade.protobuf.MusigGrpc;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigPaymentReceivedMessage_F_Handler extends TradeMessageHandler<MuSigTrade, MuSigPaymentReceivedMessage_F>
        implements TradeMessageSender<MuSigTrade> {

    public MuSigPaymentReceivedMessage_F_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        MuSigPaymentReceivedMessage_F message = (MuSigPaymentReceivedMessage_F) event;
        verifyMessage(message);

        serviceProvider.getMuSigTradeService().stopCooperativeCloseTimeout(trade);

        // ClosureType.COOPERATIVE
        // *** BUYER CLOSES TRADE ***
        SwapTxSignatureResponse sellerSwapTxSignatureResponse = message.getSwapTxSignatureResponse();
        MusigGrpc.MusigBlockingStub stub = serviceProvider.getMuSigTradeService().getMusigStub();
        CloseTradeResponse buyersCloseTradeResponse = CloseTradeResponse.fromProto(stub.closeTrade(CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .setMyOutputPeersPrvKeyShare(ByteString.copyFrom(sellerSwapTxSignatureResponse.getPeerOutputPrvKeyShare()))
                .build()));

        commitToModel(buyersCloseTradeResponse, sellerSwapTxSignatureResponse);

        MuSigCooperativeClosureMessage_G responseMessage = new MuSigCooperativeClosureMessage_G(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                buyersCloseTradeResponse);
        sendMessage(responseMessage, serviceProvider, trade);
    }

    @Override
    protected void verifyMessage(MuSigPaymentReceivedMessage_F message) {
        super.verifyMessage(message);
    }

    private void commitToModel(CloseTradeResponse buyersCloseTradeResponse,
                               SwapTxSignatureResponse sellerSwapTxSignatureResponse) {
        MuSigTradeParty buyerAsTaker = trade.getTaker();
        MuSigTradeParty sellerAsMaker = trade.getMaker();

        buyerAsTaker.setCloseTradeResponse(buyersCloseTradeResponse);
        sellerAsMaker.setSwapTxSignatureResponse(sellerSwapTxSignatureResponse);
    }
}
