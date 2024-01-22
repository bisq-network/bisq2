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
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.protocol.events.TradeMessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyReportErrorMessageHandler extends TradeMessageHandler<BisqEasyTrade, BisqEasyReportErrorMessage> {

    public BisqEasyReportErrorMessageHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyReportErrorMessage message = (BisqEasyReportErrorMessage) event;
        verifyMessage(message);
        log.warn("We received an error report from our peer.\n" +
                        "errorMessage={}\nstackTrace={}\ntradeId={}",
                message.getErrorMessage(), message.getStackTrace(), trade.getId());
        commitToModel(message);
    }

    @Override
    protected void verifyMessage(BisqEasyReportErrorMessage message) {
        super.verifyMessage(message);
    }

    private void commitToModel(BisqEasyReportErrorMessage message) {
        // Set peersErrorStackTrace first as we use peersErrorMessage observable in the handler code accessing both fields
        trade.setPeersErrorStackTrace(message.getStackTrace());
        trade.setPeersErrorMessage(message.getErrorMessage());
    }
}