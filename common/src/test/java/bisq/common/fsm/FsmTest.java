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

import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FsmTest {
    // Standin for private payment-account data that a real domain event's generated toString() could contain
    // (e.g. BisqEasyAccountDataMessage / MuSig SendAccountPayloadMessage). Used by SentinelMockEvent below.
    private static final String SENTINEL_SECRET = "SECRET_PAYMENT_ACCOUNT_DATA_MUST_NOT_LEAK";

    @Test
    void testTransitions() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);

        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S3);
        fsm.addTransition()
                .from(MockState.S3)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.COMPLETED);

        fsm.handle(new MockEvent1(model, "test1"));
        assertEquals(MockState.S1, fsm.getModel().getState());
        assertEquals("test1", model.data);

        fsm.handle(new MockEvent1(model, "test2"));
        assertEquals(MockState.S2, fsm.getModel().getState());
        assertEquals("test2", model.data);

        fsm.handle(new MockEvent1(model, "test3"));
        assertEquals(MockState.S3, fsm.getModel().getState());
        assertEquals("test3", model.data);

        fsm.handle(new MockEvent1(model, "test4"));
        assertEquals(MockState.COMPLETED, fsm.getModel().getState());
        assertEquals("test4", model.data);

        model = new MockModel(MockState.INIT);
        fsm = new SimpleFsm<>(model);

        // No change in data as no handler was defined
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S1);

        assertEquals(MockState.INIT, fsm.getModel().getState());
        fsm.handle(new MockEvent1(model, "test1"));
        assertEquals(MockState.S1, fsm.getModel().getState());
        assertNull(fsm.getModel().data);

        // Transit with event handler called
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);
        assertEquals(MockState.S1, fsm.getModel().getState());
        fsm.handle(new MockEvent2(model, "test2"));
        assertEquals(MockState.S2, fsm.getModel().getState());
        assertEquals("test2", fsm.getModel().data);

        // Different source state, same event.
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S3);
        fsm.handle(new MockEvent2(model, "test3"));
        assertEquals(MockState.S3, fsm.getModel().getState());
        assertEquals("test3", fsm.getModel().data);
    }

    @Test
    void testOutOfOrderEvents() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);

        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent3.class)
                .run(MockEventHandler.class)
                .to(MockState.S3);
        fsm.addTransition()
                .from(MockState.S3)
                .on(MockEvent4.class)
                .run(MockEventHandler.class)
                .to(MockState.COMPLETED);

        fsm.handle(new MockEvent3(model, "test3"));
        assertEquals(MockState.INIT, fsm.getModel().getState());
        assertNull((fsm.getModel()).data);
        assertEquals(1, model.eventQueue.size());
        assertEquals(0, model.processedEvents.size());

        fsm.handle(new MockEvent2(model, "test2"));
        assertEquals(MockState.INIT, fsm.getModel().getState());
        assertNull((fsm.getModel()).data);
        assertEquals(2, model.eventQueue.size());
        assertEquals(0, model.processedEvents.size());

        // Now we trigger transition from INIT state and process the queued events
        // to arrive at S3
        fsm.handle(new MockEvent1(model, "test1"));
        assertEquals(MockState.S3, fsm.getModel().getState());
        assertEquals("test3", fsm.getModel().data);
        assertEquals(0, model.eventQueue.size());
        assertEquals(3, model.processedEvents.size());

        // At a final state we clear the eventQueue and processedEvents
        fsm.handle(new MockEvent4(model, "test_comp"));
        assertEquals(MockState.COMPLETED, fsm.getModel().getState());
        assertEquals("test_comp", fsm.getModel().data);
        assertEquals(0, model.eventQueue.size());
        assertEquals(0, model.processedEvents.size());
    }

    /**
     * Reproduces the recovery mechanism behind issue #1622 (a Bisq Easy trade getting stuck because an
     * out-of-order message sits forever in the FSM's event queue): a queued event, and the model's state,
     * are captured as if they had just been read back from persisted data (this is exactly what
     * {@code Trade}'s 3-arg constructor now feeds into {@link FsmModel#FsmModel(State, Set, Set)} when a
     * trade is restored from proto). We then rebuild a fresh Fsm around that restored model and call
     * {@link Fsm#drainEventQueue()} once, with no further event delivered, and confirm the queued event is
     * still applied - proving that recovery no longer depends on some later, unrelated live transition
     * (or, as happens in production today, an app restart happening to re-deliver the same message via
     * mailbox replay) to save the trade.
     */
    @Test
    void testDrainEventQueueAfterModelRestore() {
        // Session 1: an out-of-order event arrives before its prerequisite transition. It gets queued and the
        // enabling transition (INIT -> S1) never happens live in this session - mirrors the reported bug,
        // where the buyer's confirm-fiat-sent message arrives before the seller has processed the buyer's
        // btc-address message.
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);

        fsm.handle(new MockEvent2(model, "queued"));
        assertEquals(MockState.INIT, fsm.getModel().getState());
        assertEquals(1, model.getEventQueue().size());

        // "Restart": rebuild the model purely from the persisted snapshot (state + queue), exactly as
        // BisqEasyTrade#fromProto now does via Trade#pendingEventsFromProto. processedEvents deliberately starts
        // empty, mirroring production (see the Trade restore constructor) - it is never restored across a restart.
        // The restored state (S1) represents the enabling transition having already been persisted; only the
        // event queue itself was previously at risk of being silently dropped across a restart.
        MockModel restoredModel = new MockModel(MockState.S1, model.getEventQueue(), Set.of());
        SimpleFsm<MockModel> restoredFsm = new SimpleFsm<>(restoredModel);
        restoredFsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);

        // The restored queue must round-trip faithfully.
        assertEquals(1, restoredModel.getEventQueue().size());
        assertEquals(MockState.S1, restoredFsm.getModel().getState());

        // No further event is delivered - only the drain is triggered, as happens once at trade/protocol
        // reconstruction for a restored trade (BisqEasyTradeService#createAndAddTradeProtocol).
        restoredFsm.drainEventQueue();

        assertEquals(MockState.S2, restoredFsm.getModel().getState());
        // MockEventHandler mutates the model referenced by the event itself (the original, pre-restore model,
        // per the test fixture below) rather than the restoredFsm's model - this confirms the handler for the
        // queued MockEvent2 genuinely ran during the drain, not just a state placeholder change.
        assertEquals("queued", model.data);
        assertTrue(restoredFsm.getModel().getEventQueue().isEmpty());
    }

    @Test
    void testDrainEventQueueIsSafeAndIdempotentWhenEmpty() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);

        // Calling drainEventQueue() on an empty queue must be a safe no-op, and calling it repeatedly must not
        // change behaviour or throw (re-entrancy is guarded by the same reentrant model lock handle() uses).
        fsm.drainEventQueue();
        fsm.drainEventQueue();
        assertEquals(MockState.INIT, fsm.getModel().getState());

        fsm.handle(new MockEvent1(model, "test1"));
        assertEquals(MockState.S1, fsm.getModel().getState());

        fsm.drainEventQueue();
        fsm.drainEventQueue();
        assertEquals(MockState.S1, fsm.getModel().getState());
        assertEquals("test1", fsm.getModel().data);
    }

    /**
     * Regression/documentation guard for the bug itself: if the event queue is NOT carried over on restore
     * (the behaviour before this fix - {@code Trade} used to call the single-arg {@code FsmModel(State)}
     * constructor from {@code fromProto}), the queued event is lost forever. Calling drainEventQueue() cannot
     * recover data that was never restored in the first place; restoring the queue itself (see
     * {@link #testDrainEventQueueAfterModelRestore}) is what fixes issue #1622, not drainEventQueue() alone.
     */
    @Test
    void testEventIsLostForeverIfQueueIsNotRestored() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);

        fsm.handle(new MockEvent2(model, "queued"));
        assertEquals(1, model.getEventQueue().size());

        // "Restart" using only the single-arg constructor - the pre-fix behaviour: the queue is not restored.
        MockModel restoredModel = new MockModel(MockState.S1);
        SimpleFsm<MockModel> restoredFsm = new SimpleFsm<>(restoredModel);
        restoredFsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);

        assertTrue(restoredModel.getEventQueue().isEmpty());
        restoredFsm.drainEventQueue();

        // The trade is stuck forever: nothing but the original MockEvent2 instance (now gone) could unstick it.
        assertEquals(MockState.S1, restoredFsm.getModel().getState());
        assertNull(restoredFsm.getModel().data);
    }

    /**
     * Deterministic proof that {@link FsmModel#getStateAndEventQueueSnapshot()} captures {@code state} and
     * {@code eventQueue} atomically and independently of later mutation - the concurrency guarantee behind the
     * #4885 follow-up (making the persisted {state, eventQueue} pair atomic). A genuine data race between
     * Fsm#handle() and an off-thread persistence read is not something we can reproduce deterministically without
     * a flaky thread-interleaving test, so instead this pins the single-threaded contract the fix relies on:
     * a snapshot taken at time T must forever reflect exactly what was true at T, however the model changes after.
     */
    @Test
    void testStateAndEventQueueSnapshotIsAtomicAndIndependentOfLaterMutation() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);

        // MockEvent2 arrives before the enabling INIT -> S1 transition: no transition matches MockEvent2 from
        // INIT, so the Fsm parks it in the queue instead of applying it.
        fsm.handle(new MockEvent2(model, "queued"));
        assertEquals(MockState.INIT, model.getState());
        assertEquals(1, model.getEventQueue().size());

        // A snapshot taken now must reflect exactly this pre-transition reality.
        FsmModel.StateAndEventQueue snapshotBeforeTransition = model.getStateAndEventQueueSnapshot();
        assertEquals(MockState.INIT, snapshotBeforeTransition.state());
        assertEquals(1, snapshotBeforeTransition.eventQueue().size());

        // The enabling transition fires: state moves to S1, then the automatic post-transition drain immediately
        // re-applies the queued MockEvent2, advancing straight on to S2 and emptying the live queue - all within
        // this single handle() call.
        fsm.handle(new MockEvent1(model, "test1"));
        assertEquals(MockState.S2, model.getState());
        assertTrue(model.getEventQueue().isEmpty());

        // Load-bearing assertion: the snapshot taken BEFORE the transition must be completely unaffected by it.
        // This proves two things at once: (a) the eventQueue copy was defensive/independent - a reference into
        // the live CopyOnWriteArraySet would now appear empty, since the live queue was drained - and (b) state
        // and eventQueue were captured together as of the same instant, not via two independently-racy reads.
        assertEquals(MockState.INIT, snapshotBeforeTransition.state());
        assertEquals(1, snapshotBeforeTransition.eventQueue().size());

        // A fresh snapshot taken now must reflect the new, post-transition reality.
        FsmModel.StateAndEventQueue snapshotAfterTransition = model.getStateAndEventQueueSnapshot();
        assertEquals(MockState.S2, snapshotAfterTransition.state());
        assertTrue(snapshotAfterTransition.eventQueue().isEmpty());
    }

    /**
     * Pins the bounded-tryLock behaviour {@link FsmModel#getStateAndEventQueueSnapshot()} needs for the single,
     * JVM-wide {@code Persistence} executor thread (see {@code Trade#getTradeBuilder}): while a live transition
     * holds the model's lock for a long-running event handler (simulating blocking I/O, e.g. MuSig's gRPC calls),
     * a concurrent snapshot attempt must give up close to its bound instead of blocking indefinitely, and must
     * throw {@link SnapshotLockTimeoutException} rather than fall back to an unsynchronized (and possibly torn)
     * read. Once the transition completes and releases the lock, the very next snapshot attempt must succeed and
     * reflect the settled, post-transition state.
     */
    @Test
    void testGetStateAndEventQueueSnapshotTimesOutWhileHandleHoldsLockThenSucceedsAfterRelease() throws InterruptedException {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(BlockingMockEventHandler.class)
                .to(MockState.S1);

        BlockingMockEventHandler.started = new CountDownLatch(1);
        BlockingMockEventHandler.release = new CountDownLatch(1);
        try {
            Thread slowHandlerThread = new Thread(() -> fsm.handle(new MockEvent1(model, "slow")), "slow-handler");
            slowHandlerThread.start();
            assertTrue(BlockingMockEventHandler.started.await(2, TimeUnit.SECONDS), "handler never started");

            // The slow-handler thread now holds model's lock for the whole transition (Fsm#handle wraps
            // eventHandler.handle()). A concurrent bounded snapshot attempt - exactly what Trade#getTradeBuilder
            // does from the shared Persistence executor thread - must not block indefinitely.
            long startNanos = System.nanoTime();
            assertThrows(SnapshotLockTimeoutException.class, model::getStateAndEventQueueSnapshot);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            assertTrue(elapsedMs < 2000,
                    "tryLock must give up close to its bound, not block indefinitely; took " + elapsedMs + "ms");

            // Release the slow handler; the transition completes normally.
            BlockingMockEventHandler.release.countDown();
            slowHandlerThread.join(2000);
            assertEquals(MockState.S1, model.getState());

            // Once the lock is free again, the very next snapshot call must succeed and reflect the now-settled
            // state, proving this is a bounded wait, not a permanently broken lock.
            FsmModel.StateAndEventQueue snapshot = model.getStateAndEventQueueSnapshot();
            assertEquals(MockState.S1, snapshot.state());
            assertTrue(snapshot.eventQueue().isEmpty());
        } finally {
            BlockingMockEventHandler.started = null;
            BlockingMockEventHandler.release = null;
        }
    }

    /**
     * Follow-up to #4885 (privacy/retention, Henrik's review point): a transition reaching a final state clears
     * eventQueue/processedEvents in-memory (see Fsm#handle), which may hold sensitive, trade-specific network
     * messages. The persist call which follows that clear must be routed through {@link Fsm#persistOnFinalState()}
     * rather than the plain, potentially rate-limited {@link Fsm#persist()}, so that the wipe is not left stranded
     * on disk. This test pins that dispatch deterministically, without any real I/O or timing involved.
     */
    @Test
    void testPersistOnFinalStateIsUsedInsteadOfPersistWhenReachingFinalState() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.COMPLETED);

        // A normal, non-final transition must go through the ordinary (potentially rate-limited) persist().
        fsm.handle(new MockEvent1(model, "test1"));
        assertEquals(MockState.S1, fsm.getModel().getState());
        assertEquals(1, fsm.persistCallCount);
        assertEquals(0, fsm.persistOnFinalStateCallCount);

        // The completing transition must be routed through persistOnFinalState() instead - exactly once, and
        // the ordinary persist() must NOT additionally fire for this same call.
        fsm.handle(new MockEvent2(model, "test2"));
        assertEquals(MockState.COMPLETED, fsm.getModel().getState());
        assertEquals(1, fsm.persistCallCount);
        assertEquals(1, fsm.persistOnFinalStateCallCount);
    }

    /**
     * Follow-up to #4885 (privacy/retention, Henrik's review point): a stuck trade may never reach a final state,
     * so its parked out-of-order events - which can hold sensitive account-data network messages persisted with the
     * trade - would otherwise linger on disk indefinitely. {@link FsmModel#clearEventQueue()} is the seam the
     * data-retention/redaction pass uses to scrub them once a trade passes the redaction threshold. This pins that
     * it empties eventQueue but KEEPS processedEvents: the trade may still be live and receiving duplicate resends
     * of already-applied messages, and processedEvents is the dedup guard that stops such a resend from being
     * re-queued (and re-persisted) with sensitive content.
     */
    @Test
    void testClearEventQueueScrubsParkedEventsButKeepsProcessedEvents() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        // MockEvent2's only transition starts from S2, which is never reached - so it stays parked (the
        // stuck-trade shape) even after the MockEvent1 transition below succeeds.
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S3);

        fsm.handle(new MockEvent2(model, "queued"));
        assertEquals(MockState.INIT, model.getState());
        assertEquals(1, model.getEventQueue().size());

        // A successful transition records its event class in processedEvents (the duplicate-resend dedup guard).
        fsm.handle(new MockEvent1(model, "applied"));
        assertEquals(MockState.S1, model.getState());
        assertEquals(1, model.getEventQueue().size());
        assertTrue(model.getProcessedEvents().contains(MockEvent1.class));

        model.clearEventQueue();

        assertTrue(model.getEventQueue().isEmpty(), "clearEventQueue() must empty the parked event queue");
        assertTrue(model.getProcessedEvents().contains(MockEvent1.class),
                "clearEventQueue() must keep processedEvents - the dedup guard against duplicate resends " +
                        "re-queueing sensitive content on a still-live trade");
    }

    @Test
    void testRemoveQueuedEventsIfScrubsOnlyMatchingParkedEvents() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent2.class)
                .run(MockEventHandler.class)
                .to(MockState.S2);
        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent3.class)
                .run(MockEventHandler.class)
                .to(MockState.S3);

        // Both arrive before their enabling state, so the Fsm parks them (the stuck-trade shape).
        fsm.handle(new MockEvent2(model, "queued-2"));
        fsm.handle(new MockEvent3(model, "queued-3"));
        assertEquals(2, model.getEventQueue().size());

        // The selective scrub used by the restore-drain guard (drop events from a now-banned sender):
        // only matching events go, the rest stay queued for the drain.
        boolean removed = model.removeQueuedEventsIf(MockEvent2.class::isInstance);

        assertTrue(removed, "removeQueuedEventsIf must report that something was removed");
        assertEquals(1, model.getEventQueue().size(), "non-matching events must stay queued");
        assertTrue(model.getEventQueue().stream().anyMatch(MockEvent3.class::isInstance));

        assertFalse(model.removeQueuedEventsIf(MockEvent2.class::isInstance),
                "a second scrub with no matches must report nothing removed");
    }

    @Test
    void testFromAny() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);

        fsm.addTransition()
                .fromAny()
                .on(MockEvent2.class)
                .to(MockState.S2);
        fsm.addTransition()
                .fromAny()
                .on(MockEvent1.class)
                .to(MockState.S1);
        fsm.addTransition()
                .fromAny()
                .on(MockEvent3.class)
                .to(MockState.S3);

        fsm.handle(new MockEvent1(model, ""));
        assertEquals(MockState.S1, fsm.getModel().getState());
        fsm.handle(new MockEvent2(model, ""));
        assertEquals(MockState.S2, fsm.getModel().getState());
        fsm.handle(new MockEvent3(model, ""));
        assertEquals(MockState.S3, fsm.getModel().getState());

        // Trying to go to a prev state leads to an error
        fsm.handle(new MockEvent1(model, ""));
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
    }

    @Test
    void testFromStates() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .fromStates(MockState.INIT, MockState.S1, MockState.S2)
                .on(MockEvent2.class)
                .to(MockState.S3);

        fsm.handle(new MockEvent2(model, ""));
        assertEquals(MockState.S3, fsm.getModel().getState());

        model = new MockModel(MockState.S1);
        fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .fromStates(MockState.INIT, MockState.S1, MockState.S2)
                .on(MockEvent2.class)
                .to(MockState.S3);

        fsm.handle(new MockEvent2(model, ""));
        assertEquals(MockState.S3, fsm.getModel().getState());

        model = new MockModel(MockState.S2);
        fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .fromStates(MockState.INIT, MockState.S1, MockState.S2)
                .on(MockEvent2.class)
                .to(MockState.S3);

        fsm.handle(new MockEvent2(model, ""));
        assertEquals(MockState.S3, fsm.getModel().getState());
    }

    @Test
    void testErrorState() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        // FailingMockEventHandler thrown an error
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(FailingMockEventHandler.class)
                .to(MockState.S1);
        fsm.handle(new MockEvent1(model, ""));
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
    }

    @Test
    void testCyclicGraphFailing() {
        MockModel model = new MockModel(MockState.S1);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);

        fsm.addTransition()
                .from(MockState.S1)
                .on(MockEvent1.class)
                .to(MockState.S2);
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent2.class)
                .to(MockState.S1);

        fsm.handle(new MockEvent1(model, ""));
        assertEquals(MockState.S2, fsm.getModel().getState());

        // Going to a lower state is not permitted
        fsm.handle(new MockEvent2(model, ""));
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
    }

    @Test
    void testTransitionToLowerState() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);

        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S2);
        fsm.addTransition()
                .from(MockState.S2)
                .on(MockEvent2.class)
                .to(MockState.S1);

        fsm.handle(new MockEvent1(model, ""));
        assertEquals(MockState.S2, fsm.getModel().getState());

        // Going to a lower state is not permitted (S2->S1)
        fsm.handle(new MockEvent2(model, ""));
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
    }

    @Test
    void testNoStateTransition() {
        // No such event defined: No state change, no handler call
        MockModel model1 = new MockModel(MockState.INIT);
        Fsm<MockModel> fsm1 = new SimpleFsm<>(model1);
        fsm1.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S1);
        fsm1.handle(new MockEvent2(model1, "test1"));
        assertEquals(State.FsmState.ERROR, fsm1.getModel().getState());
        assertNull((fsm1.getModel()).data);

        // No transition got added
        // If no target state is set we do not create the transition, so no exception is thrown but no transition if
        // found at handle.
        MockModel model2 = new MockModel(MockState.INIT);
        Fsm<MockModel> fsm2 = new SimpleFsm<>(model2);
        fsm2.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class);
        fsm2.handle(new MockEvent1(model2, "test2"));
        assertEquals(State.FsmState.ERROR, fsm2.getModel().getState());
        assertNull((fsm2.getModel()).data);

        // If source state is already final we do not transit
        MockModel model3 = new MockModel(MockState.COMPLETED);
        Fsm<MockModel> fsm3 = new SimpleFsm<>(model3);
        fsm3.addTransition()
                .from(MockState.COMPLETED)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.POST);
        fsm3.handle(new MockEvent1(model3, "test3"));
        assertEquals(MockState.COMPLETED, fsm3.getModel().getState());
        assertNull((fsm3.getModel()).data);

        // Same event and state combination: No state change, no handler call
        MockModel model4 = new MockModel(MockState.INIT);
        Fsm<MockModel> fsm4 = new SimpleFsm<>(model4);
        fsm4.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm4.handle(new MockEvent1(model4, "test4"));
        assertEquals(MockState.S1, fsm4.getModel().getState());
        assertEquals("test4", fsm4.getModel().data);
        fsm4.handle(new MockEvent1(model4, "test5"));
        assertEquals(MockState.S1, fsm4.getModel().getState());
        assertEquals("test4", fsm4.getModel().data);

        // No state change as wrong event fired, no transition found for event
        MockModel model5 = new MockModel(MockState.INIT);
        Fsm<MockModel> fsm5 = new SimpleFsm<>(model5);
        fsm5.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class)
                .to(MockState.S1);
        fsm5.handle(new MockEvent2(model5, "test4"));
        assertEquals(State.FsmState.ERROR, fsm5.getModel().getState());
        assertNull((fsm5.getModel()).data);
    }

    @Test
    void testEventHandlerNotCalled() {
        // No EventHandlerClass defined
        MockModel model1 = new MockModel(MockState.INIT);
        Fsm<MockModel> fsm1 = new SimpleFsm<>(model1);
        fsm1.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .to(MockState.S1);
        fsm1.handle(new MockEvent1(model1, "test1"));
        assertEquals(MockState.S1, fsm1.getModel().getState());
        assertNull((fsm1.getModel()).data);
    }

    @Test
    void testInvalidConfigs() {
        // fromStates empty
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .fromStates()
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // fromStates empty
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            //noinspection ConfusingArgumentToVarargsMethod
            fsm.addTransition()
                    .fromStates()
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // from null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(null)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // missing from
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // on null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(null)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // missing on
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // run null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(null)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // to null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(null);
            fsm.handle(new MockEvent1(model, ""));
        });

        // to missing (not added as transition to fsm). We do not throw an error in the handle method but create an error state
        MockModel model1 = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm1 = new SimpleFsm<>(model1);
        fsm1.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class);
        fsm1.handle(new MockEvent1(model1, ""));
        assertEquals(State.FsmState.ERROR, fsm1.getModel().getState());

        // Initial state is null
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(null);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.handle(new MockEvent1(model, ""));
        });

        // Same source and target state
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.INIT);
            fsm.handle(new MockEvent1(model, ""));
        });

        // InvalidMockEventHandler constructor not matching defined constructor signature in newEventHandlerFromClass
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(InvalidMockEventHandler.class)
                    .to(MockState.INIT);
            fsm.handle(new MockEvent1(model, ""));
        });

        // same pair sourceState/event added
        Assertions.assertThrows(FsmConfigException.class, () -> {
            MockModel model = new MockModel(MockState.INIT);
            SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S1);
            fsm.addTransition()
                    .from(MockState.INIT)
                    .on(MockEvent1.class)
                    .run(MockEventHandler.class)
                    .to(MockState.S2);
            fsm.handle(new MockEvent1(model, ""));
        });
    }


    /**
     * The generic Fsm layer must never string-render a full
     * event instance - domain events (e.g. BisqEasyAccountDataMessage / MuSig SendAccountPayloadMessage) have
     * generated toString() output that can contain private payment-account data, and the FsmException message
     * built here (via {@code ExceptionUtil#getRootCauseMessage}) ends up both in the trade's persisted error
     * info and in the BisqEasyReportErrorMessage sent to the peer. This drives the no-transition-found path
     * (Fsm#handle's checkArgument), the one site whose message used to concatenate the event instance directly.
     */
    @Test
    void testNoTransitionExceptionMessageDoesNotLeakEventContent() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        // Deliberately no transition registered for SentinelMockEvent at all.

        fsm.handle(new SentinelMockEvent());

        assertNotNull(fsm.lastSwallowedException, "handle() must have hit the no-transition-found path");
        String message = fsm.lastSwallowedException.getMessage();
        assertTrue(message.contains(SentinelMockEvent.class.getSimpleName()),
                "message should still identify the event's class for diagnostics: " + message);
        assertFalse(message.contains(SENTINEL_SECRET),
                "message must not render the event instance itself: " + message);
    }

    /**
     * Same guard as above, for the OTHER generic catch site in Fsm#handle: an event handler throwing mid-transition.
     * The FSM layer's own log.error/FsmException construction must not add a leak here either - what the handler's
     * own exception message says is outside the FSM layer's control (it's generic, not domain-aware), but the
     * framework itself must not concatenate the event on top of that.
     */
    @Test
    void testHandlerExceptionMessageDoesNotLeakEventContent() {
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .from(MockState.INIT)
                .on(SentinelMockEvent.class)
                .run(SentinelThrowingMockEventHandler.class)
                .to(MockState.S1);

        fsm.handle(new SentinelMockEvent());

        assertNotNull(fsm.lastSwallowedException, "handle() must have hit the handler-exception path");
        String message = fsm.lastSwallowedException.getMessage();
        assertFalse(message.contains(SENTINEL_SECRET),
                "message must not render the event instance itself: " + message);
    }

    @Test
    void handleNullEventFailsWithInformativeCauseInsteadOfMaskingNpe() {
        // Regression: the error-path logging used to call event.getClass() on the null event. The
        // resulting raw NPE was not an FsmException, so it bypassed the FsmErrorEvent/error-state
        // handling entirely (and would blow through SimpleFsm's FsmException catch), masking the
        // informative "event must not be null" message.
        MockModel model = new MockModel(MockState.INIT);
        SimpleFsm<MockModel> fsm = new SimpleFsm<>(model);
        fsm.addTransition()
                .from(MockState.INIT)
                .on(MockEvent1.class)
                .run(MockEventHandler.class);

        fsm.handle(null);

        // The configured error handling ran: FsmException raised (captured by SimpleFsm), error
        // state entered, and the informative NPE preserved as the cause.
        assertNotNull(fsm.lastSwallowedException);
        assertInstanceOf(NullPointerException.class, fsm.lastSwallowedException.getCause());
        assertEquals("event must not be null", fsm.lastSwallowedException.getCause().getMessage());
        assertEquals(State.FsmState.ERROR, fsm.getModel().getState());
    }

    @Getter
    public enum MockState implements State {
        INIT,
        S1,
        S2,
        S3,
        COMPLETED(true),
        POST;
        private final boolean isFinalState;
        private final int ordinal;

        MockState() {
            this(false);
        }

        MockState(boolean isFinalState) {
            this.isFinalState = isFinalState;
            ordinal = ordinal();
        }
    }

    public static class MockEvent1 implements Event {
        private final MockModel model;
        private final String data;

        public MockEvent1(MockModel model, String data) {
            this.model = model;
            this.data = data;
        }
    }

    public static class MockEvent2 extends MockEvent1 {
        public MockEvent2(MockModel model, String data) {
            super(model, data);
        }
    }

    public static class MockEvent3 extends MockEvent1 {
        public MockEvent3(MockModel model, String data) {
            super(model, data);
        }
    }

    public static class MockEvent4 extends MockEvent1 {
        public MockEvent4(MockModel model, String data) {
            super(model, data);
        }
    }

    public static class MockEventHandler implements EventHandler<MockEvent1> {
        @Override
        public void handle(MockEvent1 event) {
            event.model.data = event.data;
        }
    }

    public static class InvalidMockEventHandler implements EventHandler<Event> {
        public InvalidMockEventHandler(String test) {
        }

        @Override
        public void handle(Event event) {
            if (event instanceof MockEvent1 mockEvent) {
                mockEvent.model.data = mockEvent.data;
            }
        }
    }

    public static class FailingMockEventHandler implements EventHandler<Event> {
        public FailingMockEventHandler() {
        }

        @Override
        public void handle(Event event) {
            throw new RuntimeException("event is FsmException");
        }
    }

    /**
     * Standin for a domain event whose generated toString() carries private data. Used to prove the generic
     * Fsm layer only ever renders the event's CLASS, never the instance itself (see
     * testNoTransitionExceptionMessageDoesNotLeakEventContent / testHandlerExceptionMessageDoesNotLeakEventContent).
     */
    public static class SentinelMockEvent implements Event {
        @Override
        public String toString() {
            return SENTINEL_SECRET;
        }
    }

    public static class SentinelThrowingMockEventHandler implements EventHandler<Event> {
        public SentinelThrowingMockEventHandler() {
        }

        @Override
        public void handle(Event event) {
            throw new RuntimeException("handler failed");
        }
    }

    /**
     * Simulates a slow/blocking event handler (e.g. MuSig's gRPC calls) to test that a concurrent, bounded
     * {@link FsmModel#getStateAndEventQueueSnapshot()} does not block indefinitely on the model lock this handler
     * holds for the whole transition. Handler classes are constructed reflectively via a no-arg constructor (see
     * {@link SimpleFsm#newEventHandlerFromClass}), so per-run coordination must be static - reset by the test
     * before and after use.
     */
    public static class BlockingMockEventHandler implements EventHandler<MockEvent1> {
        static volatile CountDownLatch started;
        static volatile CountDownLatch release;

        @Override
        public void handle(MockEvent1 event) {
            started.countDown();
            boolean released;
            try {
                released = release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while blocking", e);
            }
            if (!released) {
                throw new IllegalStateException("blocking handler was never released");
            }
            event.model.data = event.data;
        }
    }

    public static class MockModel extends FsmModel {
        public MockModel(MockState state) {
            super(state);
        }

        public MockModel(MockState state, String data) {
            super(state);
            this.data = data;
        }

        public MockModel(MockState state, Set<Event> eventQueue, Set<Class<? extends Event>> processedEvents) {
            super(state, eventQueue, processedEvents);
        }

        private String data = null;
    }

}
