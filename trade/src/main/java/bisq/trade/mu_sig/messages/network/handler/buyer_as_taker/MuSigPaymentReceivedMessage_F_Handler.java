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
    private CloseTradeResponse myCloseTradeResponse;
    private SwapTxSignatureResponse peersSwapTxSignature;

    public MuSigPaymentReceivedMessage_F_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(MuSigPaymentReceivedMessage_F message) {
    }

    @Override
    protected void process(MuSigPaymentReceivedMessage_F message) {
        peersSwapTxSignature = message.getSwapTxSignatureResponse();

        muSigTradeService.stopCooperativeCloseTimeout(trade);

        // ClosureType.COOPERATIVE
        // *** BUYER CLOSES TRADE ***
        MusigGrpc.MusigBlockingStub musigBlockingStub = muSigTradeService.getMusigBlockingStub();
        CloseTradeRequest closeTradeRequest = CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .setMyOutputPeersPrvKeyShare(ByteString.copyFrom(peersSwapTxSignature.getPeerOutputPrvKeyShare()))
                .build();
        myCloseTradeResponse = CloseTradeResponse.fromProto(musigBlockingStub.closeTrade(closeTradeRequest));
    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getTaker();
        MuSigTradeParty peer = trade.getMaker();

        mySelf.setCloseTradeResponse(myCloseTradeResponse);
        peer.setSwapTxSignatureResponse(peersSwapTxSignature);
    }

    @Override
    protected void sendMessage() {
        send(new MuSigCooperativeClosureMessage_G(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                myCloseTradeResponse));
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Buyer confirmed to have received the payment and closed the trade.\n." +
                "Buyer sent his closeTradeResponse to the seller.");
    }
}
