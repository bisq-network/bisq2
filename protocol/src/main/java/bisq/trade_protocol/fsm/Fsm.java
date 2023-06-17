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
package bisq.trade_protocol.fsm;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public abstract class Fsm {
    private final TransitionBuilder transitionBuilder = new TransitionBuilder(this);
    private final List<Transition> transitions = new ArrayList<>();

    private final Object currentStateLock = new Object();
    private final Model model;

    public Fsm() {
        this(new Model());
    }

    public Fsm(Model model) {
        this.model = model;
    }

    public void handle(Event event) {
        checkNotNull(event);
        synchronized (currentStateLock) {
            State currentState = model.getCurrentState().get();
            if (currentState == null || currentState.isFinalState()) {
                return;
            }
            for (Transition transition : transitions) {
                if (currentState.equals(transition.getFrom()) &&
                        transition.getEventClass().equals(event.getClass())) {
                    EventHandler eventHandlerFromClass = newEventHandlerFromClass(transition.getEventHandlerClass());
                    eventHandlerFromClass.handle(event);
                    model.getCurrentState().set(transition.getTo());
                    return;
                }
            }
        }
    }

    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) {
        try {
            return handlerClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void addTransition(Transition transition) {
        checkArgument(transition.isValid());
        transitions.add(transition);
    }

    public TransitionBuilder transitionBuilder() {
        return transitionBuilder;
    }

    public abstract void configTransition();

    public static class TransitionBuilder {
        private final Transition transition;
        private final Fsm fsm;

        private TransitionBuilder(Fsm fsm) {
            this.fsm = fsm;
            transition = new Transition();
        }

        public TransitionBuilder from(State from) {
            transition.setFrom(from);
            return this;
        }

        public TransitionBuilder on(Class<? extends Event> eventClass) {
            transition.setEventClass(eventClass);
            return this;
        }

        public TransitionBuilder handle(Class<? extends EventHandler> eventHandlerClass) {
            transition.setEventHandlerClass(eventHandlerClass);
            return this;
        }

        public void to(State to) {
            transition.setTo(to);
            fsm.addTransition(transition);
        }
    }
}
