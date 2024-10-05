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

import bisq.common.threading.ThreadName;
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
            ThreadName.set(this, "shutdownHook-" + getPersistence().getStorePath());
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
                        writeInProgress = false;
                        return throwable == null;
                    });
        }
    }

    protected long getMaxWriteRateInMs() {
        return 1000;
    }

    private void persistOnShutdown() {
        if (dropped) {
            dropped = false;
            getPersistence().persist(getPersistableStore().getClone());
        }
    }
}