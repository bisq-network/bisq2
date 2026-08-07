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

import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

@Slf4j
@ToString
@EqualsAndHashCode
public class FsmModel {
    /**
     * Bound for {@link #getStateAndEventQueueSnapshot()}'s {@code tryLock}. Chosen short enough that the single,
     * JVM-wide {@code Persistence} executor thread (see {@code bisq.persistence.Persistence#EXECUTOR}) can never
     * be stuck for long on one trade's live transition - which would otherwise queue up every other store's
     * writes behind it - yet long enough to comfortably clear the lock's normal, fast holders (a transition's
     * state mutation plus a non-blocking persist() submission). A timed-out snapshot fails this write cycle
     * rather than risk a torn read; RateLimitedPersistenceClient's generation pair keeps the store dirty so the
     * next persist()/shutdown fallback retries - by which time a merely-slow (not stuck) handler has typically
     * released the lock.
     */
    static final long SNAPSHOT_LOCK_TIMEOUT_MS = 500;

    private final Observable<State> state = new Observable<>();

    // Explicit lock replacing the previous intrinsic-monitor use (synchronized(model) in Fsm#handle()/
    // drainEventQueue(), synchronized methods below): ReentrantLock, unlike synchronized, offers a bounded
    // tryLock(timeout), which getStateAndEventQueueSnapshot() below needs. Every party that used to synchronize
    // on the model instance must now go through this SAME lock object - Fsm#handle()/drainEventQueue() included -
    // or mutual exclusion between a live transition and a persistence-facing read silently breaks.
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    final ReentrantLock lock = new ReentrantLock();

    // Package visibility for access from Fsm mutating the collections.
    // CopyOnWriteArraySet rather than a plain HashSet, as defense-in-depth for any reader that bypasses
    // getStateAndEventQueueSnapshot() and calls getEventQueue()/getProcessedEvents() directly without acquiring
    // the model's monitor (Fsm#handle()/Fsm#drainEventQueue() now synchronize on the model instance itself - see
    // getStateAndEventQueueSnapshot() below for why). Concurrently iterating a plain HashSet while another thread
    // structurally mutates it is undefined behavior (ConcurrentModificationException or a torn read). Both sets
    // are always small (a handful of pending FSM events per trade at most) and read far more often than mutated,
    // which is exactly CopyOnWriteArraySet's sweet spot; it also gives every such reader (including
    // Fsm#drainEventQueue()'s defensive copy-before-iterate, which a plain HashSet copy does not actually make
    // safe under concurrent mutation) consistent snapshot-iterator semantics for free, without requiring any
    // external synchronization. The primary, recommended mechanism for a cross-thread, all-fields-consistent read
    // (e.g. Trade#getTradeBuilder during persistence) is getStateAndEventQueueSnapshot(), not these getters.
    final Set<Event> eventQueue = new CopyOnWriteArraySet<>();
    final Set<Class<? extends Event>> processedEvents = new CopyOnWriteArraySet<>();

    public FsmModel(State initialState) {
        if (initialState == null) {
            throw new FsmConfigException("initialState must not be null");
        }
        state.set(initialState);
    }

    public FsmModel(State initialState, Set<Event> eventQueue, Set<Class<? extends Event>> processedEvents) {
        if (initialState == null) {
            throw new FsmConfigException("initialState must not be null");
        }
        state.set(initialState);
        this.eventQueue.addAll(eventQueue);
        this.processedEvents.addAll(processedEvents);
    }

    public ReadOnlyObservable<State> stateObservable() {
        return state;
    }

    public State getState() {
        return state.get();
    }

    // Only called from FSM
    void setNewState(State newState) {
        state.set(newState);
    }

    public Set<Event> getEventQueue() {
        return Collections.unmodifiableSet(eventQueue);
    }

    public Set<Class<? extends Event>> getProcessedEvents() {
        return Collections.unmodifiableSet(processedEvents);
    }

    /**
     * Returns an atomic, defensively-copied snapshot of {@code state} and {@code eventQueue} taken together.
     * <br/>
     * {@link Fsm#handle(Event)} and {@link Fsm#drainEventQueue()} take the same {@link #lock} (not a lock on the
     * {@code Fsm} instance) precisely so that this method - callable directly on the model, e.g. from
     * {@code bisq.trade.Trade#getTradeBuilder} during persistence, which runs on a different thread than the Fsm's
     * own transitions (persistence is cloned/serialized asynchronously, and for MuSig trades the message-handling
     * itself runs on a dedicated executor - see {@code MuSigTradeService#handleMuSigTradeMessage}) - can take the
     * same lock and therefore never observe a torn read.
     * <br/>
     * Reading {@link #getState()} and {@link #getEventQueue()} via two separate, unsynchronized calls can tear:
     * a persistence snapshot could capture the state AFTER a transition together with the just-applied event
     * STILL in the queue (causing a double-apply on restore), or the state BEFORE the transition with the event
     * already removed (causing the event to be silently lost). Capturing both fields in one atomically-locked
     * method closes that gap.
     * <br/>
     * Unlike {@link Fsm#handle(Event)}/{@link Fsm#drainEventQueue()}, which take the lock unbounded (transition
     * correctness cannot be short-circuited), this method bounds its wait via {@code tryLock}: it is called from
     * {@code Trade#getTradeBuilder}, which runs on the single, JVM-wide {@code Persistence} executor thread. A live
     * transition can legitimately hold {@link #lock} for as long as its event handler takes (including blocking
     * I/O - e.g. MuSig's gRPC calls), and that thread must never block on it indefinitely: doing so would queue up
     * every other store's disk writes in the JVM behind this one trade. On timeout this throws
     * {@link SnapshotLockTimeoutException} instead of falling back to an unsynchronized read - a torn read is
     * exactly the bug this method exists to prevent - so the write fails for this cycle and retries once the
     * generation pair notices it's still dirty (see {@code RateLimitedPersistenceClient}).
     * <br/>
     * The returned {@code eventQueue} is a defensive copy ({@link Set#copyOf}): it is independent of the live,
     * mutable {@code eventQueue} field, so a later transition can never retroactively change a snapshot that was
     * already taken and handed to a caller.
     */
    public StateAndEventQueue getStateAndEventQueueSnapshot() {
        boolean acquired;
        try {
            acquired = lock.tryLock(SNAPSHOT_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting up to {}ms to acquire the FsmModel lock during serialization; " +
                    "write cycle will retry. modelClass={}", SNAPSHOT_LOCK_TIMEOUT_MS, getClass().getSimpleName());
            throw new SnapshotLockTimeoutException(this, SNAPSHOT_LOCK_TIMEOUT_MS, e);
        }
        if (!acquired) {
            log.warn("FsmModel lock not acquired within {}ms during serialization - a live transition is likely " +
                    "holding it; write cycle will retry. modelClass={}", SNAPSHOT_LOCK_TIMEOUT_MS, getClass().getSimpleName());
            throw new SnapshotLockTimeoutException(this, SNAPSHOT_LOCK_TIMEOUT_MS);
        }
        try {
            return new StateAndEventQueue(getState(), Set.copyOf(eventQueue));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears the pending out-of-order event queue (and {@code processedEvents}) under the same lock used by
     * {@link #getStateAndEventQueueSnapshot()}, mirroring the final-state cleanup in {@link Fsm#handle(Event)}.
     * Takes the lock unbounded, like {@link Fsm#handle(Event)}: this is called from the trade services, not from
     * the persistence-serialization path, so there is no shared-executor-stall concern to bound against here.
     * <br/>
     * Used by the trade data-retention/redaction pass: the queue can hold sensitive account-data network messages
     * (e.g. {@code BisqEasyAccountDataMessage} / MuSig {@code SendAccountPayloadMessage}) which are persisted with
     * the trade. A stuck trade may never reach a final state to clear them, so they must be scrubbed once the trade
     * passes the redaction threshold, otherwise the persisted copy would linger on disk indefinitely.
     */
    public void clearEventQueue() {
        lock.lock();
        try {
            eventQueue.clear();
            processedEvents.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes queued out-of-order events matching {@code filter}, under the same lock as
     * {@link #getStateAndEventQueueSnapshot()}. Returns true if anything was removed (so callers know a
     * persist is warranted). Takes the lock unbounded - see {@link #clearEventQueue()}.
     * <br/>
     * Used by the restore-drain guard in the trade services: a queued trade message passed validation when it
     * was originally received, but its sender may have been banned before the restart. Draining applies queued
     * events directly through the FSM - bypassing the {@code onMessage()} banned-sender check - so such events
     * must be scrubbed before the drain, mirroring what {@code onMessage()} would do with a live message.
     */
    public boolean removeQueuedEventsIf(Predicate<Event> filter) {
        lock.lock();
        try {
            return eventQueue.removeIf(filter);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Immutable {state, eventQueue} pair captured atomically by {@link #getStateAndEventQueueSnapshot()}.
     */
    public record StateAndEventQueue(State state, Set<Event> eventQueue) {
    }
}