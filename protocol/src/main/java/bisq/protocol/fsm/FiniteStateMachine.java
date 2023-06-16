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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public final class FiniteStateMachine {
    private final Config config = new Config(this);
    private final List<Transition> transitions = new ArrayList<>();
    private final Map<Class<?>, Transition> transitionsByEventType = new HashMap<>();

    @Setter
    private State currentState;

    public void handleEvent(Event event) {
        checkNotNull(event);

        if (currentState.isFinalState()) {
            return;
        }

        for (Transition transition : transitions) {
            if (currentState.equals(transition.getFrom()) &&
                    transition.getEventType().equals(event.getEventType())) {
                //  try {
                // EventHandler handler = (EventHandler) transition.getHandler().getDeclaredConstructor(Event.class).newInstance(event);
                event.getHandler().handle(event);
              /*  } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }*/
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

        public Config on(EventType eventType) {
            transition.setEventType(eventType);
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
