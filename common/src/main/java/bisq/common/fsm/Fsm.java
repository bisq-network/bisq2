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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Enhanced finite state machine implementation with support for hierarchical FSMs.
 * Allows for elegant definition of complex state machines with nested sub-states.
 * Features event bubbling from child to parent FSMs when appropriate.
 * In case of out-of-order events we bubbled it to the top-FSM and store the un-handled
 * events (we do not persist it)
 */
@Slf4j
public abstract class Fsm<M extends FsmModel> {
    private final Map<Pair<State, Class<? extends Event>>, Transition> transitionMap = new HashMap<>();
    @Getter
    protected final M model;

    private final Map<State, StateFsmDefinition<?, ?>> stateFsms = new HashMap<>();
    private Fsm<?> activeFsm = null;
    private State activeFsmState = null;
    @Setter
    private Fsm<?> parent = null;

    protected Fsm(M model) {
        this.model = model;

        configErrorHandling();
        configTransitions();
    }

    abstract protected void configErrorHandling();

    abstract protected void configTransitions();

    protected <F extends FsmModel> void associateFsm(State state, Fsm<F> fsm,
                                                     State successTarget, State failureTarget) {
        stateFsms.put(state, new StateFsmDefinition<>(fsm, successTarget, failureTarget));
        fsm.setParent(this);
    }

    private boolean isChildFsm() {
        return parent != null;
    }


    public void handle(Event event) {
        synchronized (this) {
            try {
                checkNotNull(event, "event must not be null");
                State currentState = model.getState();
                checkNotNull(currentState, "currentState must not be null");
                if (currentState.isFinalState() && !(event instanceof FsmCompletedEvent)) {
                    log.warn("We have reached the final state and do not allow further state transition. New event was: {}", event);
                    return;
                }

                log.info("Start transition from currentState {}", currentState);

                if (activeFsm != null) {
                    activeFsm.handle(event);

                    if (isFsmInFinalState(activeFsm)) {
                        handleFsmCompletion();

                        if (!(event instanceof FsmCompletedEvent)) {
                            handleInMainFsm(event);
                            checkForNewStateFsm();
                        }
                    }
                    return;
                }

                handleInMainFsm(event);

                checkForNewStateFsm();

            } catch (Exception exception) {
                log.error("Error at handling {}.", event, exception);
                FsmException fsmException = new FsmException(exception, event);
                if (!(fsmException.getEvent() instanceof FsmErrorEvent)) {
                    handle(new FsmErrorEvent(fsmException));
                }
                throw fsmException;
            }
        }
    }

    private void handleInMainFsm(Event event) {
        State currentState = model.getState();
        Class<? extends Event> eventClass = event.getClass();

        var transitionMapEntriesForEvent = findTransitionMapEntriesForEvent(eventClass);
        if (transitionMapEntriesForEvent.isEmpty()) {
            log.info("No transition found for given event {} in FSM", event);

            if (isChildFsm()) {
                log.info("No transition for event {} in child FSM, bubbling up",
                        eventClass.getSimpleName());
                return;
            }

            log.warn("No transition found for event {} at any level, transitioning to ERROR",
                    eventClass.getSimpleName());
            model.setNewState(State.FsmState.ERROR);
            return;
        }

        Optional<Transition> transition = findTransition(currentState, transitionMapEntriesForEvent);
        if (transition.isPresent()) {
            State targetState = transition.get().getTargetState();

            // Check if the target state has a lower or equal ordinal than the current state
            if (targetState.getOrdinal() <= currentState.getOrdinal()) {
                log.error("Invalid state transition attempted: The target state ordinal must be higher than the current state ordinal. " +
                        "currentState={}, targetState={}", currentState, targetState);
                model.setNewState(State.FsmState.ERROR);
                return;
            }

            Optional<Class<? extends EventHandler>> eventHandlerClass = transition.get().getEventHandlerClass();
            if (eventHandlerClass.isPresent()) {
                try {
                    EventHandler eventHandler = newEventHandlerFromClass(eventHandlerClass.get());
                    String eventHandlerName = eventHandler.getClass().getSimpleName();
                    log.info("Handle {} at {}", event.getClass().getSimpleName(), eventHandlerName);
                    eventHandler.handle(event);
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    log.error("Failed to instantiate event handler: {}", eventHandlerClass.get().getSimpleName(), e);
                    throw new RuntimeException("Failed to instantiate event handler", e);
                }
            }

            Optional<EventHandler> eventHandler = transition.get().getEventHandler();
            eventHandler.ifPresent(handler -> handler.handle(event));

            log.info("Transition completed to new state {}", targetState);
            model.setNewState(targetState);
            model.eventQueue.remove(event);

            if (targetState.isFinalState()) {
                model.processedEvents.clear();
                model.eventQueue.clear();
            } else {
                model.processedEvents.add(eventClass);
                // Apply all pending events to see if any of those match our current state.
                new HashSet<>(model.getEventQueue()).forEach(this::handle);
            }
        } else {
            log.info("We did not find a transition with state {} and event {}. " +
                            "We add the event to the eventQueue for potential later processing.",
                    currentState, eventClass.getSimpleName());

            transitionMap.keySet().stream()
                    .filter(key -> key.getSecond().equals(eventClass) &&
                            !model.processedEvents.contains(eventClass))
                    .forEach(e -> model.eventQueue.add(event));
        }
    }

    private void handleFsmCompletion() {
        log.info("State-specific FSM completed in state: {}", activeFsm.getModel().getState());

        StateFsmDefinition<?, ?> definition = stateFsms.get(activeFsmState);

        boolean isSuccess = isFsmInSuccessState(activeFsm);
        State nextState = isSuccess ? definition.getSuccessTarget() : definition.getFailureTarget();

        log.info("FSM completed with {}, transitioning main FSM to: {}",
                isSuccess ? "success" : "failure", nextState);

        Fsm<?> completedFsm = activeFsm;
        activeFsm = null;
        activeFsmState = null;

        FsmCompletedEvent completionEvent = new FsmCompletedEvent(completedFsm, isSuccess);

        // Directly set the next state
        model.setNewState(nextState);

        // Handle the completion event to trigger any transitions that might be waiting for it
        Optional<Transition> transition = findTransitionForEvent(nextState, FsmCompletedEvent.class);
        if (transition.isPresent()) {
            handleInMainFsm(completionEvent);
        }
    }

    private void checkForNewStateFsm() {
        State currentState = model.getState();

        if (stateFsms.containsKey(currentState) && activeFsm == null) {
            activateFsmForState(currentState);
        }
    }

    private void activateFsmForState(State state) {
        if (stateFsms.containsKey(state)) {
            activeFsmState = state;
            activeFsm = stateFsms.get(state).getFsm();
            log.info("Activated FSM for state: {}, initial sub-state: {}",
                    state, activeFsm.getModel().getState());
        }
    }

    private Optional<Transition> findTransitionForEvent(State state, Class<? extends Event> eventClass) {
        var entries = findTransitionMapEntriesForEvent(eventClass);
        return findTransition(state, entries);
    }

    private boolean isFsmInFinalState(Fsm<?> fsm) {
        return fsm.getModel().getState().isFinalState();
    }

    private boolean isFsmInSuccessState(Fsm<?> fsm) {
        return fsm.getModel().getState().isSuccessState();
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

    /**
     * Event fired when a state-specific FSM completes.
     */
    public static class FsmCompletedEvent implements Event {
        @Getter
        private final Fsm<?> completedFsm;
        private final boolean success;

        public FsmCompletedEvent(Fsm<?> completedFsm, boolean success) {
            this.completedFsm = completedFsm;
            this.success = success;
        }
    }

    /**
     * Definition of an FSM associated with a specific state, including
     * the transitions to take when the FSM completes.
     */
    @Getter
    private static class StateFsmDefinition<S extends State, F extends FsmModel> {
        private final Fsm<F> fsm;
        private final State successTarget;
        private final State failureTarget;

        public StateFsmDefinition(Fsm<F> fsm, State successTarget, State failureTarget) {
            this.fsm = fsm;
            this.successTarget = successTarget;
            this.failureTarget = failureTarget;
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

        public TransitionBuilder<M> run(EventHandler eventHandler) {
            if (eventHandler == null) {
                throw new FsmConfigException("eventHandler must not be null");
            }
            transition.setEventHandler(Optional.of(eventHandler));
            return this;
        }

        public TransitionBuilder<M> to(State targetState) {
            transition.setTargetState(targetState);
            fsm.insertTransition(transition);
            return this;
        }

        public ChildFsmBuilder<M> withFSM() {
            if (transition.getTargetState() == null) {
                throw new FsmConfigException("Target state must be set before defining a child FSM");
            }

            return new ChildFsmBuilder<>(fsm, transition.getTargetState(), this);
        }

        // The paths param is not used. It is just to allow nesting the paths inside the branch for better readability.
        public TransitionBuilder<M> branch(Object... paths) {
            return this;
        }

        public TransitionBuilder<M> then() {
            return new TransitionBuilder<>(fsm);
        }
    }

    public static class ChildFsmBuilder<M extends FsmModel> {
        private final Fsm<M> parentFsm;
        private final State parentSourceState;
        private final TransitionBuilder<M> parentBuilder;

        private State initialState;
        private State errorTargetState;
        private Class<? extends Event> errorEventClass;
        private final Set<TransitionDefinition> transitions = new HashSet<>();
        private State successTarget;
        private State failureTarget;

        ChildFsmBuilder(Fsm<M> parentFsm, State parentSourceState, TransitionBuilder<M> parentBuilder) {
            this.parentFsm = parentFsm;
            this.parentSourceState = parentSourceState;
            this.parentBuilder = parentBuilder;
        }

        public ChildFsmBuilder<M> initialState(State initialState) {
            this.initialState = initialState;
            return this;
        }

        public ChildFsmBuilder<M> errorHandler(Class<? extends Event> errorEventClass, State errorTargetState) {
            this.errorEventClass = errorEventClass;
            this.errorTargetState = errorTargetState;
            return this;
        }

        public TransitionDefinitionBuilder transition() {
            return new TransitionDefinitionBuilder(this);
        }

        public ChildFsmBuilder<M> onSuccess(State successTarget) {
            this.successTarget = successTarget;
            return this;
        }

        public ChildFsmBuilder<M> onFailure(State failureTarget) {
            this.failureTarget = failureTarget;
            return this;
        }

        public TransitionBuilder<M> then() {
            buildAndAssociateFsm();
            return parentBuilder;
        }

        private void buildAndAssociateFsm() {
            // Create the child FSM
            Fsm<FsmModel> childFsm = new Fsm<>(new FsmModel(initialState)) {
                @Override
                protected void configErrorHandling() {
                    if (errorEventClass != null && errorTargetState != null) {
                        fromAny()
                                .on(errorEventClass)
                                .to(errorTargetState);
                    }
                }

                @Override
                protected void configTransitions() {
                    for (TransitionDefinition transition : transitions) {
                        from(transition.getSourceState())
                                .on(transition.getEventClass())
                                .run(transition.getHandlerClass())
                                .to(transition.getTargetState());
                    }
                }

                @Override
                protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass)
                        throws NoSuchMethodException, InvocationTargetException, InstantiationException,
                        IllegalAccessException {
                    return parentFsm.newEventHandlerFromClass(handlerClass);
                }
            };

            parentFsm.associateFsm(parentSourceState, childFsm, successTarget, failureTarget);
        }

        public class TransitionDefinitionBuilder {
            private final ChildFsmBuilder<M> parent;
            private State sourceState;
            private Class<? extends Event> eventClass;
            private Class<? extends EventHandler> handlerClass;
            private State targetState;

            TransitionDefinitionBuilder(ChildFsmBuilder<M> parent) {
                this.parent = parent;
            }

            public TransitionDefinitionBuilder from(State sourceState) {
                this.sourceState = sourceState;
                return this;
            }

            public TransitionDefinitionBuilder on(Class<? extends Event> eventClass) {
                this.eventClass = eventClass;
                return this;
            }

            public TransitionDefinitionBuilder run(Class<? extends EventHandler> handlerClass) {
                this.handlerClass = handlerClass;
                return this;
            }

            public ChildFsmBuilder<M> to(State targetState) {
                this.targetState = targetState;
                addTransition();
                return parent;
            }

            private void addTransition() {
                TransitionDefinition transition = new TransitionDefinition(
                        sourceState, eventClass, handlerClass, targetState);
                parent.transitions.add(transition);
            }
        }

        @Getter
        private static class TransitionDefinition {
            private final State sourceState;
            private final Class<? extends Event> eventClass;
            private final Class<? extends EventHandler> handlerClass;
            private final State targetState;

            TransitionDefinition(State sourceState,
                                 Class<? extends Event> eventClass,
                                 Class<? extends EventHandler> handlerClass,
                                 State targetState) {
                this.sourceState = sourceState;
                this.eventClass = eventClass;
                this.handlerClass = handlerClass;
                this.targetState = targetState;
            }
        }
    }
}
