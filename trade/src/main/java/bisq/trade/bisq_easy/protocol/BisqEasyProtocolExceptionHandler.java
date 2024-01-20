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

package bisq.trade.bisq_easy.protocol;

import bisq.common.fsm.Event;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.TradeProtocolException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage;
import bisq.trade.protocol.events.SendTradeMessageHandler;

public class BisqEasyProtocolExceptionHandler extends SendTradeMessageHandler<BisqEasyTrade> {
    protected BisqEasyProtocolExceptionHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        TradeProtocolException tradeProtocolException = (TradeProtocolException) event;
        String errorMessage = ExceptionUtil.print(tradeProtocolException);
        commitToModel(errorMessage);

        // We might want to add a flag to the TradeProtocolException to decide if we want to send
        // the error message to the peer.
        // Also, we need to take care that the errorMessage does not contain private data.
        sendMessage(new BisqEasyReportErrorMessage(StringUtils.createUid(),
                trade.getId(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                StringUtils.truncate(errorMessage, BisqEasyReportErrorMessage.MAX_LENGTH)));
    }

    private void commitToModel(String errorMessage) {
        trade.setErrorMessage(errorMessage);
    }
}