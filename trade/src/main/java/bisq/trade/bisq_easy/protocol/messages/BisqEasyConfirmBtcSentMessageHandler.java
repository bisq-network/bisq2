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

package bisq.trade.bisq_easy.protocol.messages;

import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.protocol.events.TradeMessageHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyConfirmBtcSentMessageHandler extends TradeMessageHandler<BisqEasyTrade, BisqEasyConfirmBtcSentMessage> {
    public BisqEasyConfirmBtcSentMessageHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyConfirmBtcSentMessage message = (BisqEasyConfirmBtcSentMessage) event;
        verifyMessage(message);

        commitToModel(message.getPaymentProof());
    }

    @Override
    protected void verifyMessage(BisqEasyConfirmBtcSentMessage message) {
        super.verifyMessage(message);

        message.getPaymentProof().ifPresent(paymentProof -> {
            checkArgument(StringUtils.isNotEmpty(paymentProof));
            // We leave it flexible so that users can use other than BTC mainnet data as txId
            checkArgument(paymentProof.length() <= 1000);
        });
    }

    private void commitToModel(Optional<String> paymentProof) {
        paymentProof.ifPresent(e -> trade.getPaymentProof().set(e));
    }
}