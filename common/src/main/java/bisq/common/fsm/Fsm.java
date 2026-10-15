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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Minimalistic finite state machine implementation inspired by <a href="https://github.com/j-easy/easy-states">easy-states</a>
 * <br/>
 * In case of out-of-order events we store the un-handled events (at most one per event type; subclasses may persist
 * them) and retry to apply those pending states after the next state transition.
 * The handling of out-of-order events only support unique event/state pairs. The out-of-order handling does not
 * support the use of the same event for multiple transitions. Though that is not a restriction of the transition config.
 * <br/>
 * State-changing transitions do not allow cycle graphs or transitions to previous states. For determining the order
 * of the states we use getOrdinal() which returns in case of enums the ordinal. Internal transitions can process an
 * event without changing the current state.
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

    public <E extends Event> void handle(E event) {
        // The Fsm monitor is the primary lock: callers serialize multi-step work with event handling via
        // synchronized (protocol) (e.g. MuSigTradeService). The model lock is taken second - always in this order.
        synchronized (this) {
            handleUnderModelLock(event);
        }
    }

    private <E extends Event> void handleUnderModelLock(E event) {
        // The model lock serves readers which only hold the model, not this Fsm (e.g. a persistence snapshot of
        // {state, eventQueue}, see FsmModel#getStateAndEventQueueSnapshot): holding it for the whole transition
        // means such a snapshot is never taken mid-transition. A live transition takes it unbounded; only the
        // snapshot side uses a bounded tryLock.
        model.lock.lock();
        try {
            // Tracks whether this call reached a final state, so the finally block below can route the mandatory
            // post-transition persist() call through persistOnFinalState() instead of the normal persist(). This
            // matters for privacy/retention: a final state clears eventQueue/processedEvents
            // in-memory a few lines below, and that wipe must reach disk promptly - persistOnFinalState() bypasses
            // any write-rate limiting a subclass may apply to the ordinary persist(), so the wipe is not silently
            // dropped and left stranded on disk for an indefinite time.
            boolean reachedFinalState = false;
            try {
                checkNotNull(event, "event must not be null");
                State currentState = model.getState();
                checkNotNull(currentState, "currentState must not be null");
                if (currentState.isFinalState()) {
                    log.warn("We have reached the final state and do not allow further state transition. New event was: {}",
                            describe(event));
                    return;
                }
                log.info("Start transition from currentState {}", currentState);
                Class<? extends Event> eventClass = event.getClass();
                var transitionMapEntriesForEvent = findTransitionMapEntriesForEvent(eventClass);
                checkArgument(!transitionMapEntriesForEvent.isEmpty(), "No transition found for given event " + describe(event));
                Optional<Transition> transition = findTransition(currentState, transitionMapEntriesForEvent);
                if (transition.isPresent()) {
                    Transition selectedTransition = transition.orElseThrow();
                    Optional<State> targetState = selectedTransition.getTargetState();
                    if (!selectedTransition.isInternal()) {
                        State newState = targetState.orElseThrow();
                        checkArgument(newState.getOrdinal() > currentState.getOrdinal(),
                                "The target state ordinal must be higher than the current state ordinal. " +
                                        "currentState=%s, targetState=%s", currentState, newState);
                    }
                    Optional<Class<? extends EventHandler<? extends Event>>> eventHandlerClass =
                            selectedTransition.getEventHandlerClass();
                    if (eventHandlerClass.isPresent()) {
                        @SuppressWarnings("unchecked")
                        EventHandler<E> eventHandler = newEventHandlerFromClass((Class<? extends EventHandler<E>>) eventHandlerClass.get());
                        String eventHandlerName = eventHandler.getClass().getSimpleName();
                        log.info("Handle {} at {}", event.getClass().getSimpleName(), eventHandlerName);
                        eventHandler.handle(event);
                    }

                    model.eventQueue.remove(event);
                    if (selectedTransition.isInternal()) {
                        log.info("Internal transition completed at state {}", currentState);
                    } else {
                        State newState = targetState.orElseThrow();
                        log.info("Transition completed to new state {}", newState);
                        model.setNewState(newState);
                        if (newState.isFinalState()) {
                            model.processedEvents.clear();
                            model.eventQueue.clear();
                            reachedFinalState = true;
                        } else {
                            model.processedEvents.add(eventClass);
                            // Apply all pending events to see if any of those match our current state.
                            // If an exception is thrown by the processed pending event it will get thrown to the
                            // caller. This would be a different triggering event as the event which cause
                            // the exception (the one from the queue).
                            drainEventQueue();
                        }
                    }
                } else {
                    log.info("We did not find a transition with state {} and event {}. " +
                                    "We add the event to the eventQueue for potential later processing unless it " +
                                    "was processed already or an event of that type is queued already.",
                            currentState, eventClass.getSimpleName());
                    // In case we get an event which does not match our current state we add the event to our
                    // event queue if the event was not already processed. At most one event per type is queued:
                    // out-of-order handling only supports unique event/state pairs, so a second event of a queued
                    // type could never be applied, and an unbounded queue would let a peer grow the trade data
                    // (which subclasses may persist) at will. The first one is kept.
                    if (!model.processedEvents.contains(eventClass) &&
                            model.eventQueue.stream().noneMatch(queued -> queued.getClass().equals(eventClass))) {
                        model.eventQueue.add(event);
                    }
                }
            } catch (Exception exception) {
                log.error("Error at handling {}.", describe(event), exception);
                FsmException fsmException = new FsmException(exception, event);
                // In case of an exception we fire the FsmErrorEvent to trigger an error state.
                // We apply that only if the event which triggered the exception was not the FsmErrorEvent itself
                // to avoid potential recursive calls if the error handling code causes a follow-up exception.
                if (!(event instanceof FsmErrorEvent)) {
                    persist();
                    handle(new FsmErrorEvent(fsmException));
                }
                // We throw the exception to allow specific error handling to the implementation class.
                throw fsmException;
            } finally {
                if (reachedFinalState) {
                    persistOnFinalState();
                } else {
                    persist();
                }
            }
        } finally {
            model.lock.unlock();
        }
    }

    /**
     * Persistence hook invoked after every handled event (and from the error path).
     */
    protected abstract void persist();

    /**
     * Called instead of {@link #persist()} exactly once, when a transition reaches a final state. The default
     * implementation just delegates to {@link #persist()}; override to bypass any write-rate limiting the
     * concrete {@link #persist()} implementation applies (see {@code bisq.persistence.RateLimitedPersistenceClient}).
     * <br/>
     * Reaching a final state clears {@code eventQueue}/{@code processedEvents} in-memory a few lines above in
     * {@link #handle(Event)}; those may hold sensitive, trade-specific network messages. Without a guaranteed,
     * unthrottled flush here, a rate-limited persist() call could be silently dropped, leaving the (already
     * in-memory-wiped) sensitive data sitting on disk for an unbounded time - potentially until the process's
     * next unrelated persist() call, or its next clean shutdown, neither of which is guaranteed to happen promptly
     * (or at all, on a crash/kill -9).
     */
    protected void persistOnFinalState() {
        persist();
    }

    /**
     * Re-attempts to handle every event currently sitting in the event queue against the current state.
     * <br/>
     * This is called automatically after any successful transition (see {@link #handle(Event)}), but it is
     * also safe - and sometimes necessary - to call explicitly, e.g. once right after a model has been restored
     * from persisted data: the event queue itself is not guaranteed to be persisted for every {@link FsmModel}
     * subclass, so events which arrived out of order before a restart may otherwise never be re-applied because
     * no further live transition ever occurs for that trade.
     * <br/>
     * Safe to call when the queue is empty (no-op) and safe to call multiple times. Exceptions from handling a
     * queued event behave exactly as for a live event: each event goes through the concrete {@link #handle(Event)}
     * implementation, so whether an {@link FsmException} propagates to the caller or is absorbed is up to the
     * subclass (e.g. the trade protocols swallow it and surface an error state instead).
     */
    public void drainEventQueue() {
        // Same lock order as handle(). Both locks are reentrant, so the nested handle(event) calls below are free.
        synchronized (this) {
            model.lock.lock();
            try {
                // Copy since handle() mutates model.eventQueue; a list keeps the arrival order for the replay.
                List.copyOf(model.getEventQueue()).forEach(this::handle);
            } finally {
                model.lock.unlock();
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

    abstract protected <E extends Event> EventHandler<E> newEventHandlerFromClass(Class<? extends EventHandler<E>> handlerClass)
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

        public TransitionBuilder<M> run(Class<? extends EventHandler<? extends Event>> eventHandlerClass) {
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

        /**
         * Runs an internal transition without changing the current state. The event is consumed without recording its
         * class as processed, so later events of the same class can still be handled.
         */
        public TransitionBuilder<M> stay() {
            transition.markInternal();
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

    // Events can be network messages, for example trade messages with the payment account data of the peer. Users
    // share their log files, and exception messages reach the logs and error reports. So we name only the type.
    private static String describe(Event event) {
        return event == null ? "null" : event.getClass().getSimpleName();
    }
}
