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

package bisq.trade.mu_sig.events.buyer_as_taker;

import bisq.common.fsm.Event;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandler;
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import bisq.trade.protobuf.CloseTradeRequest;
import bisq.trade.protobuf.MusigGrpc;
import com.google.protobuf.ByteString;

public final class MuSigBuyersCooperativeCloseTimeoutEventHandler extends MuSigTradeEventHandler<MuSigTrade> {

    public MuSigBuyersCooperativeCloseTimeoutEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void processEvent(Event event) {
        muSigTradeService.stopCooperativeCloseTimeout(trade);

        MuSigTradeParty buyerAsTake = trade.getTaker();

        // ClosureType.UNCOOPERATIVE
        // Buyer never got Message F from seller -- picks up Swap Tx from bitcoin network instead.
        // *** BUYER CLOSES TRADE ***
        // TODO get swap tx from bitcoin network
        //ByteString swapTx = swapTxSignatureResponse.getSwapTx();
        byte[] swapTx = new byte[]{};
        MusigGrpc.MusigBlockingStub musigBlockingStub = muSigTradeService.getMusigBlockingStub();
        CloseTradeResponse buyersCloseTradeResponse = CloseTradeResponse.fromProto(musigBlockingStub.closeTrade(CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .setSwapTx(ByteString.copyFrom(swapTx))
                .build()));
    }

    @Override
    protected void commitToModel() {
    }
}
