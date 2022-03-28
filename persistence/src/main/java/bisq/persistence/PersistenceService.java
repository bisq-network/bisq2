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

import bisq.common.proto.Proto;
import bisq.common.util.CompletableFutureUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class PersistenceService {
    @Getter
    private final String baseDir;
    @Getter
    protected final List<PersistenceClient<? extends Proto>> clients = new CopyOnWriteArrayList<>();
    protected final List<Persistence<? extends Proto>> persistenceInstances = new CopyOnWriteArrayList<>();

    public PersistenceService(String baseDir) {
        this.baseDir = baseDir;
    }

    public <T extends PersistableStore<T>> Persistence<T> getOrCreatePersistence(PersistenceClient<T> client,
                                                                                 PersistableStore<T> persistableStore) {
        return getOrCreatePersistence(client, "db", persistableStore.getClass().getSimpleName(), persistableStore);
    }

    public <T extends PersistableStore<T>> Persistence<T> getOrCreatePersistence(PersistenceClient<T> client,
                                                                                 String subDir,
                                                                                 PersistableStore<T> persistableStore) {
        return getOrCreatePersistence(client, subDir, persistableStore.getClass().getSimpleName(), persistableStore);
    }

    public <T extends PersistableStore<T>> Persistence<T> getOrCreatePersistence(PersistenceClient<T> client,
                                                                                 String subDir,
                                                                                 String fileName,
                                                                                 PersistableStore<T> persistableStore) {

       PersistableStoreResolver.addResolver(persistableStore.getResolver()); 
        clients.add(client);
        Persistence<T> persistence = new Persistence<>(baseDir + File.separator + subDir, fileName);
        persistenceInstances.add(persistence);
        return persistence;
    }

    public CompletableFuture<Boolean> readAllPersisted() {
        return CompletableFutureUtils.allOf(clients.stream()
                        .map(persistenceClient -> persistenceClient.readPersisted()
                                .whenComplete((optionalResult, throwable) -> {
                                    String storagePath = persistenceClient.getPersistence().getStoragePath();
                                    if (throwable == null) {
                                        if (optionalResult.isPresent()) {
                                            log.info("Read persisted data from {}", storagePath);
                                        } else {
                                            log.debug("No persisted data at {} found", storagePath);
                                        }
                                    } else {
                                        log.error("Error at read persisted data from: {}", storagePath);
                                        throwable.printStackTrace();
                                    }
                                })))
                .thenApply(list -> true);
    }
}