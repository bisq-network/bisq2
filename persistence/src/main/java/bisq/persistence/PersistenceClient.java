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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PersistenceClient<T extends PersistableStore<T>> {
    default Optional<T> readPersisted() {
        return getPersistence().read()
                .map(persisted -> {
                    persisted = preProcessPersisted(persisted);
                    persisted = prunePersisted(persisted);
                    getPersistableStore().applyPersisted(persisted);
                    onPersistedApplied(persisted);
                    return persisted;
                });
    }

    // In case we want to apply changes to persisted data
    default T preProcessPersisted(T persisted) {
        return persisted;
    }

    default T prunePersisted(T persisted) {
        return persisted;
    }

    default void onPersistedApplied(T persisted) {
    }

    Persistence<T> getPersistence();

    PersistableStore<T> getPersistableStore();

    default CompletableFuture<Boolean> persist() {
        // Snapshot capture and write-ticket assignment (inside persistAsync) must be one atomic unit:
        // Persistence's write-id guard skips a write when a higher-ticket write already landed, which is
        // only correct if ticket order equals capture order. Without this lock, two concurrent persist()
        // calls (e.g. ProfileAgeService/SignedWitnessService/AccountAgeService are reachable from
        // scheduler and message-handler threads) could capture in one order and take tickets in the
        // other, silently dropping the newer snapshot. RateLimitedPersistenceClient overrides this with
        // its own scheduleLock-based equivalent.
        synchronized (this) {
            return getPersistence().persistAsync(getPersistableStore().getClone())
                    .handle((nil, throwable) -> throwable == null);
        }
    }
}
