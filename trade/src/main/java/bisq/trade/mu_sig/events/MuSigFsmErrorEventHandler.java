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

package bisq.trade.mu_sig.events;

import bisq.common.fsm.FsmErrorEvent;
import bisq.common.fsm.FsmException;
import bisq.common.util.ExceptionUtil;
import bisq.trade.ServiceProvider;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import lombok.extern.slf4j.Slf4j;

import static bisq.common.util.StringUtils.createUid;
import static bisq.common.util.StringUtils.truncate;
import static bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage.MAX_LENGTH_ERROR_MESSAGE;
import static bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage.MAX_LENGTH_STACKTRACE;

@Slf4j
public final class MuSigFsmErrorEventHandler extends MuSigTradeEventHandlerAsMessageSender<MuSigTrade, FsmErrorEvent> {
    private String errorMessage;
    private String errorStackTrace;
    private TradeProtocolFailure tradeProtocolFailure;

    public MuSigFsmErrorEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(FsmErrorEvent event) {
        FsmException fsmException = event.getFsmException();
        if (fsmException.getCause() instanceof TradeProtocolException bisqEasyProtocolException) {
            tradeProtocolFailure = bisqEasyProtocolException.getTradeProtocolFailure();
        } else {
            tradeProtocolFailure = TradeProtocolFailure.UNKNOWN;
        }
        errorMessage = ExceptionUtil.getRootCauseMessage(fsmException);
        errorStackTrace = ExceptionUtil.getSafeStackTraceAsString(fsmException);
    }

    @Override
    protected void commit() {
        trade.setErrorData(tradeProtocolFailure, errorStackTrace, errorMessage);
    }

    @Override
    protected void sendMessage() {
        log.warn("We send the error message and stackTrace to our peer.\n" +
                "errorMessage={}\nstackTrace={}", errorMessage, errorStackTrace);
        send(new MuSigReportErrorMessage(createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                truncate(errorMessage, MAX_LENGTH_ERROR_MESSAGE),
                truncate(errorStackTrace, MAX_LENGTH_STACKTRACE),
                tradeProtocolFailure));
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("An error occurred.\n" +
                "We send the error message and stackTrace to our peer.");
    }
}