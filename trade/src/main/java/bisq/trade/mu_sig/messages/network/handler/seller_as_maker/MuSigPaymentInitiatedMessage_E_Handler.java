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
import bisq.trade.mu_sig.messages.network.MuSigPaymentInitiatedMessage_E;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PartialSignatures;
import bisq.trade.mu_sig.messages.network.mu_sig_data.RedactedPartialSignatures;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MuSigPaymentInitiatedMessage_E_Handler extends MuSigTradeMessageHandler<MuSigTrade, MuSigPaymentInitiatedMessage_E> {
    private byte[] peersSwapTxInputPartialSignature;

    public MuSigPaymentInitiatedMessage_E_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(MuSigPaymentInitiatedMessage_E message) {
    }

    @Override
    protected void process(MuSigPaymentInitiatedMessage_E message) {
        peersSwapTxInputPartialSignature = message.getSwapTxInputPartialSignature();
    }

    @Override
    protected void commit() {
        MuSigTradeParty peer = trade.getPeer();
        // Now we reconstruct the un-redacted PartialSignatures
        RedactedPartialSignatures redactedPartialSignatures = peer.getPeersRedactedPartialSignatures().orElseThrow();
        PartialSignatures peersPartialSignatures = PartialSignatures.from(redactedPartialSignatures, peersSwapTxInputPartialSignature);
        peer.setPeersPartialSignatures(peersPartialSignatures);
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Seller received the message that the buyer has initiated the payment");
    }
}
