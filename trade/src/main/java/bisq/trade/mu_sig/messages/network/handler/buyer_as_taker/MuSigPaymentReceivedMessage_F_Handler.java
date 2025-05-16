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

import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import bisq.trade.mu_sig.messages.grpc.SwapTxSignatureResponse;
import bisq.trade.mu_sig.messages.network.MuSigCooperativeClosureMessage_G;
import bisq.trade.mu_sig.messages.network.MuSigPaymentReceivedMessage_F;
import bisq.trade.protobuf.CloseTradeRequest;
import bisq.trade.protobuf.MusigGrpc;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MuSigPaymentReceivedMessage_F_Handler extends MuSigTradeMessageHandlerAsMessageSender<MuSigTrade, MuSigPaymentReceivedMessage_F> {

    public MuSigPaymentReceivedMessage_F_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(MuSigPaymentReceivedMessage_F message) {
        muSigTradeService.stopCooperativeCloseTimeout(trade);

        // ClosureType.COOPERATIVE
        // *** BUYER CLOSES TRADE ***
        SwapTxSignatureResponse sellerSwapTxSignatureResponse = message.getSwapTxSignatureResponse();
        MusigGrpc.MusigBlockingStub musigBlockingStub = muSigTradeService.getMusigBlockingStub();
        CloseTradeResponse buyersCloseTradeResponse = CloseTradeResponse.fromProto(musigBlockingStub.closeTrade(CloseTradeRequest.newBuilder()
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
    }

    private void commitToModel(CloseTradeResponse buyersCloseTradeResponse,
                               SwapTxSignatureResponse sellerSwapTxSignatureResponse) {
        MuSigTradeParty buyerAsTaker = trade.getTaker();
        MuSigTradeParty sellerAsMaker = trade.getMaker();

        buyerAsTaker.setCloseTradeResponse(buyersCloseTradeResponse);
        sellerAsMaker.setSwapTxSignatureResponse(sellerSwapTxSignatureResponse);
    }
}
