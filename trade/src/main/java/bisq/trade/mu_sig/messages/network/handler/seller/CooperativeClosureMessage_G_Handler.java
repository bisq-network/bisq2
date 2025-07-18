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

package bisq.trade.mu_sig.messages.network.handler.seller;

import bisq.common.data.ByteArray;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandler;
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import bisq.trade.mu_sig.messages.network.CooperativeClosureMessage_G;
import bisq.trade.protobuf.CloseTradeRequest;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CooperativeClosureMessage_G_Handler extends MuSigTradeMessageHandler<MuSigTrade, CooperativeClosureMessage_G> {
    private ByteArray peersOutputPrvKeyShare;
    private CloseTradeResponse myCloseTradeResponse;

    public CooperativeClosureMessage_G_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(CooperativeClosureMessage_G message) {
    }

    @Override
    protected void process(CooperativeClosureMessage_G message) {
        peersOutputPrvKeyShare = message.getPeerOutputPrvKeyShare();

        tradeService.stopCloseTradeTimeout(trade);

        // ClosureType.COOPERATIVE
        // *** SELLER CLOSES TRADE ***
        CloseTradeRequest closeTradeRequest = CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .setMyOutputPeersPrvKeyShare(ByteString.copyFrom(peersOutputPrvKeyShare.getBytes()))
                .build();
        myCloseTradeResponse = CloseTradeResponse.fromProto(blockingStub.closeTrade(closeTradeRequest));
    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getMyself();
        MuSigTradeParty peer = trade.getPeer();

        mySelf.setMyCloseTradeResponse(myCloseTradeResponse);
        peer.setPeersOutputPrvKeyShare(peersOutputPrvKeyShare);
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Seller received peersOutputPrvKeyShare.\n" +
                "Seller closed trade.");
    }
}
