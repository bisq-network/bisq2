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
import bisq.persistence.backup.MaxBackupSize;
import bisq.persistence.backup.RestoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class RateLimitedPersistenceClientTests {

    @Test
    void failedWriteKeepsRetryPending(@TempDir Path tempDirPath) {
        var persistence = new Persistence<TimestampStore>(tempDirPath, "TimestampStore", MaxBackupSize.TEN_MB, new RestoreService()) {
            @Override
            public CompletableFuture<Void> persistAsync(TimestampStore serializable) {
                return CompletableFuture.failedFuture(
                        new SnapshotLockTimeoutException(new FsmModel(State.FsmState.ERROR), 500));
            }
        };
        var client = createClient(persistence);

        // A failed write must not be reported as a success, and must leave the dropped flag set so that
        // persistOnShutdown() retries it if no further persist() call ever comes.
        assertThat(client.persist().join()).isFalse();
        assertThat(client.isDropped()).isTrue();
    }

    @Test
    void persistNowBypassesRateLimit(@TempDir Path tempDirPath) {
        var persistence = new Persistence<TimestampStore>(tempDirPath, "TimestampStore", MaxBackupSize.TEN_MB, new RestoreService());
        var client = createClient(persistence);

        assertThat(client.persist().join()).isTrue();

        // Within the max-write-rate window the throttled path drops the write...
        assertThat(client.persist().join()).isFalse();
        assertThat(client.isDropped()).isTrue();

        // ...but the unthrottled path must write anyway and clear the pending retry it covers.
        assertThat(client.persistNow().join()).isTrue();
        assertThat(client.isDropped()).isFalse();
    }

    @Test
    void failedPersistNowKeepsRetryPending(@TempDir Path tempDirPath) {
        var persistence = new Persistence<TimestampStore>(tempDirPath, "TimestampStore", MaxBackupSize.TEN_MB, new RestoreService()) {
            @Override
            public CompletableFuture<Void> persistAsync(TimestampStore serializable) {
                return CompletableFuture.failedFuture(
                        new SnapshotLockTimeoutException(new FsmModel(State.FsmState.ERROR), 500));
            }
        };
        var client = createClient(persistence);

        assertThat(client.persistNow().join()).isFalse();
        assertThat(client.isDropped()).isTrue();
    }

    @Test
    void successfulWriteLeavesNoRetryPending(@TempDir Path tempDirPath) {
        var persistence = new Persistence<TimestampStore>(tempDirPath, "TimestampStore", MaxBackupSize.TEN_MB, new RestoreService());
        var client = createClient(persistence);

        assertThat(client.persist().join()).isTrue();
        assertThat(client.isDropped()).isFalse();
    }

    private static RateLimitedPersistenceClient<TimestampStore> createClient(Persistence<TimestampStore> persistence) {
        var store = new TimestampStore();
        return new RateLimitedPersistenceClient<>() {
            @Override
            public Persistence<TimestampStore> getPersistence() {
                return persistence;
            }

            @Override
            public PersistableStore<TimestampStore> getPersistableStore() {
                return store;
            }

            @Override
            protected long getMaxWriteRateInMs() {
                // Wide window so back-to-back persist() calls are reliably rate-limited even under a slow/paused
                // test JVM; the default 1000ms could theoretically elapse between the two calls.
                return 60_000;
            }
        };
    }
}
