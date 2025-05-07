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

package bisq.trade.mu_sig.events.seller_as_maker;

import bisq.common.fsm.Event;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.protobuf.CloseTradeRequest;
import bisq.trade.protobuf.CloseTradeResponse;
import bisq.trade.protobuf.MusigGrpc;
import bisq.trade.protocol.events.TradeEventHandler;

public class MuSigSellersCooperativeCloseTimeoutEventHandler extends TradeEventHandler<MuSigTrade> {

    public MuSigSellersCooperativeCloseTimeoutEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        serviceProvider.getMuSigTradeService().stopCooperativeCloseTimeout(trade);

        MuSigTradeParty buyerAsTake = trade.getTaker();

        // ClosureType.UNCOOPERATIVE
        // *** SELLER FORCE-CLOSES TRADE ***
        MusigGrpc.MusigBlockingStub stub = serviceProvider.getMuSigTradeService().getMusigStub();
        //TODO isn't here the swap Tx needed to pass?
        CloseTradeResponse sellersCloseTradeResponse = stub.closeTrade(CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .build());
    }

    private void commitToModel() {
    }
}
