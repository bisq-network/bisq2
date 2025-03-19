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

package bisq.trade.bisq_musig.events;

import bisq.common.fsm.Event;
import bisq.common.fsm.EventHandler;
import bisq.common.fsm.FsmErrorEvent;
import bisq.common.util.ExceptionUtil;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqMuSigErrorEventHandler implements EventHandler {
    private final ServiceProvider serviceProvider;
    private final BisqMuSigTrade trade;

    public BisqMuSigErrorEventHandler(ServiceProvider serviceProvider, BisqMuSigTrade trade) {
        this.serviceProvider = serviceProvider;
        this.trade = trade;
    }

    @Override
    public void handle(Event event) {
        if (!(event instanceof FsmErrorEvent)) {
            return;
        }

        FsmErrorEvent errorEvent = (FsmErrorEvent) event;

        log.error("Protocol error: {}", errorEvent.getFsmException().getMessage(),
                errorEvent.getFsmException());

        String errorMessage = ExceptionUtil.getRootCauseMessage(errorEvent.getFsmException());
        String stackTrace = ExceptionUtil.getStackTraceAsString(errorEvent.getFsmException());

        commitToModel(errorMessage, stackTrace);

        // Optionally report the error to the peer
        // reportErrorToPeer(errorMessage, stackTrace);
    }

    private void commitToModel(String errorMessage, String errorStackTrace) {
        trade.setErrorStackTrace(errorStackTrace);
        trade.setErrorMessage(errorMessage);
    }

    private void reportErrorToPeer(String errorMessage, String stackTrace) {
        //...
    }

}