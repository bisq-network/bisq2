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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public final class FiniteStateMachine {
    private final Config config = new Config(this);
    private final List<Transition> transitions = new ArrayList<>();

    @Setter
    private State currentState;

    public void onEvent(Event event, Task task) {
        checkNotNull(event);

        if (currentState.isFinalState()) {
            return;
        }

        for (Transition transition : transitions) {
            if (currentState.equals(transition.getFrom()) &&
                    transition.getEvent().equals(event)) {
                task.run();
                currentState = transition.getTo();
                return;
            }
        }
    }


    void addTransition(Transition transition) {
        transitions.add(transition);
    }


    public Config transition() {
        return config;
    }

    public static class Config {
        private final Transition transition;
        private final FiniteStateMachine stateMachine;

        public Config(FiniteStateMachine stateMachine) {
            this.stateMachine = stateMachine;
            transition = new Transition();
        }

        public Config from(State sourceState) {
            transition.setFrom(sourceState);
            return this;
        }

        public Config on(Event event) {
            transition.setEvent(event);
            return this;
        }

        public Config run(Class<?> handler) {
            transition.setHandler(handler);
            return this;
        }

        public void to(State targetState) {
            transition.setTo(targetState);
            stateMachine.addTransition(transition);
        }
    }
}
