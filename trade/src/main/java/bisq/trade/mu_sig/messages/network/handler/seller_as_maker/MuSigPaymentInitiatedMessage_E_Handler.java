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

import bisq.common.fsm.Event;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.messages.grpc.SwapTxSignatureResponse;
import bisq.trade.mu_sig.messages.network.MuSigPaymentInitiatedMessage_E;
import bisq.trade.protocol.handler.TradeMessageHandler;
import bisq.trade.protocol.handler.TradeMessageSender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MuSigPaymentInitiatedMessage_E_Handler extends TradeMessageHandler<MuSigTrade, MuSigPaymentInitiatedMessage_E>
        implements TradeMessageSender<MuSigTrade> {

    public MuSigPaymentInitiatedMessage_E_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        MuSigPaymentInitiatedMessage_E message = (MuSigPaymentInitiatedMessage_E) event;
        verifyMessage(message);
    }

    @Override
    protected void verifyMessage(MuSigPaymentInitiatedMessage_E message) {
        super.verifyMessage(message);
    }

    private void commitToModel(SwapTxSignatureResponse sellerSwapTxSignatureResponse) {
    }
}
