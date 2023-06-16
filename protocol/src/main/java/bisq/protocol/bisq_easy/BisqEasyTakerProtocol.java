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

package bisq.protocol.bisq_easy;

import bisq.protocol.bisq_easy.states.BisqEasyTakerState;
import bisq.protocol.fsm.demo.TakeOfferEvent;

public abstract class BisqEasyTakerProtocol extends BisqEasyProtocol {

    public BisqEasyTakerProtocol(BisqEasyProtocolModel model) {
        super(model);
    }

    @Override
    protected void configStateMachine() {
        stateMachine.transition()
                .from(BisqEasyTakerState.INIT)
                .on(TakeOfferEvent.TYPE)
                .to(BisqEasyTakerState.OFFER_TAKEN);

        stateMachine.transition()
                .from(BisqEasyTakerState.OFFER_TAKEN)
                .on(TakeOfferEvent.TYPE)
                .to(BisqEasyTakerState.COMPLETE);
    }

    public void takeOffer() {

    }
}