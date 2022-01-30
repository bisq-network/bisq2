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

package bisq.protocol.fsm_demo;

import org.jeasy.states.api.*;
import org.jeasy.states.core.FiniteStateMachineBuilder;
import org.jeasy.states.core.TransitionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class FSM_main {
    private static final Logger log = LoggerFactory.getLogger(FSM_main.class);

    public static void main(String[] args) throws FiniteStateMachineException {
        new FSM_main();
    }

    static class PushEvent extends AbstractEvent {
    }

    static class CoinEvent extends AbstractEvent {
    }

    static class Unlock implements EventHandler<CoinEvent> {
        @Override
        public void handleEvent(CoinEvent event) throws Exception {
            log.error("Unlock " + event);
        }
    }

    static class Lock implements EventHandler<PushEvent> {
        @Override
        public void handleEvent(PushEvent event) throws Exception {
            log.error("Lock " + event);
        }
    }

    static class WarnCoin implements EventHandler<CoinEvent> {
        @Override
        public void handleEvent(CoinEvent event) throws Exception {
            log.error("WarnCoin " + event);
        }
    }

    static class WarnPush implements EventHandler<PushEvent> {
        @Override
        public void handleEvent(PushEvent event) throws Exception {
            log.error("WarnPush " + event);
        }
    }

    public FSM_main() throws FiniteStateMachineException {
        State locked = new State("locked");
        State unlocked = new State("unlocked");

        Set<State> states = new HashSet<>();
        states.add(locked);
        states.add(unlocked);

        Transition unlock = new TransitionBuilder()
                .name("unlock")
                .sourceState(locked)
                .eventType(CoinEvent.class)
                .eventHandler(new Unlock())
                .targetState(unlocked)
                .build();

        Transition pushLocked = new TransitionBuilder()
                .name("pushLocked")
                .sourceState(locked)
                .eventType(PushEvent.class)
                .eventHandler(new WarnPush())
                .targetState(locked)
                .build();

        Transition lock = new TransitionBuilder()
                .name("lock")
                .sourceState(unlocked)
                .eventType(PushEvent.class)
                .eventHandler(new Lock())
                .targetState(locked)
                .build();

        Transition coinUnlocked = new TransitionBuilder()
                .name("coinUnlocked")
                .sourceState(unlocked)
                .eventType(CoinEvent.class)
                .eventHandler(new WarnCoin())
                .targetState(unlocked)
                .build();

        FiniteStateMachine turnstileStateMachine = new FiniteStateMachineBuilder(states, locked)
                .registerTransition(lock)
                .registerTransition(pushLocked)
                .registerTransition(unlock)
                .registerTransition(coinUnlocked)
                .build();

        turnstileStateMachine.fire(new CoinEvent()); // unlock
        turnstileStateMachine.fire(new CoinEvent()); // nothing
        turnstileStateMachine.fire(new PushEvent());    // lock
        turnstileStateMachine.fire(new PushEvent());    // nothing

    }
}