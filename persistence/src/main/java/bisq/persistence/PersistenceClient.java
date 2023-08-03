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

/**
 * Interface for the outside envelope object persisted to disk.
 */
public interface PersistenceClient<T extends PersistableStore<T>> {
    default CompletableFuture<Optional<T>> readPersisted() {
        return getPersistence().readAsync(persisted -> {
            persisted = prunePersisted(persisted);
            getPersistableStore().applyPersisted(persisted);
            onPersistedApplied(persisted);
        });
    }

    default T prunePersisted(T persisted) {
        return persisted;
    }

    default void onPersistedApplied(T persisted) {
    }

    Persistence<T> getPersistence();

    PersistableStore<T> getPersistableStore();

    default CompletableFuture<Boolean> persist() {
        return getPersistence().persistAsync(getPersistableStore().getClone())
                .handle((nil, throwable) -> throwable == null);
    }
}
