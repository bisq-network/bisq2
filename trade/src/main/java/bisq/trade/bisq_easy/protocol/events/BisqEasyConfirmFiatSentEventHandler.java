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
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatSentMessage;
import bisq.trade.protocol.handler.SendTradeMessageHandler;

public class BisqEasyConfirmFiatSentEventHandler extends SendTradeMessageHandler<BisqEasyTrade> {

    public BisqEasyConfirmFiatSentEventHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        sendMessage(new BisqEasyConfirmFiatSentMessage(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId()));
    }
}