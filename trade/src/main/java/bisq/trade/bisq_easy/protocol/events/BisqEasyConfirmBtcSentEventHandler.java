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

package bisq.trade.bisq_easy.protocol.events;

import bisq.common.fsm.Event;
import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmBtcSentMessage;
import bisq.trade.protocol.events.SendTradeMessageHandler;

import java.util.Optional;

public class BisqEasyConfirmBtcSentEventHandler extends SendTradeMessageHandler<BisqEasyTrade> {

    public BisqEasyConfirmBtcSentEventHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyConfirmBtcSentEvent bisqEasyConfirmBtcSentEvent = (BisqEasyConfirmBtcSentEvent) event;
        Optional<String> paymentProof = bisqEasyConfirmBtcSentEvent.getPaymentProof();
        commitToModel(paymentProof);
        sendMessage(new BisqEasyConfirmBtcSentMessage(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                paymentProof));
    }

    private void commitToModel(Optional<String> paymentProof) {
        paymentProof.ifPresent(e -> trade.getPaymentProof().set(e));
    }
}
