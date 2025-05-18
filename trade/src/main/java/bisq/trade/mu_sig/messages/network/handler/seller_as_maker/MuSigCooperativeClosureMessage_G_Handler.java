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

package bisq.trade.mu_sig.messages.network.handler.seller_as_maker;

import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandler;
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import bisq.trade.mu_sig.messages.network.MuSigCooperativeClosureMessage_G;
import bisq.trade.protobuf.CloseTradeRequest;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MuSigCooperativeClosureMessage_G_Handler extends MuSigTradeMessageHandler<MuSigTrade, MuSigCooperativeClosureMessage_G> {
    private CloseTradeResponse peersCloseTradeResponse;
    private CloseTradeResponse myCloseTradeResponse;

    public MuSigCooperativeClosureMessage_G_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(MuSigCooperativeClosureMessage_G message) {
    }

    @Override
    protected void process(MuSigCooperativeClosureMessage_G message) {
        peersCloseTradeResponse = message.getCloseTradeResponse();

        muSigTradeService.stopCooperativeCloseTimeout(trade);

        // ClosureType.COOPERATIVE
        // *** SELLER CLOSES TRADE ***
        CloseTradeRequest closeTradeRequest = CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .setMyOutputPeersPrvKeyShare(ByteString.copyFrom(peersCloseTradeResponse.getPeerOutputPrvKeyShare()))
                .build();
        myCloseTradeResponse = CloseTradeResponse.fromProto(musigBlockingStub.closeTrade(closeTradeRequest));
    }

    @Override
    protected void commit() {
        MuSigTradeParty peer = trade.getTaker();
        MuSigTradeParty mySelf = trade.getMaker();

        mySelf.setCloseTradeResponse(myCloseTradeResponse);
        peer.setCloseTradeResponse(peersCloseTradeResponse);
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Seller received peers closeTradeResponse.\n" +
                "Seller created his closeTradeResponse.");
    }
}
