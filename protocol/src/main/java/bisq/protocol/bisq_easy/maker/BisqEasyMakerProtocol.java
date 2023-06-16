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

package bisq.protocol.bisq_easy.maker;

import bisq.protocol.bisq_easy.BisqEasyEvent;
import bisq.protocol.bisq_easy.BisqEasyProtocolModel;
import bisq.protocol.bisq_easy.ServiceProvider;
import bisq.protocol.bisq_easy.maker.handlers.ProcessTakeOfferRequestHandler;
import bisq.protocol.bisq_easy.states.BisqEasyState;
import bisq.protocol.bisq_easy.taker.messages.BisqEasyTakeOfferRequest;
import bisq.protocol.fsm.FiniteStateMachine;

public interface BisqEasyMakerProtocol<M extends BisqEasyProtocolModel> {
    default void handleTakeOfferRequest(ServiceProvider serviceProvider, BisqEasyTakeOfferRequest message) {
        ProcessTakeOfferRequestHandler handler = new ProcessTakeOfferRequestHandler(serviceProvider, getModel(), message);
        getFsm().onEvent(BisqEasyEvent.RECEIVED_TAKE_OFFER_REQUEST, handler);
    }

    FiniteStateMachine getFsm();

    M getModel();

    default void configStateMachine() {
        getFsm().transition()
                .from(BisqEasyState.INIT)
                .on(BisqEasyEvent.RECEIVED_TAKE_OFFER_REQUEST)
                .to(BisqEasyState.TAKE_OFFER_REQUEST_ACCEPTED);

    }
}
