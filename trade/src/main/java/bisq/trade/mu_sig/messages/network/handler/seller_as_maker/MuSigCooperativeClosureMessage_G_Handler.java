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
import bisq.trade.protobuf.MusigGrpc;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MuSigCooperativeClosureMessage_G_Handler extends MuSigTradeMessageHandler<MuSigTrade, MuSigCooperativeClosureMessage_G> {

    public MuSigCooperativeClosureMessage_G_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void processMessage(MuSigCooperativeClosureMessage_G message) {
        muSigTradeService.stopCooperativeCloseTimeout(trade);

        // ClosureType.COOPERATIVE
        // *** SELLER CLOSES TRADE ***
        CloseTradeResponse buyerCloseTradeResponse = message.getCloseTradeResponse();
        MusigGrpc.MusigBlockingStub musigBlockingStub = muSigTradeService.getMusigBlockingStub();
        CloseTradeResponse sellersCloseTradeResponse = CloseTradeResponse.fromProto(musigBlockingStub.closeTrade(CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .setMyOutputPeersPrvKeyShare(ByteString.copyFrom(buyerCloseTradeResponse.getPeerOutputPrvKeyShare()))
                .build()));

        commitToModel(sellersCloseTradeResponse, buyerCloseTradeResponse);
    }

    @Override
    protected void verifyMessage(MuSigCooperativeClosureMessage_G message) {
    }

    private void commitToModel(CloseTradeResponse sellersCloseTradeResponse,
                               CloseTradeResponse buyerCloseTradeResponse) {
        MuSigTradeParty buyerAsTaker = trade.getTaker();
        MuSigTradeParty sellerAsMaker = trade.getMaker();

        sellerAsMaker.setCloseTradeResponse(sellersCloseTradeResponse);
        buyerAsTaker.setCloseTradeResponse(buyerCloseTradeResponse);
    }
}
