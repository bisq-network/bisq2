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

import bisq.common.proto.ProtoResolver;
import bisq.persistence.backup.MaxBackupSize;
import bisq.persistence.backup.RestoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
