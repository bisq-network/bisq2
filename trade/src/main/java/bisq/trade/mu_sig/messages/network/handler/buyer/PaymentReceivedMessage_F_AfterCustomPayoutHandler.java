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

package bisq.trade.mu_sig.messages.network.handler.buyer;

import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandler;
import bisq.trade.mu_sig.messages.network.PaymentReceivedMessage_F;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PaymentReceivedMessage_F_AfterCustomPayoutHandler
        extends MuSigTradeMessageHandler<MuSigTrade, PaymentReceivedMessage_F> {
    public PaymentReceivedMessage_F_AfterCustomPayoutHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(PaymentReceivedMessage_F message) {
    }

    @Override
    protected void process(PaymentReceivedMessage_F message) {
        log.info("Ignoring PaymentReceivedMessage_F {} for trade {} because the custom payout was already signed.",
                message.getId(), message.getTradeId());
    }

    @Override
    protected void commit() {
    }

    @Override
    protected void sendLogMessage() {
    }
}
