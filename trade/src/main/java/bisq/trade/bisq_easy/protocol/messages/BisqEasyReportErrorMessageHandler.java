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

import bisq.common.fsm.ErrorCode;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.handler.BisqEasyTradeMessageHandler;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.requireNonNullElse;

@Slf4j
public class BisqEasyReportErrorMessageHandler extends BisqEasyTradeMessageHandler<BisqEasyTrade, BisqEasyReportErrorMessage> {
    private String stackTrace;
    private String errorMessage;
    private ErrorCode errorCode;

    public BisqEasyReportErrorMessageHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(BisqEasyReportErrorMessage message) {
    }

    @Override
    protected void process(BisqEasyReportErrorMessage message) {
        log.warn("We received an error report from our peer.\n" +
                        "errorMessage={}\nstackTrace={}\ntradeId={}",
                message.getErrorMessage(), message.getStackTrace(), trade.getId());
        stackTrace = message.getStackTrace();
        errorMessage = message.getErrorMessage();
        errorCode = requireNonNullElse(message.getErrorCode(), ErrorCode.UNSPECIFIED);
    }

    @Override
    protected void commit() {
        // Set errorStackTrace first as we use errorMessage observable in the handler code accessing both fields
        trade.setPeersErrorStackTrace(stackTrace);
        trade.setPeersErrorCode(errorCode);
        trade.setPeersErrorMessage(errorMessage);
    }
}