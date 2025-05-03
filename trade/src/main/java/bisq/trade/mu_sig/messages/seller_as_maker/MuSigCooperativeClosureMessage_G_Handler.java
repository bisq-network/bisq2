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

package bisq.trade.mu_sig.messages.seller_as_maker;

import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.grpc.CloseTradeRequest;
import bisq.trade.mu_sig.grpc.CloseTradeResponse;
import bisq.trade.mu_sig.grpc.MusigGrpc;
import bisq.trade.mu_sig.messages.MuSigCooperativeClosureMessage_G;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigCooperativeClosureMessage_G_Handler extends TradeMessageHandler<MuSigTrade, MuSigCooperativeClosureMessage_G>
        implements TradeMessageSender<MuSigTrade> {

    public MuSigCooperativeClosureMessage_G_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        MuSigCooperativeClosureMessage_G message = (MuSigCooperativeClosureMessage_G) event;
        verifyMessage(message);

        serviceProvider.getMuSigTradeService().stopCooperativeCloseTimeout(trade);

        MuSigTradeParty buyerAsTake = trade.getTaker();

        // ClosureType.COOPERATIVE
        // *** SELLER CLOSES TRADE ***
        CloseTradeResponse buyersCloseTradeResponse = message.getCloseTradeResponse();
         MusigGrpc.MusigBlockingStub stub = serviceProvider.getMuSigTradeService().getMusigStub();

        CloseTradeResponse sellersCloseTradeResponse = stub.closeTrade(CloseTradeRequest.newBuilder()
                .setTradeId(trade.getId())
                .setMyOutputPeersPrvKeyShare(buyersCloseTradeResponse.getPeerOutputPrvKeyShare())
                .build());

        MuSigCooperativeClosureMessage_G response = new MuSigCooperativeClosureMessage_G(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                buyersCloseTradeResponse);
        sendMessage(response, serviceProvider, trade);
    }

    @Override
    protected void verifyMessage(MuSigCooperativeClosureMessage_G message) {
        super.verifyMessage(message);
    }

    private void commitToModel() {
    }
}
