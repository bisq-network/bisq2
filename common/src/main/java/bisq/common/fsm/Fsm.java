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
package bisq.common.fsm;

import bisq.common.data.Pair;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public abstract class Fsm {
    private final TransitionBuilder transitionBuilder = new TransitionBuilder(this);
    private final Map<Pair<State, Class<? extends Event>>, Transition> transitionMap = new HashMap<>();
    private final Object currentStateLock = new Object();
    private final Model model;

    public Fsm() {
        this(new Model());
    }

    public Fsm(Model model) {
        this.model = model;
        configTransitions();
    }

    public void handle(Event event) throws FsmException {
        try {
            checkNotNull(event, "event must not be null");
            synchronized (currentStateLock) {
                State currentState = model.getCurrentState().get();
                checkNotNull(currentState, "currentState must not be null");
                if (currentState.isFinalState()) {
                    log.warn("We have reached the final state and do not allow further state transition");
                    return;
                }
                Pair<State, Class<? extends Event>> validTransitionKey = new Pair<>(currentState, event.getClass());
                Transition transition = transitionMap.get(validTransitionKey);
                if (transition != null) {
                    EventHandler eventHandlerFromClass = newEventHandlerFromClass(transition.getEventHandlerClass());
                    eventHandlerFromClass.handle(event);
                    model.getCurrentState().set(transition.getTo());
                }
            }
        } catch (Exception e) {
            log.error("Error at handling event.", e);
            throw new FsmException(e);
        }
    }

    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return handlerClass.getDeclaredConstructor().newInstance();
    }

    private void addTransition(Transition transition) {
        checkArgument(transition.isValid(), "Invalid transition. transition=%s", transition);
        Pair<State, Class<? extends Event>> pair = new Pair<>(transition.getFrom(), transition.getEventClass());
        checkArgument(!transitionMap.containsKey(pair),
                "A transition exists already with the state/event pair. pair=%s", pair);
        transitionMap.put(pair, transition);
    }

    public TransitionBuilder buildTransition() {
        return transitionBuilder;
    }

    public abstract void configTransitions();

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

        public TransitionBuilder run(Class<? extends EventHandler> eventHandlerClass) {
            transition.setEventHandlerClass(eventHandlerClass);
            return this;
        }

        public void to(State to) {
            transition.setTo(to);
            fsm.addTransition(transition);
        }
    }
}
