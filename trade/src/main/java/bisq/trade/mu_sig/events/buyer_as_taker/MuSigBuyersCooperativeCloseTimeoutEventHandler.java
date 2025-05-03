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
import bisq.trade.mu_sig.grpc.CloseTradeRequest;
import bisq.trade.mu_sig.grpc.CloseTradeResponse;
import bisq.trade.mu_sig.grpc.GrpcStubMock;
import bisq.trade.protocol.events.SendTradeMessageHandler;

import java.util.Optional;

public class MuSigBuyersCooperativeCloseTimeoutEventHandler extends SendTradeMessageHandler<MuSigTrade> {

    public MuSigBuyersCooperativeCloseTimeoutEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        serviceProvider.getMuSigTradeService().stopCooperativeCloseTimeout(trade);

        MuSigTradeParty buyerAsTake = trade.getTaker();

        // ClosureType.UNCOOPERATIVE
        // Buyer never got Message F from seller -- picks up Swap Tx from bitcoin network instead.
        // *** BUYER CLOSES TRADE ***
        // TODO get swap tx from bitcoin network
        byte[] swapTx = new byte[]{};
        CloseTradeRequest closeTradeRequest = new CloseTradeRequest(trade.getId(), Optional.empty(), Optional.of(swapTx));
        GrpcStubMock stub = new GrpcStubMock();
        CloseTradeResponse buyersCloseTradeResponse = stub.closeTrade(closeTradeRequest);
    }

    private void commitToModel() {
    }
}
