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
import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the write-id guard in {@link Persistence} (#4885 review): a snapshot written directly on the
 * caller's thread (the shutdown-hook path) must be durable the moment the call returns, and a STALE
 * write still queued on the shared persistence executor must not overwrite it when the queue later
 * drains. Without the guard the stale write wins and the disk silently ends on old state while the
 * client's generation bookkeeping reports clean.
 */
class PersistenceWriteGuardTests {

    @Test
    void directWriteIsDurableOnReturnAndCannotBeOverwrittenByAStaleQueuedWrite(@TempDir Path tempDirPath) throws Exception {
        registerTimestampStoreResolver();

        // Store A occupies the SHARED single-thread persistence executor: its serialization gates until
        // released - the real-world shutdown congestion case (one slow store starving all queued writes).
        var executorGate = new CountDownLatch(1);
        var serializationEntered = new CountDownLatch(1);
        var gatedStore = new GatedSerializationStore(executorGate, serializationEntered);
        var persistenceA = new Persistence<GatedSerializationStore>(
                tempDirPath, "GatedStore", MaxBackupSize.ZERO, new RestoreService());
        CompletableFuture<Void> blockedWrite = persistenceA.persistAsync(gatedStore);
        // try/finally around everything that runs while the gate is closed: a failing assertion must not
        // leave the SHARED single-thread executor parked, or every later test writing through it cascades
        // into unrelated timeouts.
        try {
            assertThat(serializationEntered.await(2, TimeUnit.SECONDS))
                    .as("the gated write must be running on the executor thread")
                    .isTrue();

            // Store B: an older snapshot (v1) gets queued behind the blocked write, then the newest snapshot
            // (v2) is written DIRECTLY on this thread - as the shutdown hook does after its bounded wait
            // timed out.
            var persistenceB = new Persistence<TimestampStore>(
                    tempDirPath, "TimestampStore", MaxBackupSize.ZERO, new RestoreService());
            CompletableFuture<Void> staleQueuedWrite = persistenceB.persistAsync(storeWithVersion(1));
            persistenceB.persist(storeWithVersion(2));

            // Durability: the newest snapshot is on disk the moment the direct write returns - while the
            // stale write is still queued. On a real shutdown the JVM may die right here; v2 must survive.
            assertThat(readVersion(persistenceB))
                    .as("the direct write must be durable before it returns")
                    .isEqualTo(2L);

            // The queue drains after the hook returned (JVM stayed alive long enough): the stale queued
            // write must be skipped, not applied over the newer on-disk state.
            executorGate.countDown();
            blockedWrite.get(5, TimeUnit.SECONDS);
            staleQueuedWrite.get(5, TimeUnit.SECONDS);
            assertThat(readVersion(persistenceB))
                    .as("a stale queued write must never overwrite a newer on-disk snapshot")
                    .isEqualTo(2L);
        } finally {
            executorGate.countDown();
        }
    }

    @Test
    void ticketOrderDecidesWhichWriteWins(@TempDir Path tempDirPath) {
        registerTimestampStoreResolver();
        var persistence = new Persistence<TimestampStore>(
                tempDirPath, "TimestampStore", MaxBackupSize.ZERO, new RestoreService());

        // The Hole this pins (security review of #4885): a snapshot captured EARLIER must never win over
        // one captured later. Callers guarantee capture-order == ticket-order by reserving the ticket
        // under the same lock as the snapshot capture (RateLimitedPersistenceClient's scheduleLock, or
        // the synchronized default in PersistenceClient#persist); here we verify the guard end: a write
        // carrying an older reserved ticket is skipped once a newer-ticket write has landed...
        long olderTicket = persistence.reserveWriteId();
        persistence.persist(storeWithVersion(2)); // auto-ticket, newer than olderTicket
        persistence.persist(storeWithVersion(1), olderTicket);
        assertThat(readVersion(persistence))
                .as("a write with an older reserved ticket must be skipped, not applied over newer state")
                .isEqualTo(2L);

        // ...while a reserved ticket that is still the newest writes normally.
        long newestTicket = persistence.reserveWriteId();
        persistence.persist(storeWithVersion(3), newestTicket);
        assertThat(readVersion(persistence)).isEqualTo(3L);
    }

    @Test
    void queuedWriteStillRunsWhenNoNewerWriteLanded(@TempDir Path tempDirPath) throws Exception {
        registerTimestampStoreResolver();
        var persistence = new Persistence<TimestampStore>(
                tempDirPath, "TimestampStore", MaxBackupSize.ZERO, new RestoreService());

        persistence.persistAsync(storeWithVersion(1)).get(5, TimeUnit.SECONDS);
        assertThat(readVersion(persistence)).isEqualTo(1L);

        // And newer writes keep applying normally after older ones.
        persistence.persistAsync(storeWithVersion(2)).get(5, TimeUnit.SECONDS);
        assertThat(readVersion(persistence)).isEqualTo(2L);
    }

    private static final String VERSION_KEY = "version";

    private static TimestampStore storeWithVersion(long version) {
        TimestampStore store = new TimestampStore();
        store.getTimestampsByProfileId().put(VERSION_KEY, version);
        return store;
    }

    private static long readVersion(Persistence<TimestampStore> persistence) {
        TimestampStore read = persistence.read().orElseThrow();
        return read.getTimestampsByProfileId().get(VERSION_KEY);
    }

    private static void registerTimestampStoreResolver() {
        try {
            PersistableStoreResolver.addResolver(new TimestampStore().getResolver());
        } catch (Exception e) {
            // Already registered by another test in this JVM - fine.
        }
    }

    /**
     * Minimal store whose serialization ({@link #toAny()}, the entry point
     * {@code PersistableStoreReaderWriter#writeStoreToFilePath} uses) blocks until released - holding the
     * shared persistence executor's single thread exactly like a slow/wedged store write does.
     */
    private static final class GatedSerializationStore implements PersistableStore<GatedSerializationStore> {
        private final TimestampStore delegate = new TimestampStore();
        private final CountDownLatch gate;
        private final CountDownLatch entered;

        private GatedSerializationStore(CountDownLatch gate, CountDownLatch entered) {
            this.gate = gate;
            this.entered = entered;
        }

        @Override
        public Any toAny() {
            entered.countDown();
            try {
                if (!gate.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("gated serialization was never released");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
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
        public GatedSerializationStore getClone() {
            return this;
        }

        @Override
        public void applyPersisted(GatedSerializationStore persisted) {
            // not needed
        }

        @Override
        public ProtoResolver<PersistableStore<?>> getResolver() {
            return delegate.getResolver();
        }
    }
}
