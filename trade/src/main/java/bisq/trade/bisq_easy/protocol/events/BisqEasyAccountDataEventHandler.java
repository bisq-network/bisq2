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

import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyAccountDataMessage;
import bisq.trade.protocol.handler.TradeEventHandlerAsMessageSender;

public class BisqEasyAccountDataEventHandler extends TradeEventHandlerAsMessageSender<BisqEasyTrade, BisqEasyAccountDataEvent> {
    private String paymentAccountData;

    public BisqEasyAccountDataEventHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(BisqEasyAccountDataEvent event) {
        paymentAccountData = event.getPaymentAccountData();
    }

    @Override
    protected void commit() {
        trade.getPaymentAccountData().set(paymentAccountData);
    }

    @Override
    protected void sendMessage() {
        send(new BisqEasyAccountDataMessage(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                paymentAccountData,
                trade.getOffer()));
    }
}
