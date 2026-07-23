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

package bisq.persistence;

import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This implementation of PersistenceClient drops persist requests if they happen too frequently.
 * It registers a shutdown hook and persists at shutdown. If the JVM got terminated non-gracefully
 * (e.g. kill signal or JVM crash) the shutdown hook is not executed (but any other approach to write in such cases
 * would fail as well).
 * As there is no guarantee that the last data are persisted in case of such unexpected terminations, it should be only
 * used if data loss is not critical (e.g. network data) and when write frequency is rather high.
 */
@Slf4j
public abstract class RateLimitedPersistenceClient<T extends PersistableStore<T>> implements PersistenceClient<T> {
    // Max time the shutdown hook waits for its queued fallback write to confirm. Bounded so a stuck
    // write can't hang JVM shutdown indefinitely.
    private static final long SHUTDOWN_WRITE_AWAIT_MS = 2000;

    private volatile long lastWrite;
    // Throttle heuristic only: with the generation pair below, CORRECTNESS no longer depends on this flag,
    // so its benign races (two threads may both observe it false) cost at most an extra write.
    private volatile boolean writeInProgress;
    // Dirty state is DERIVED from this generation pair instead of a mutable boolean flag:
    //  - requestedGeneration is bumped on EVERY persist()/persistNow() entry, before the store snapshot is
    //    taken, so a write scheduled at generation N covers all state up to N.
    //  - persistedGeneration advances (monotonically, via max) only when a write confirms, to the generation
    //    its snapshot covered.
    //  - dirty <=> requestedGeneration > persistedGeneration.
    // This removes the check-then-act races a boolean flag had (an older write's completion could wipe the
    // dirty mark of a newer, never-written request), and it makes failed writes keep the store dirty for
    // free: a failure simply never advances persistedGeneration, so the shutdown fallback retries it.
    private final AtomicLong requestedGeneration = new AtomicLong();
    private final AtomicLong persistedGeneration = new AtomicLong();
    // Serializes generation bump -> snapshot capture -> handoff to the persistence executor, so snapshots
    // reach the (write-serializing) executor in generation order. Without it, an older call could capture
    // its snapshot, stall, and schedule AFTER a newer call's write - leaving stale state on disk while the
    // generation pair reports clean. Only an in-memory clone and an executor submit happen under this lock,
    // never disk I/O.
    private final Object scheduleLock = new Object();

    public RateLimitedPersistenceClient() {
        //todo (Critical) check if we want to use ShutdownHook here
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("RateLimitedPersistenceClient.shutdownHook-" + StringUtils.truncate(getPersistence().getStorePath(), 8));
            persistOnShutdown();
        }));
    }

    /**
     * True while the in-memory store holds state which no confirmed write has covered yet — because a request
     * was throttled away, a write is still in flight, or a write failed. Consulted by the shutdown hook to
     * decide whether the fallback synchronous write is needed.
     */
    public boolean isDropped() {
        return requestedGeneration.get() > persistedGeneration.get();
    }

    @Override
    public CompletableFuture<Boolean> persist() {
        synchronized (scheduleLock) {
            // Bump BEFORE the throttle decision and before any snapshot: if this request gets dropped, the store
            // is dirty by construction (requested > persisted) until some later write covers this generation.
            long myGeneration = requestedGeneration.incrementAndGet();
            boolean tooFast = System.currentTimeMillis() - lastWrite < getMaxWriteRateInMs();
            if (tooFast || writeInProgress) {
                return CompletableFuture.completedFuture(false);
            }
            lastWrite = System.currentTimeMillis();
            writeInProgress = true;
            CompletableFuture<Boolean> future = getPersistence()
                    .persistAsync(getPersistableStore().getClone())
                    .handle((nil, throwable) -> {
                        writeInProgress = false;
                        boolean success = throwable == null;
                        if (success) {
                            // The snapshot was taken after the bump to myGeneration, so it covers everything up
                            // to it. max() keeps persistedGeneration monotone; it can never claim a generation a
                            // newer request has already moved past.
                            persistedGeneration.accumulateAndGet(myGeneration, Math::max);
                        }
                        return success;
                    });
            return future;
        }
    }

    protected long getMaxWriteRateInMs() {
        return 1000;
    }

    /**
     * Persists immediately, bypassing the write-rate limiter that {@link #persist()} applies.
     * <br/>
     * Reserved for privacy/retention-critical writes where a throttled-and-dropped {@link #persist()} call could
     * leave sensitive data on disk for an unbounded time - e.g. a completed trade's pending FSM events, which are
     * cleared in-memory the instant the trade reaches a final state (see {@code bisq.common.fsm.Fsm#handle}), but
     * whose on-disk copy has no other guaranteed opportunity to catch up: the next unrelated persist() call may
     * never come (a completed trade has no further transitions), and the shutdown-hook flush this class registers
     * is best-effort only - it never runs on a JVM crash or {@code kill -9}.
     */
    public CompletableFuture<Boolean> persistNow() {
        synchronized (scheduleLock) {
            long myGeneration = requestedGeneration.incrementAndGet();
            lastWrite = System.currentTimeMillis();
            writeInProgress = true;
            CompletableFuture<Boolean> future = getPersistence()
                    .persistAsync(getPersistableStore().getClone())
                    .handle((nil, throwable) -> {
                        writeInProgress = false;
                        boolean success = throwable == null;
                        if (success) {
                            persistedGeneration.accumulateAndGet(myGeneration, Math::max);
                        }
                        return success;
                    });
            return future;
        }
    }

    // Package-private for testing
    void persistOnShutdown() {
        // An in-flight or failed write leaves requested > persisted, so this single check covers "write
        // still pending", "write failed" and "request throttled away" alike.
        if (!isDropped()) {
            return;
        }
        try {
            // Route the fallback through the SAME serialized executor as every other write (persistNow
            // bumps the generation, captures under scheduleLock and submits): queued behind any still-
            // pending write, it can never be overwritten by an older snapshot landing later. Bounded so
            // a congested queue can't hang JVM shutdown.
            boolean success = persistNow().get(SHUTDOWN_WRITE_AWAIT_MS, TimeUnit.MILLISECONDS);
            if (success) {
                return;
            }
            // The queued write failed (e.g. disk error) - retry directly below.
        } catch (TimeoutException e) {
            // The queued write may never run: the executor uses daemon threads, which the JVM stops
            // right after the shutdown hooks return. Fall through to the direct write - it is durable
            // before this hook returns, and Persistence's write-id guard makes it safe: if the stale
            // queued write does wake up later it is SKIPPED, not applied over the newer state.
            log.warn("The queued shutdown write did not confirm within {} ms; writing directly on the shutdown thread.",
                    SHUTDOWN_WRITE_AWAIT_MS);
        } catch (Exception e) {
            // Submission itself failed (e.g. the executor is already shut down) - direct write below.
            log.warn("Submitting the shutdown fallback write failed; falling back to a direct write.", e);
        }
        // Generation, snapshot AND the write ticket are all captured under scheduleLock - the identical
        // atomicity contract the async paths have (their ticket is assigned inside persistAsync, invoked
        // under this same lock). Reserving the ticket outside the lock would open a gap where a concurrent
        // persistNow() captures a NEWER snapshot yet ends up with an OLDER ticket, making the guard skip
        // the newer write as stale and falsely report it clean. Only the disk write stays outside the lock.
        long myGeneration;
        T snapshot;
        long writeId;
        synchronized (scheduleLock) {
            myGeneration = requestedGeneration.get();
            snapshot = getPersistableStore().getClone();
            writeId = getPersistence().reserveWriteId();
        }
        try {
            // Durable the moment this returns (shares the store's write monitor with the executor task, so
            // it cannot interleave with an in-flight write of this store). If a write with a NEWER ticket
            // already landed by now, this is skipped - correct: that ticket's snapshot was captured after
            // ours under the same lock, so strictly newer state is already on disk.
            getPersistence().persist(snapshot, writeId);
            persistedGeneration.accumulateAndGet(myGeneration, Math::max);
        } catch (Exception e) {
            // Persistence.persist() propagates write failures (so callers can't mistake failure for
            // durability); on the shutdown-hook thread there is nothing left to do but log.
            log.error("Direct write at shutdown failed.", e);
        }
    }
}
