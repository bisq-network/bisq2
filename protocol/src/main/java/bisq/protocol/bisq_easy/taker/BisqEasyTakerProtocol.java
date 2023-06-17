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

package bisq.protocol.bisq_easy.taker;

import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.protocol.bisq_easy.BisqEasyEvent;
import bisq.protocol.bisq_easy.BisqEasyProtocolModel;
import bisq.protocol.bisq_easy.ServiceProvider;
import bisq.protocol.bisq_easy.states.BisqEasyState;
import bisq.protocol.bisq_easy.taker.tasks.SendBisqEasyTakeOfferRequest;
import bisq.protocol.fsm.FiniteStateMachine;

public interface BisqEasyTakerProtocol<M extends BisqEasyProtocolModel> {
    FiniteStateMachine getFsm();

    M getModel();

    default void configStateMachine() {
        getFsm().transition()
                .from(BisqEasyState.INIT)
                .on(BisqEasyEvent.TAKE_OFFER)
                .to(BisqEasyState.TAKE_OFFER_REQUEST_SENT);
    }

    default void takeOffer(ServiceProvider serviceProvider, Identity takerIdentity, BisqEasyContract bisqEasyContract) {
        SendBisqEasyTakeOfferRequest handler = new SendBisqEasyTakeOfferRequest(serviceProvider,
                getModel(),
                takerIdentity,
                bisqEasyContract);
        getFsm().onEvent(BisqEasyEvent.TAKE_OFFER, handler);
    }
}
