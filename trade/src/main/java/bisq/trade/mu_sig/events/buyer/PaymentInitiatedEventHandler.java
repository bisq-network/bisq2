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

package bisq.trade.mu_sig.events.buyer;

import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import bisq.trade.mu_sig.messages.network.PaymentInitiatedMessage_E;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PartialSignatures;
import bisq.trade.protobuf.PartialSignaturesRequest;

public class PaymentInitiatedEventHandler extends MuSigTradeEventHandlerAsMessageSender<MuSigTrade, PaymentInitiatedEvent> {
    private PartialSignaturesMessage myPartialSignaturesMessage;

    public PaymentInitiatedEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(PaymentInitiatedEvent event) {
        // Buyer calls 'GetPartialSignatures' a second time, this time with the ready-to-release flag set, so that his
        // previously withheld swapTxInputPartialSignature is revealed, to pass to the seller.
        PartialSignaturesRequest partialSignaturesRequest = PartialSignaturesRequest.newBuilder()
                .setTradeId(trade.getId())
                .setBuyerReadyToRelease(true)
                .build();
        myPartialSignaturesMessage = PartialSignaturesMessage.fromProto(blockingStub.getPartialSignatures(partialSignaturesRequest));

        tradeService.startCloseTradeTimeout(trade, new BuyersCloseTradeTimeoutEvent());
    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getMyself();
        mySelf.setMyPartialSignaturesMessage(myPartialSignaturesMessage);
    }

    @Override
    protected void sendMessage() {
        // Now we send the previously withheld swapTxInputPartialSignature to the seller
        PartialSignaturesMessage partialSignaturesMessage = trade.getMyself().getMyPartialSignaturesMessage().orElseThrow();
        PartialSignatures partialSignatures = PartialSignatures.from(partialSignaturesMessage, false);
        byte[] swapTxInputPartialSignature = partialSignatures.getSwapTxInputPartialSignature().orElseThrow();
        send(new PaymentInitiatedMessage_E(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                swapTxInputPartialSignature));
    }

    @Override
    protected void sendLogMessage() {
    }
}
