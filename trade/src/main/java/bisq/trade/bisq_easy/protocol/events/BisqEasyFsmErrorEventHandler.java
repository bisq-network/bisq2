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

import bisq.common.fsm.ErrorCode;
import bisq.common.fsm.FsmErrorEvent;
import bisq.common.fsm.FsmException;
import bisq.common.util.ExceptionUtil;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.handler.BisqEasyTradeEventHandlerAsMessageSender;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage;
import lombok.extern.slf4j.Slf4j;

import static bisq.common.util.StringUtils.createUid;
import static bisq.common.util.StringUtils.truncate;
import static bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage.MAX_LENGTH_ERROR_MESSAGE;
import static bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage.MAX_LENGTH_STACKTRACE;
import static java.util.Objects.requireNonNullElse;

@Slf4j
public class BisqEasyFsmErrorEventHandler extends BisqEasyTradeEventHandlerAsMessageSender<BisqEasyTrade, FsmErrorEvent> {
    private String errorMessage;
    private String errorStackTrace;
    private ErrorCode errorCode;

    public BisqEasyFsmErrorEventHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(FsmErrorEvent event) {
        FsmException fsmException = event.getFsmException();
        errorMessage = ExceptionUtil.getRootCauseMessage(fsmException);
        errorStackTrace = ExceptionUtil.getSafeStackTraceAsString(fsmException);
        errorCode = requireNonNullElse(fsmException.getErrorCode(), ErrorCode.UNSPECIFIED);
    }

    @Override
    protected void commit() {
        // Set errorStackTrace first as we use errorMessage observable in the handler code accessing both fields
        trade.setErrorStackTrace(errorStackTrace);
        trade.setErrorCode(errorCode);
        trade.setErrorMessage(errorMessage);
    }

    @Override
    protected void sendMessage() {
        log.warn("We send the cause stack and stackTrace to our peer.\n" +
                "errorMessage={}\nstackTrace={}", errorMessage, errorStackTrace);
        send(new BisqEasyReportErrorMessage(createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                truncate(errorMessage, MAX_LENGTH_ERROR_MESSAGE),
                truncate(errorStackTrace, MAX_LENGTH_STACKTRACE),
                trade.getErrorCode()));
    }
}