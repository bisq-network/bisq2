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

import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandler;
import bisq.trade.protobuf.CloseTradeRequest;
import bisq.trade.protobuf.CloseTradeResponse;
import bisq.trade.protobuf.MusigGrpc;

public final class MuSigSellersCooperativeCloseTimeoutEventHandler extends MuSigTradeEventHandler<MuSigTrade, MuSigSellersCooperativeCloseTimeoutEvent> {

    public MuSigSellersCooperativeCloseTimeoutEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(MuSigSellersCooperativeCloseTimeoutEvent event) {
        muSigTradeService.stopCooperativeCloseTimeout(trade);

        MuSigTradeParty buyerAsTake = trade.getTaker();

        // ClosureType.UNCOOPERATIVE
        // *** SELLER FORCE-CLOSES TRADE ***
        MusigGrpc.MusigBlockingStub musigBlockingStub = muSigTradeService.getMusigBlockingStub();
        //TODO isn't here the swap Tx needed to pass?
        CloseTradeResponse sellersCloseTradeResponse = musigBlockingStub.closeTrade(CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .build());
    }

    @Override
    protected void commit() {
    }
}
