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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

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
    private volatile long lastWrite;
    @Getter
    private volatile boolean dropped;
    private volatile boolean writeInProgress;

    public RateLimitedPersistenceClient() {
        //todo (Critical) check if we want to use ShutdownHook here
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("RateLimitedPersistenceClient.shutdownHook-" + StringUtils.truncate(getPersistence().getStorePath(), 8));
            persistOnShutdown();
        }));
    }

    @Override
    public CompletableFuture<Boolean> persist() {
        boolean tooFast = System.currentTimeMillis() - lastWrite < getMaxWriteRateInMs();
        dropped = tooFast || writeInProgress;
        if (dropped) {
            return CompletableFuture.completedFuture(false);
        } else {
            lastWrite = System.currentTimeMillis();
            writeInProgress = true;
            dropped = false;
            return getPersistence()
                    .persistAsync(getPersistableStore().getClone())
                    .handle((nil, throwable) -> {
                        if (throwable != null) {
                            // The write failed (e.g. SnapshotLockTimeoutException while serializing - see
                            // PersistableStoreReaderWriter#write): the on-disk snapshot is stale, so keep the
                            // retry state. The next persist() call writes the then-current store, and
                            // persistOnShutdown() covers the case where no further persist() ever comes.
                            dropped = true;
                        }
                        writeInProgress = false;
                        return throwable == null;
                    });
        }
    }

    /**
     * Unthrottled write path: bypasses the max-write-rate gate and any in-progress write, for the rare writes
     * that must not be silently dropped - e.g. a trade protocol reaching a final state, whose in-memory wipe of
     * sensitive queued messages has to reach disk promptly (see {@code bisq.common.fsm.Fsm#persistOnFinalState}).
     * Safe to submit while a throttled write is in flight: the single Persistence executor thread serializes
     * writes, and this call clones the store at submission time, so it captures state at least as new as any
     * write already queued ahead of it.
     */
    public CompletableFuture<Boolean> persistNow() {
        lastWrite = System.currentTimeMillis();
        dropped = false;
        return getPersistence()
                .persistAsync(getPersistableStore().getClone())
                .handle((nil, throwable) -> {
                    if (throwable != null) {
                        // Same contract as the throttled path above: a failed write must leave a retry pending.
                        dropped = true;
                    }
                    return throwable == null;
                });
    }

    protected long getMaxWriteRateInMs() {
        return 1000;
    }

    private void persistOnShutdown() {
        if (dropped) {
            dropped = false;
            try {
                getPersistence().persist(getPersistableStore().getClone());
            } catch (Exception e) {
                // Runs on a JVM shutdown-hook thread: an escaping exception (e.g. SnapshotLockTimeoutException,
                // which the write path deliberately rethrows) would die unlogged there, and nothing can retry
                // after this point anyway. The previous store file on disk remains intact.
                log.error("Persisting {} at shutdown failed.", getPersistence().getStorePath(), e);
            }
        }
    }
}