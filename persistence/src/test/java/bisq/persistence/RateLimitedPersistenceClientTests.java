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

import bisq.common.fsm.FsmModel;
import bisq.common.fsm.SnapshotLockTimeoutException;
import bisq.common.fsm.State;
import bisq.common.proto.ProtoResolver;
import bisq.persistence.backup.MaxBackupSize;
import bisq.persistence.backup.RestoreService;
import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class RateLimitedPersistenceClientTests {

    @Test
    void persistNowKeepsDirtyUntilWriteConfirms(@TempDir Path tempDirPath) {
        var persistence = new ManualPersistence(tempDirPath);
        var client = new TestClient(persistence);
        var inFlightWrite = new CompletableFuture<Void>();
        persistence.nextWrite.set(inFlightWrite);

        CompletableFuture<Boolean> result = client.persistNow();
        // No optimistic clear: while the write is in flight the store must still count as dirty.
        assertThat(client.isDropped()).isTrue();

        inFlightWrite.complete(null);
        assertThat(result.join()).isTrue();
        assertThat(client.isDropped()).isFalse();
    }

    @Test
    void persistNowSuccessDoesNotClearDropMarkedByNewerDroppedPersist(@TempDir Path tempDirPath) {
        var persistence = new ManualPersistence(tempDirPath);
        var client = new TestClient(persistence);
        var inFlightWrite = new CompletableFuture<Void>();
        persistence.nextWrite.set(inFlightWrite);

        CompletableFuture<Boolean> result = client.persistNow();

        // A newer persist request arrives while the write is still in flight and gets dropped
        // (writeInProgress). Its state is NOT covered by the in-flight snapshot.
        assertThat(client.persist().join()).isFalse();
        assertThat(client.isDropped()).isTrue();

        inFlightWrite.complete(null);
        assertThat(result.join()).isTrue();
        // The older write's success must not mark the newer, never-written state as clean -
        // otherwise the shutdown hook would skip its fallback write and silently lose it.
        assertThat(client.isDropped()).isTrue();
    }

    @Test
    void persistNowKeepsDirtyWhenWriteFails(@TempDir Path tempDirPath) {
        var persistence = new ManualPersistence(tempDirPath);
        var client = new TestClient(persistence);
        var inFlightWrite = new CompletableFuture<Void>();
        persistence.nextWrite.set(inFlightWrite);

        CompletableFuture<Boolean> result = client.persistNow();

        inFlightWrite.completeExceptionally(new IOException("disk full"));
        assertThat(result.join()).isFalse();
        assertThat(client.isDropped()).isTrue();
    }

    @Test
    void successfulOrdinaryPersistMarksStoreClean(@TempDir Path tempDirPath) {
        var persistence = new ManualPersistence(tempDirPath);
        var client = new TestClient(persistence);
        var inFlightWrite = new CompletableFuture<Void>();
        persistence.nextWrite.set(inFlightWrite);

        CompletableFuture<Boolean> result = client.persist();
        // Dirty while in flight - clean only once the write confirms (no optimistic clear).
        assertThat(client.isDropped()).isTrue();

        inFlightWrite.complete(null);
        assertThat(result.join()).isTrue();
        assertThat(client.isDropped()).isFalse();
    }

    @Test
    void failedOrdinaryPersistKeepsStoreDirtyAndShutdownRetriesIt(@TempDir Path tempDirPath) {
        var persistence = new ManualPersistence(tempDirPath);
        var client = new TestClient(persistence);
        var inFlightWrite = new CompletableFuture<Void>();
        persistence.nextWrite.set(inFlightWrite);

        CompletableFuture<Boolean> result = client.persist();
        inFlightWrite.completeExceptionally(new IOException("disk full"));
        assertThat(result.join()).isFalse();

        // A failed ordinary persist() must leave the store dirty - previously it was marked clean at
        // schedule time and a failure never restored the flag, so shutdown skipped its fallback write.
        assertThat(client.isDropped()).isTrue();

        client.persistOnShutdown();
        // The retry is routed through the serialized write queue (so it cannot be overtaken by an older
        // queued write); what matters is the outcome: the failed request's generation ends up covered.
        assertThat(client.isDropped()).isFalse();
    }

    /**
     * persist()/persistNow() update lastWrite BEFORE the asynchronous write even
     * starts. Before this fix, a FAILED write left lastWrite exactly as if it had succeeded, so the generation
     * pair correctly kept the store dirty (isDropped()==true) but nothing ACTIVELY retried it: the very next
     * ORDINARY persist() call - within getMaxWriteRateInMs() of the failed attempt - would be silently dropped
     * (tooFast) instead of retrying, and only an explicit persistNow(), the shutdown fallback, or some unrelated
     * call landing outside the throttle window would ever close the gap. No persistNow(), no sleep here - the
     * retry must go through the plain ordinary path.
     */
    @Test
    void failedOrdinaryPersistRollsBackThrottleSoNextOrdinaryPersistRetriesWithoutPersistNow(@TempDir Path tempDirPath) {
        var persistence = new ManualPersistence(tempDirPath);
        var client = new TestClient(persistence);
        var inFlightWrite = new CompletableFuture<Void>();
        persistence.nextWrite.set(inFlightWrite);

        CompletableFuture<Boolean> firstResult = client.persist();
        inFlightWrite.completeExceptionally(new IOException("disk full"));
        assertThat(firstResult.join()).isFalse();
        assertThat(client.isDropped()).isTrue();

        // Immediately after - well within getMaxWriteRateInMs() - a plain ordinary persist() call.
        CompletableFuture<Boolean> secondResult = client.persist();
        assertThat(secondResult.join())
                .as("the failed write's rollback must let the very next ordinary persist() pass the tooFast check")
                .isTrue();
        assertThat(client.isDropped()).isFalse();
    }

    /**
     * Control for the fix above: the rollback must apply to FAILURES only. A successful write still has to
     * consume the rate-limit budget exactly as before, or persist() stops rate-limiting altogether.
     */
    @Test
    void successfulOrdinaryPersistStillEnforcesRateLimitOnNextCall(@TempDir Path tempDirPath) {
        var persistence = new ManualPersistence(tempDirPath);
        var client = new TestClient(persistence);
        var inFlightWrite = new CompletableFuture<Void>();
        persistence.nextWrite.set(inFlightWrite);

        CompletableFuture<Boolean> firstResult = client.persist();
        inFlightWrite.complete(null);
        assertThat(firstResult.join()).isTrue();
        assertThat(client.isDropped()).isFalse();

        // Immediately after a SUCCESSFUL write, a second ordinary persist() must still be throttled.
        CompletableFuture<Boolean> secondResult = client.persist();
        assertThat(secondResult.join())
                .as("a successful write must still consume the rate-limit budget - the rollback is failure-only")
                .isFalse();
        // requestedGeneration is bumped BEFORE the throttle check on every persist() call, dropped or not (by
        // design, unrelated to this fix - see persist()'s own comment) - so isDropped() correctly flips back to
        // true here: this second call represents a real, not-yet-covered persist REQUEST. What this test pins is
        // narrower: unlike the failure case, this drop is a genuine throttle (tooFast), not something the
        // rollback should have prevented - a later persist()/persistNow() call still needs to actually run to
        // clear it.
        assertThat(client.isDropped()).isTrue();
    }

    @Test
    void olderSnapshotCannotBeWrittenAfterNewerOne(@TempDir Path tempDirPath) throws Exception {
        var persistence = new ManualPersistence(tempDirPath);
        var store = new BlockingCloneStore();
        var client = new TestClient(persistence, store);

        // Thread A: an ordinary persist() whose snapshot capture stalls - simulating a thread paused
        // between capturing its (older) snapshot and handing it to the persistence executor.
        store.armBlockOnNextClone();
        Thread olderPersist = new Thread(() -> client.persist(), "older-persist");
        olderPersist.start();
        store.awaitCloneEntered();

        // The store advances, and a newer unthrottled persistNow() arrives (e.g. a trade completed).
        store.bumpVersion();
        Thread newerPersistNow = new Thread(() -> client.persistNow(), "newer-persistNow");
        newerPersistNow.start();
        // Unfixed code: the newer call is not ordered w.r.t. the stalled older one and finishes now.
        // Fixed code: it parks until the older capture+schedule completes - either way this join returns.
        newerPersistNow.join(500);

        store.releaseClone();
        olderPersist.join(2000);
        newerPersistNow.join(2000);

        // Whatever the interleaving, the LAST snapshot written must be the newest store state. The
        // failure mode being pinned here: disk ends at the stale older snapshot while the generation
        // pair claims clean - a silent, undetected data loss.
        assertThat(persistence.writtenVersions).isNotEmpty();
        assertThat(persistence.writtenVersions.get(persistence.writtenVersions.size() - 1))
                .isEqualTo(store.currentVersion());
        assertThat(client.isDropped()).isFalse();
    }

    @Test
    void shutdownFallbackCannotBeOvertakenByQueuedOlderWrite(@TempDir Path tempDirPath) throws Exception {
        var persistence = new ManualPersistence(tempDirPath);
        var store = new BlockingCloneStore();
        var client = new TestClient(persistence, store);

        // An older async write is submitted but its actual disk write is still queued on the executor.
        var olderWriteGate = new CompletableFuture<Void>();
        persistence.nextWrite.set(olderWriteGate);
        client.persist();

        // The store advances past the queued snapshot, then the JVM shuts down.
        store.bumpVersion();
        Thread shutdownHook = new Thread(client::persistOnShutdown, "shutdown-hook");
        shutdownHook.start();
        // Let the shutdown path run its course (it awaits the in-flight write with a bounded timeout).
        shutdownHook.join(8_000);

        // The queued older write finally lands - after shutdown already wrote its newer fallback state.
        olderWriteGate.complete(null);

        // Both writes settle; whatever the interleaving, the disk must end at the NEWEST state. The bug
        // being pinned: the fallback bypassed the write queue, so the late v1 write overwrote its v2.
        long deadline = System.currentTimeMillis() + 5_000;
        while (persistence.writtenVersions.size() < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertThat(persistence.writtenVersions).isNotEmpty();
        assertThat(persistence.writtenVersions.get(persistence.writtenVersions.size() - 1))
                .isEqualTo(store.currentVersion());
    }

    @Test
    void shutdownFallbackWritesDroppedStateAndSurvivesWriteFailure(@TempDir Path tempDirPath) {
        var persistence = new ManualPersistence(tempDirPath);
        var client = new TestClient(persistence);
        var inFlightWrite = new CompletableFuture<Void>();
        persistence.nextWrite.set(inFlightWrite);

        client.persistNow();
        inFlightWrite.completeExceptionally(new IOException("disk full"));
        assertThat(client.isDropped()).isTrue();

        // The executor is already gone at this point in the shutdown sequence: submission is rejected, so
        // the synchronous last-resort write runs - and its failure must not propagate (on the real
        // shutdown-hook thread there is nobody left to handle it).
        persistence.rejectAsyncWrites = true;
        persistence.failSyncPersist = true;
        client.persistOnShutdown();
        assertThat(persistence.syncPersistCalls.get()).isEqualTo(1);
    }

    /**
     * End-to-end proof for the FsmModel#getStateAndEventQueueSnapshot() bounded-tryLock fix: a real
     * SnapshotLockTimeoutException thrown from serialization (toAny(), the entry point
     * PersistableStoreReaderWriter#writeStoreToFilePath actually calls - see PersistenceWriteGuardTests'
     * GatedSerializationStore for the same seam), driven through the REAL Persistence/PersistableStoreReaderWriter
     * write path (not the ManualPersistence stand-in above), proves this PR's own machinery already handles it
     * correctly with no further changes needed: the failed write leaves the client dirty (isDropped()==true) and
     * does not corrupt disk, and the next unthrottled retry (mirroring Fsm#persistOnFinalState's persistNow()
     * route, or the shutdown fallback) succeeds once the simulated lock contention has cleared.
     */
    @Test
    void snapshotLockTimeoutDuringSerializationKeepsClientDirtyAndRetries(@TempDir Path tempDirPath) {
        registerTimestampStoreResolver();
        var writePersistence = new Persistence<FlakySerializationStore>(tempDirPath, "TimestampStore", MaxBackupSize.ZERO, new RestoreService());
        var failNextSerialization = new AtomicBoolean(true);
        var store = new FlakySerializationStore(failNextSerialization, 1L);
        var client = new RealPersistenceTestClient(writePersistence, store);

        // First write cycle: the model lock is (simulated as) held by a live transition past the bounded wait.
        assertThat(client.persistNow().join()).isFalse();
        assertThat(client.isDropped()).isTrue();
        assertThat(readVersion(tempDirPath)).isEmpty();

        // The transition has since released the lock (simulated by arming the store to succeed): the next
        // write cycle's retry must succeed and reach disk.
        store.bumpVersion(2L);
        assertThat(client.persistNow().join()).isTrue();
        assertThat(client.isDropped()).isFalse();
        assertThat(readVersion(tempDirPath)).contains(2L);
    }

    /**
     * end-to-end: same SnapshotLockTimeoutException-driven failure as the sibling
     * test above, but the retry this time is a single plain, ORDINARY persist() call - no persistNow(), no
     * sleep - proving the rollback-on-failure fix closes the retry gap through the exact real-world path
     * (AuthenticatedDataStorageService.add()/remove()/refresh() etc. only ever call the ordinary persist()).
     */
    @Test
    void snapshotLockTimeoutDuringSerializationRetriesOnNextOrdinaryPersistWithoutPersistNow(@TempDir Path tempDirPath) {
        registerTimestampStoreResolver();
        var writePersistence = new Persistence<FlakySerializationStore>(tempDirPath, "TimestampStore", MaxBackupSize.ZERO, new RestoreService());
        var failNextSerialization = new AtomicBoolean(true);
        var store = new FlakySerializationStore(failNextSerialization, 1L);
        var client = new RealPersistenceTestClient(writePersistence, store);

        // First write cycle: the model lock is (simulated as) held by a live transition past the bounded wait.
        assertThat(client.persist().join()).isFalse();
        assertThat(client.isDropped()).isTrue();
        assertThat(readVersion(tempDirPath)).isEmpty();

        // The transition has since released the lock (simulated by arming the store to succeed). A single
        // ORDINARY persist() call, immediately after and well within getMaxWriteRateInMs(), must retry and land
        // on disk
        store.bumpVersion(2L);
        assertThat(client.persist().join()).isTrue();
        assertThat(client.isDropped()).isFalse();
        assertThat(readVersion(tempDirPath)).contains(2L);
    }

    // Reads back through a separately-typed Persistence<TimestampStore> pointed at the same file: the bytes on
    // disk are genuinely a TimestampStore proto (FlakySerializationStore#toAny() packs via the delegate), resolved
    // through the global registry - reading them back as the self-typed FlakySerializationStore would be an
    // unchecked, erasure-hidden cast to the wrong runtime type.
    private static java.util.Optional<Long> readVersion(Path tempDirPath) {
        var readPersistence = new Persistence<TimestampStore>(tempDirPath, "TimestampStore", MaxBackupSize.ZERO, new RestoreService());
        return readPersistence.read().map(store -> store.getTimestampsByProfileId().get("version"));
    }

    private static void registerTimestampStoreResolver() {
        try {
            PersistableStoreResolver.addResolver(new TimestampStore().getResolver());
        } catch (Exception e) {
            // Already registered by another test in this JVM - fine.
        }
    }

    /**
     * Store whose serialization ({@link #toAny()}) throws a real {@link SnapshotLockTimeoutException} exactly
     * once (while armed), simulating FsmModel#getStateAndEventQueueSnapshot()'s bounded tryLock giving up on a
     * live transition, then serializes normally - simulating the transition having released the lock by the next
     * write cycle. Self-typed as {@code PersistableStore<FlakySerializationStore>} (rather than wrapping
     * TimestampStore as its type parameter) so that {@link RateLimitedPersistenceClient#persist()}'s
     * {@code getPersistableStore().getClone()} returns an object whose {@code toAny()} is still this flaky
     * behaviour - exactly like BisqEasyTradeStore#getClone()'s shallow copy still reaches the same live trades.
     */
    private static final class FlakySerializationStore implements PersistableStore<FlakySerializationStore> {
        private final TimestampStore delegate = new TimestampStore();
        private final AtomicBoolean failNextSerialization;

        private FlakySerializationStore(AtomicBoolean failNextSerialization, long version) {
            this.failNextSerialization = failNextSerialization;
            delegate.getTimestampsByProfileId().put("version", version);
        }

        void bumpVersion(long version) {
            delegate.getTimestampsByProfileId().put("version", version);
        }

        @Override
        public Any toAny() {
            if (failNextSerialization.compareAndSet(true, false)) {
                throw new SnapshotLockTimeoutException(new FsmModel(State.FsmState.ERROR), 500);
            }
            return Any.pack(delegate.toProto(false));
        }

        @Override
        public bisq.persistence.protobuf.TimestampStore toProto(boolean serializeForHash) {
            return delegate.toProto(serializeForHash);
        }

        @Override
        public bisq.persistence.protobuf.TimestampStore.Builder getBuilder(boolean serializeForHash) {
            return delegate.getBuilder(serializeForHash);
        }

        @Override
        public FlakySerializationStore getClone() {
            // Shallow: shares the same delegate and flag, like BisqEasyTradeStore#getClone()'s Set.copyOf of live
            // trade references - the point being pinned is that the CLONE's serialization can still fail/block on
            // live state, not that this test harness needs a deep copy.
            return this;
        }

        @Override
        public void applyPersisted(FlakySerializationStore persisted) {
            delegate.applyPersisted(persisted.delegate);
        }

        @Override
        public ProtoResolver<PersistableStore<?>> getResolver() {
            return delegate.getResolver();
        }
    }

    private static final class RealPersistenceTestClient extends RateLimitedPersistenceClient<FlakySerializationStore> {
        private final Persistence<FlakySerializationStore> persistence;
        private final PersistableStore<FlakySerializationStore> store;

        private RealPersistenceTestClient(Persistence<FlakySerializationStore> persistence, PersistableStore<FlakySerializationStore> store) {
            this.persistence = persistence;
            this.store = store;
        }

        @Override
        public Persistence<FlakySerializationStore> getPersistence() {
            return persistence;
        }

        @Override
        public PersistableStore<FlakySerializationStore> getPersistableStore() {
            return store;
        }
    }

    private static final class ManualPersistence extends Persistence<TimestampStore> {
        final AtomicReference<CompletableFuture<Void>> nextWrite = new AtomicReference<>();
        final AtomicInteger syncPersistCalls = new AtomicInteger();
        // Versions carried by the snapshots in DISK-WRITE order: async writes are FIFO-chained the way the
        // real (serialized, single-threaded) persistence executor runs them, and each records its version
        // when its write COMPLETES; the synchronous persist() records at call time, like the real direct
        // write. The last element is therefore what would be left on disk.
        final List<Long> writtenVersions = new CopyOnWriteArrayList<>();
        volatile boolean failSyncPersist;
        // Simulates the persistence executor being already shut down: submissions are rejected.
        volatile boolean rejectAsyncWrites;
        private CompletableFuture<Void> writeQueueTail = CompletableFuture.completedFuture(null);

        private ManualPersistence(Path directoryPath) {
            super(directoryPath, "TimestampStore", MaxBackupSize.ZERO, new RestoreService());
        }

        @Override
        public synchronized CompletableFuture<Void> persistAsync(TimestampStore serializable) {
            if (rejectAsyncWrites) {
                throw new java.util.concurrent.RejectedExecutionException("executor already shut down");
            }
            Long version = serializable.getTimestampsByProfileId().get(BlockingCloneStore.VERSION_KEY);
            CompletableFuture<Void> manual = nextWrite.getAndSet(null);
            CompletableFuture<Void> trigger = manual != null ? manual : CompletableFuture.completedFuture(null);
            // FIFO like the real executor: this write runs only after every previously submitted write.
            CompletableFuture<Void> write = writeQueueTail
                    .exceptionally(prior -> null) // an earlier failed write doesn't block the queue
                    .thenCompose(prior -> trigger)
                    .thenRun(() -> {
                        if (version != null) {
                            writtenVersions.add(version);
                        }
                    });
            writeQueueTail = write;
            return write;
        }

        @Override
        protected void doWrite(TimestampStore persistableStore) {
            syncPersistCalls.incrementAndGet();
            if (failSyncPersist) {
                throw new RuntimeException("Couldn't write persistable store to disk.");
            }
            Long version = persistableStore.getTimestampsByProfileId().get(BlockingCloneStore.VERSION_KEY);
            if (version != null) {
                writtenVersions.add(version);
            }
        }
    }

    /**
     * Store whose {@link #getClone()} tags each snapshot with the store version at capture time and can
     * stall a single capture on demand - the hook the ordering regression test uses to hold an older
     * snapshot "in hand" while a newer call races past it.
     */
    private static final class BlockingCloneStore implements PersistableStore<TimestampStore> {
        static final String VERSION_KEY = "version";

        private final TimestampStore delegate = new TimestampStore();
        private final AtomicLong version = new AtomicLong(1);
        // Armed latch is CLAIMED (taken and cleared) by the first clone that sees it, so exactly one
        // capture stalls; later captures run through unblocked.
        private final AtomicReference<CountDownLatch> blockNextClone = new AtomicReference<>();
        private volatile CountDownLatch armedLatch;
        private final CountDownLatch cloneEntered = new CountDownLatch(1);

        void armBlockOnNextClone() {
            armedLatch = new CountDownLatch(1);
            blockNextClone.set(armedLatch);
        }

        void awaitCloneEntered() throws InterruptedException {
            if (!cloneEntered.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("clone was never entered");
            }
        }

        void releaseClone() {
            CountDownLatch latch = armedLatch;
            if (latch != null) {
                latch.countDown();
            }
        }

        void bumpVersion() {
            version.incrementAndGet();
        }

        long currentVersion() {
            return version.get();
        }

        @Override
        public TimestampStore getClone() {
            long capturedVersion = version.get();
            CountDownLatch latch = blockNextClone.getAndSet(null);
            if (latch != null) {
                cloneEntered.countDown();
                try {
                    if (!latch.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("blocked clone was never released");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            TimestampStore snapshot = new TimestampStore();
            snapshot.getTimestampsByProfileId().put(VERSION_KEY, capturedVersion);
            return snapshot;
        }

        @Override
        public void applyPersisted(TimestampStore persisted) {
            delegate.applyPersisted(persisted);
        }

        @Override
        public bisq.persistence.protobuf.TimestampStore toProto(boolean serializeForHash) {
            return delegate.toProto(serializeForHash);
        }

        @Override
        public bisq.persistence.protobuf.TimestampStore.Builder getBuilder(boolean serializeForHash) {
            return delegate.getBuilder(serializeForHash);
        }

        @Override
        public ProtoResolver<PersistableStore<?>> getResolver() {
            return delegate.getResolver();
        }
    }

    private static final class TestClient extends RateLimitedPersistenceClient<TimestampStore> {
        private final ManualPersistence persistence;
        private final PersistableStore<TimestampStore> store;

        private TestClient(ManualPersistence persistence) {
            this(persistence, new TimestampStore());
        }

        private TestClient(ManualPersistence persistence, PersistableStore<TimestampStore> store) {
            this.persistence = persistence;
            this.store = store;
        }

        @Override
        public Persistence<TimestampStore> getPersistence() {
            return persistence;
        }

        @Override
        public PersistableStore<TimestampStore> getPersistableStore() {
            return store;
        }
    }
}
