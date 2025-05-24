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

package bisq.trade.mu_sig.protocol.not_yet_impl;

import bisq.common.fsm.FsmErrorEvent;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.events.MuSigFsmErrorEventHandler;
import bisq.trade.mu_sig.events.MuSigReportErrorMessageHandler;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import lombok.extern.slf4j.Slf4j;

import static bisq.trade.mu_sig.protocol.MuSigTradeState.FAILED;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.FAILED_AT_PEER;

@Slf4j
public final class MuSigSellerAsTakerProtocol extends MuSigProtocol {

    public MuSigSellerAsTakerProtocol(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
        log.error("MuSigSellerAsTakerProtocol not implemented yet");
    }

    @Override
    protected void configErrorHandling() {
        fromAny()
                .on(FsmErrorEvent.class)
                .run(MuSigFsmErrorEventHandler.class)
                .to(FAILED);

        fromAny()
                .on(MuSigReportErrorMessage.class)
                .run(MuSigReportErrorMessageHandler.class)
                .to(FAILED_AT_PEER);
    }

    public void configTransitions() {
        // TODO: Implement state transitions for seller-as-taker protocol
        throw new UnsupportedOperationException("MuSigSellerAsTakerProtocol not implemented yet");
    }
}