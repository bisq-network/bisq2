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

import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.handler.BisqEasyTradeEventHandlerAsMessageSender;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage;
import bisq.trade.exceptions.TradeProtocolFailure;
import lombok.extern.slf4j.Slf4j;

import static bisq.common.util.StringUtils.createUid;
import static bisq.common.util.StringUtils.truncate;
import static bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage.MAX_LENGTH_ERROR_MESSAGE;

@Slf4j
public class BisqEasyChatChannelMismatchEventHandler extends BisqEasyTradeEventHandlerAsMessageSender<BisqEasyTrade, BisqEasyChatChannelMismatchEvent> {
    private String errorMessage;

    public BisqEasyChatChannelMismatchEventHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(BisqEasyChatChannelMismatchEvent event) {
        errorMessage = event.getDetails();
    }

    @Override
    protected void commit() {
        trade.setErrorData(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, "", errorMessage);
    }

    @Override
    protected void sendMessage() {
        log.warn("Trade failed as the chat channel data does not match the contract. errorMessage={}", errorMessage);
        send(new BisqEasyReportErrorMessage(createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                truncate(errorMessage, MAX_LENGTH_ERROR_MESSAGE),
                "",
                TradeProtocolFailure.CHANNEL_DATA_MISMATCH));
    }
}
