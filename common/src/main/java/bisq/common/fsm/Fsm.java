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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j

public class Fsm {
    private final Map<Pair<State, Class<? extends Event>>, Transition> transitionMap = new HashMap<>();
    private final Object lock = new Object();
    @Getter
    private final FsmModel model;

    public Fsm(State initialState) {
        this(new FsmModel(initialState));
    }

    public Fsm(FsmModel model) {
        this.model = model;
        configTransitions();
    }

    protected void configTransitions() {
        // Subclasses might use that for transition config
    }

    public void handle(Event event) throws FsmException {
        try {
            checkNotNull(event, "event must not be null");
            synchronized (lock) {
                State currentState = model.getState();
                checkNotNull(currentState, "currentState must not be null");
                if (currentState.isFinalState()) {
                    log.warn("We have reached the final state and do not allow further state transition");
                    return;
                }
                Pair<State, Class<? extends Event>> validTransitionKey = new Pair<>(currentState, event.getClass());
                Transition transition = transitionMap.get(validTransitionKey);
                if (transition != null) {
                    Optional<Class<? extends EventHandler>> eventHandlerClass = transition.getEventHandlerClass();
                    if (eventHandlerClass.isPresent()) {
                        EventHandler eventHandlerFromClass = newEventHandlerFromClass(eventHandlerClass.get());
                        eventHandlerFromClass.handle(event);
                    }
                    model.setNewState(transition.getTargetState());
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

    private void addTransition(Transition transition) throws FsmException {
        try {
            checkArgument(transition.isValid(), "Invalid transition. transition=%s", transition);
            Pair<State, Class<? extends Event>> pair = new Pair<>(transition.getSourceState(), transition.getEventClass());
            checkArgument(!transitionMap.containsKey(pair),
                    "A transition exists already with the state/event pair. pair=%s", pair);
            synchronized (lock) {
                transitionMap.put(pair, transition);
            }
        } catch (Exception e) {
            throw new FsmException(e);
        }
    }

    public TransitionBuilder addTransition() {
        return new TransitionBuilder(this);
    }

    public static class TransitionBuilder {
        private final Transition transition;
        private final Fsm fsm;

        private TransitionBuilder(Fsm fsm) {
            this.fsm = fsm;
            transition = new Transition();
        }

        public TransitionBuilder from(State sourceState) {
            transition.setSourceState(sourceState);
            return this;
        }

        public TransitionBuilder on(Class<? extends Event> eventClass) {
            transition.setEventClass(eventClass);
            return this;
        }

        public TransitionBuilder run(Class<? extends EventHandler> eventHandlerClass) {
            try {
                transition.setEventHandlerClass(Optional.of(eventHandlerClass));
            } catch (Exception e) {
                throw new FsmException(e);
            }
            return this;
        }

        public void to(State targetState) {
            transition.setTargetState(targetState);
            fsm.addTransition(transition);
        }
    }
}
