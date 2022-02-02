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

package bisq.protocol.prototype.fsm_demo;

import org.jeasy.states.api.*;
import org.jeasy.states.core.FiniteStateMachineBuilder;
import org.jeasy.states.core.TransitionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class Engine {
    private static final Logger log = LoggerFactory.getLogger(Engine.class);

    public static void main(String[] args) throws FiniteStateMachineException {
        new Engine();
    }

    static class Conf extends AbstractEvent {
    }

    static class OnConf implements EventHandler<Conf> {
        @Override
        public void handleEvent(Conf event) throws Exception {
            log.error("OnConf " + event);
        }
    }

    static class Sent extends AbstractEvent {
    }

    static class OnSent implements EventHandler<Sent> {
        @Override
        public void handleEvent(Sent event) throws Exception {
            log.error("OnSent " + event);
        }
    }

    static class ConfReceipt extends AbstractEvent {
    }

    static class OnConfReceipt implements EventHandler<ConfReceipt> {
        @Override
        public void handleEvent(ConfReceipt event) throws Exception {
            log.error("OnConfReceipt " + event);
        }
    }

    public Engine() throws FiniteStateMachineException {
        State waitConf = new State("waitConf");
        State sendFiat = new State("sendFiat");
        State waitReceived = new State("waitReceived");
        State completed = new State("completed");

        Set<State> states = new HashSet<>();
        states.add(waitConf);
        states.add(sendFiat);
        states.add(waitReceived);
        states.add(completed);

        Transition confirmed = new TransitionBuilder()
                .name("confirmed")
                .sourceState(waitConf)
                .eventType(Conf.class)
                .eventHandler(new OnConf())
                .targetState(sendFiat)
                .build();

        Transition fiatSent = new TransitionBuilder()
                .name("fiatSent")
                .sourceState(sendFiat)
                .eventType(Sent.class)
                .eventHandler(new OnSent())
                .targetState(waitReceived)
                .build();

        Transition fiatReceived = new TransitionBuilder()
                .name("fiatReceived")
                .sourceState(waitReceived)
                .eventType(ConfReceipt.class)
                .eventHandler(new OnConfReceipt())
                .targetState(completed)
                .build();

        FiniteStateMachine buyerStateMachine = new FiniteStateMachineBuilder(states, waitConf)
                .registerTransition(confirmed)
                .registerTransition(fiatSent)
                .registerTransition(fiatReceived)
                .build();

        buyerStateMachine.fire(new Conf());             // BTC tx confirmed
        buyerStateMachine.fire(new Sent());             // Fiat sent
        buyerStateMachine.fire(new ConfReceipt());      // Seller confirmed receipt
    }
}