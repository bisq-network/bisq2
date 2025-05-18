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
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import bisq.trade.protobuf.CloseTradeRequest;

public final class MuSigSellersCooperativeCloseTimeoutEventHandler extends MuSigTradeEventHandler<MuSigTrade, MuSigSellersCooperativeCloseTimeoutEvent> {
    private CloseTradeResponse myCloseTradeResponse;

    public MuSigSellersCooperativeCloseTimeoutEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(MuSigSellersCooperativeCloseTimeoutEvent event) {
        muSigTradeService.stopCooperativeCloseTimeout(trade);

        MuSigTradeParty buyerAsTake = trade.getTaker();

        // ClosureType.UNCOOPERATIVE
        // *** SELLER FORCE-CLOSES TRADE ***
        //TODO isn't here the swap Tx needed to pass?
        CloseTradeRequest closeTradeRequest = CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .build();
        myCloseTradeResponse = CloseTradeResponse.fromProto(musigBlockingStub.closeTrade(closeTradeRequest));
    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getMaker();

        mySelf.setCloseTradeResponse(myCloseTradeResponse);
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Seller did not receive peers closeTradeResponse and the timeout got triggered\n." +
                "Seller created his closeTradeResponse and force-close the trade.");
    }
}
