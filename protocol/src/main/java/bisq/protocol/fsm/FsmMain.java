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

package bisq.protocol.fsm;

import bisq.protocol.fsm.demo.AccountSentEvent;
import bisq.protocol.fsm.demo.BisqEasyState;
import bisq.protocol.fsm.demo.TakeOfferEvent;


public class FsmMain {
    public static void main(String[] args) {

        FiniteStateMachine stateMachine = new FiniteStateMachine();

        stateMachine.transition()
                .from(BisqEasyState.INIT)
                .on(TakeOfferEvent.TYPE)
                .to(BisqEasyState.OFFER_TAKEN);

        stateMachine.transition()
                .from(BisqEasyState.OFFER_TAKEN)
                .on(AccountSentEvent.TYPE)
                .to(BisqEasyState.PAYMENT_ACCOUNT_SENT);
    }
}