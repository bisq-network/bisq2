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

import bisq.common.fsm.Event;
import bisq.common.fsm.FsmErrorEvent;
import bisq.common.fsm.FsmException;
import bisq.common.util.ExceptionUtil;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import lombok.extern.slf4j.Slf4j;

import static bisq.common.util.StringUtils.createUid;
import static bisq.common.util.StringUtils.truncate;
import static bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage.MAX_LENGTH_ERROR_MESSAGE;
import static bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage.MAX_LENGTH_STACKTRACE;

@Slf4j
public final class MuSigFsmErrorEventHandler extends MuSigTradeEventHandlerAsMessageSender<MuSigTrade> {
    private String errorMessage;
    private String errorStackTrace;

    public MuSigFsmErrorEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void processEvent(Event event) {
        FsmErrorEvent fsmErrorEvent = (FsmErrorEvent) event;
        FsmException fsmException = fsmErrorEvent.getFsmException();
        errorMessage = ExceptionUtil.getRootCauseMessage(fsmException);
        errorStackTrace = ExceptionUtil.getSafeStackTraceAsString(fsmException);
    }

    @Override
    protected void commitToModel() {
        // Set errorStackTrace first as we use errorMessage observable in the handler code accessing both fields
        trade.setErrorStackTrace(errorStackTrace);
        trade.setErrorMessage(errorMessage);
    }

    @Override
    protected void sendMessage() {
        log.warn("We send the cause stack and stackTrace to our peer.\n" +
                "errorMessage={}\nstackTrace={}", errorMessage, errorStackTrace);
        sendMessage(new MuSigReportErrorMessage(createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                truncate(errorMessage, MAX_LENGTH_ERROR_MESSAGE),
                truncate(errorStackTrace, MAX_LENGTH_STACKTRACE)));
    }

    @Override
    protected void sendLogMessage() {

    }

}