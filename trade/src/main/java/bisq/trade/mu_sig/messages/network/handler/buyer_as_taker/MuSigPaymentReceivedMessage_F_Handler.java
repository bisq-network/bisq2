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

import bisq.common.data.ByteArray;
import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import bisq.trade.mu_sig.messages.network.MuSigCooperativeClosureMessage_G;
import bisq.trade.mu_sig.messages.network.MuSigPaymentReceivedMessage_F;
import bisq.trade.mu_sig.messages.network.mu_sig_data.SwapTxSignature;
import bisq.trade.protobuf.CloseTradeRequest;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MuSigPaymentReceivedMessage_F_Handler extends MuSigTradeMessageHandlerAsMessageSender<MuSigTrade, MuSigPaymentReceivedMessage_F> {
    private CloseTradeResponse myCloseTradeResponse;
    private SwapTxSignature peersSwapTxSignature;

    public MuSigPaymentReceivedMessage_F_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(MuSigPaymentReceivedMessage_F message) {
    }

    @Override
    protected void process(MuSigPaymentReceivedMessage_F message) {
        peersSwapTxSignature = message.getSwapTxSignature();

        muSigTradeService.stopCloseTimeout(trade);

        // ClosureType.COOPERATIVE
        // *** BUYER CLOSES TRADE ***
        CloseTradeRequest closeTradeRequest = CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .setMyOutputPeersPrvKeyShare(ByteString.copyFrom(peersSwapTxSignature.getPeerOutputPrvKeyShare()))
                .build();
        myCloseTradeResponse = CloseTradeResponse.fromProto(musigBlockingStub.closeTrade(closeTradeRequest));
    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getMyself();
        MuSigTradeParty peer = trade.getPeer();

        mySelf.setMyCloseTradeResponse(myCloseTradeResponse);
        peer.setPeersSwapTxSignature(peersSwapTxSignature);
    }

    @Override
    protected void sendMessage() {
        byte[] peerOutputPrvKeyShare = myCloseTradeResponse.getPeerOutputPrvKeyShare().clone();
        send(new MuSigCooperativeClosureMessage_G(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                new ByteArray(peerOutputPrvKeyShare)));
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Buyer received the message that the seller has confirmed payment receipt.\n" +
                "The message contained peersSwapTxSignature which includes the peerOutputPrvKeyShare.\n" +
                "Buyer closed the trade.\n" +
                "Buyer sent peerOutputPrvKeyShare to the seller.");
    }
}
