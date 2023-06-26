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

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyConfirmFiatSentMessageHandler extends TradeMessageHandler<BisqEasyTrade, BisqEasyConfirmFiatSentMessage> {

    public BisqEasyConfirmFiatSentMessageHandler(ServiceProvider serviceProvider,
                                                 BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyConfirmFiatSentMessage message = (BisqEasyConfirmFiatSentMessage) event;
        verifyMessage(message);

        commitToModel(message.getBtcAddress());
    }

    @Override
    protected void verifyMessage(BisqEasyConfirmFiatSentMessage message) {
        //todo
        checkArgument(StringUtils.isNotEmpty(message.getBtcAddress()));
    }

    private void commitToModel(String btcAddress) {
        trade.getBtcAddress().set(btcAddress);
    }
}