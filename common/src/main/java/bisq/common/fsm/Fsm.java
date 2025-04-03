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
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Minimalistic finite state machine implementation inspired by <a href="https://github.com/j-easy/easy-states">easy-states</a>
 * <br/>
 * In case of out-of-order events we store the un-handled events (we do not persist it) and retry to apply those
 * pending states after the next state transition.
 * The handling of out-of-order events only support unique event/state pairs. The out-of-order handling does not
 * support the use of the same event for multiple transitions. Though that is not a restriction of the transition config.
 * <br/>
 * The Fsm does not allow cycle graphs or transitions to previous states. For determining the order of the states we
 * use getOrdinal() which returns in case of enums the ordinal.
 */
@Slf4j
public abstract class Fsm<M extends FsmModel> {
    private final Map<Pair<State, Class<? extends Event>>, Transition> transitionMap = new HashMap<>();
    @Getter
    protected final M model;

    protected Fsm(M model) {
        this.model = model;

        configErrorHandling();
        configTransitions();
    }

    abstract protected void configErrorHandling();

    abstract protected void configTransitions();

    public void handle(Event event) {
        synchronized (this) {
            try {
                checkNotNull(event, "event must not be null");
                State currentState = model.getState();
                checkNotNull(currentState, "currentState must not be null");
                if (currentState.isFinalState()) {
                    log.warn("We have reached the final state and do not allow further state transition. New event was: {}", event);
                    return;
                }
                log.info("Start transition from currentState {}", currentState);
                Class<? extends Event> eventClass = event.getClass();
                var transitionMapEntriesForEvent = findTransitionMapEntriesForEvent(eventClass);
                checkArgument(!transitionMapEntriesForEvent.isEmpty(), "No transition found for given event " + event);
                Optional<Transition> transition = findTransition(currentState, transitionMapEntriesForEvent);
                if (transition.isPresent()) {
                    State targetState = transition.get().getTargetState();
                    checkArgument(targetState.getOrdinal() > currentState.getOrdinal(),
                            "The target state ordinal must be higher than the current state ordinal. " +
                                    "currentState=%s, targetState=%s", currentState, targetState);
                    Optional<Class<? extends EventHandler>> eventHandlerClass = transition.get().getEventHandlerClass();
                    if (eventHandlerClass.isPresent()) {
                        EventHandler eventHandler = newEventHandlerFromClass(eventHandlerClass.get());
                        String eventHandlerName = eventHandler.getClass().getSimpleName();
                        log.info("Handle {} at {}", event.getClass().getSimpleName(), eventHandlerName);
                        eventHandler.handle(event);
                    }

                    log.info("Transition completed to new state {}", targetState);
                    model.setNewState(targetState);
                    model.eventQueue.remove(event);
                    if (targetState.isFinalState()) {
                        model.processedEvents.clear();
                        model.eventQueue.clear();
                    } else {
                        model.processedEvents.add(eventClass);
                        // Apply all pending events to see if any of those match our current state.
                        // If an exception is thrown by the processed pending event it will get thrown to the
                        // caller. This would be a different triggering event as the event which cause
                        // the exception (the one from the queue).
                        // Clone set to avoid ConcurrentModificationException
                        new HashSet<>(model.getEventQueue()).forEach(this::handle);
                    }
                } else {
                    log.info("We did not find a transition with state {} and event {}. " +
                                    "We add the event to the eventQueue for potential later processing.",
                            currentState, eventClass.getSimpleName());
                    // In case we get an event which does not match our current state we add the event to our
                    // event queue if the event was not already processed.
                    transitionMap.keySet().stream()
                            .filter(key -> key.getSecond().equals(eventClass) &&
                                    !model.processedEvents.contains(eventClass))
                            .forEach(e -> model.eventQueue.add(event));
                }
            } catch (Exception exception) {
                log.error("Error at handling {}.", event, exception);
                FsmException fsmException = new FsmException(exception, event);
                // In case of an exception we fire the FsmErrorEvent to trigger an error state.
                // We apply that only if the event which triggered the exception was not the FsmErrorEvent itself
                // to avoid potential recursive calls if the error handling code causes a follow-up exception.
                if (!(fsmException.getEvent() instanceof FsmErrorEvent)) {
                    handle(new FsmErrorEvent(fsmException));
                }
                // We throw the exception and leave further error handling to the concrete Fsm implementation.
                throw fsmException;
            }
        }
    }

    public TransitionBuilder<M> addTransition() {
        return new TransitionBuilder<>(this);
    }

    public TransitionBuilder<M> fromAny() {
        return new TransitionBuilder<>(this).from(State.FsmState.ANY);
    }

    public TransitionBuilder<M> from(State sourceState) {
        return new TransitionBuilder<>(this).fromStates(sourceState);
    }

    public TransitionBuilder<M> fromStates(State... sourceStates) {
        return new TransitionBuilder<>(this).fromStates(sourceStates);
    }

    // The description parma is not used, it serves in the protocol config to give additional context info about the path
    public TransitionBuilder<M> path(String description) {
        return new TransitionBuilder<>(this);
    }

    abstract protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException;

    private Set<Map.Entry<Pair<State, Class<? extends Event>>, Transition>> findTransitionMapEntriesForEvent(Class<? extends Event> eventClass) {
        return transitionMap.entrySet().stream()
                .filter(e -> e.getKey().getSecond().equals(eventClass))
                .collect(Collectors.toSet());
    }

    private Optional<Transition> findTransition(State currentState,
                                                Set<Map.Entry<Pair<State, Class<? extends Event>>, Transition>> transitionMapEntriesForEvent) {
        return transitionMapEntriesForEvent.stream()
                .filter(e -> e.getKey().getFirst().equals(currentState) || e.getKey().getFirst().isAnyState())
                .map(Map.Entry::getValue)
                .findAny();
    }

    private void insertTransition(Transition transition) {
        try {
            checkArgument(transition.isValid(), "Invalid transition. transition=%s", transition);
            transition.getSourceStates().forEach(sourceState -> {
                Pair<State, Class<? extends Event>> pair = new Pair<>(sourceState, transition.getEventClass());
                checkArgument(!transitionMap.containsKey(pair),
                        "A transition exists already with the state/event pair. pair=%s", pair);
                transitionMap.put(pair, transition);
            });
        } catch (IllegalArgumentException e) {
            throw new FsmConfigException(e);
        }
    }

    public static class TransitionBuilder<M extends FsmModel> {
        private final Transition transition;
        private final Fsm<M> fsm;

        private TransitionBuilder(Fsm<M> fsm) {
            this.fsm = fsm;
            transition = new Transition();
        }

        public TransitionBuilder<M> from(State sourceState) {
            if (sourceState == null) {
                throw new FsmConfigException("sourceState must not be null");
            }
            return fromStates(sourceState);
        }

        public TransitionBuilder<M> fromAny() {
            from(State.FsmState.ANY);
            return this;
        }

        public TransitionBuilder<M> fromStates(State... sourceStates) {
            if (sourceStates == null) {
                throw new FsmConfigException("sourceStates must not be null");
            }
            if (sourceStates.length == 0) {
                throw new FsmConfigException("sourceStates must not be empty");
            }
            transition.getSourceStates().clear();
            transition.getSourceStates().addAll(Set.of(sourceStates));
            return this;
        }

        public TransitionBuilder<M> on(Class<? extends Event> eventClass) {
            transition.setEventClass(eventClass);
            return this;
        }

        public TransitionBuilder<M> run(Class<? extends EventHandler> eventHandlerClass) {
            if (eventHandlerClass == null) {
                throw new FsmConfigException("eventHandlerClass must not be null");
            }
            transition.setEventHandlerClass(Optional.of(eventHandlerClass));
            return this;
        }

        public TransitionBuilder<M> to(State targetState) {
            transition.setTargetState(targetState);
            fsm.insertTransition(transition);
            return this;
        }

        // The paths param is not used. It is just to allow nesting the paths inside the branch for better readability.
        public TransitionBuilder<M> branch(Object... paths) {
            return this;
        }

        public TransitionBuilder<M> then() {
            return new TransitionBuilder<>(fsm);
        }
    }
}
