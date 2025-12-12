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

import bisq.common.proto.PersistableProto;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.persistence.backup.BackupFileInfo;
import bisq.persistence.backup.MaxBackupSize;
import bisq.persistence.backup.RestoreService;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class PersistenceService {
    private static final ExecutorService EXECUTOR = ExecutorFactory.newSingleThreadExecutor("PersistenceService");

    @Getter
    private final Path appDataDirPath;
    @Getter
    protected final List<PersistenceClient<? extends PersistableProto>> clients = new CopyOnWriteArrayList<>();
    protected final List<Persistence<? extends PersistableProto>> persistenceInstances = new CopyOnWriteArrayList<>();
    @Getter
    private final RestoreService restoreService = new RestoreService();

    public PersistenceService(Path appDataDirPath) {
        this.appDataDirPath = appDataDirPath;
    }

    public <T extends PersistableStore<T>> Persistence<T> getOrCreatePersistence(PersistenceClient<T> client,
                                                                                 DbSubDirectory dbSubDirectory,
                                                                                 PersistableStore<T> persistableStore) {
        return getOrCreatePersistence(client,
                dbSubDirectory,
                persistableStore.getClass().getSimpleName(),
                persistableStore);
    }

    public <T extends PersistableStore<T>> Persistence<T> getOrCreatePersistence(PersistenceClient<T> client,
                                                                                 DbSubDirectory dbSubDirectory,
                                                                                 String fileName,
                                                                                 PersistableStore<T> persistableStore) {
        return getOrCreatePersistence(client,
                dbSubDirectory,
                fileName,
                persistableStore,
                MaxBackupSize.from(dbSubDirectory));
    }

    public <T extends PersistableStore<T>> Persistence<T> getOrCreatePersistence(PersistenceClient<T> client,
                                                                                 DbSubDirectory dbSubDirectory,
                                                                                 PersistableStore<T> persistableStore,
                                                                                 MaxBackupSize maxBackupSize) {
        return getOrCreatePersistence(client,
                dbSubDirectory.getDbPath(),
                persistableStore.getClass().getSimpleName(),
                persistableStore,
                maxBackupSize);
    }

    public <T extends PersistableStore<T>> Persistence<T> getOrCreatePersistence(PersistenceClient<T> client,
                                                                                 DbSubDirectory dbSubDirectory,
                                                                                 String fileName,
                                                                                 PersistableStore<T> persistableStore,
                                                                                 MaxBackupSize maxBackupSize) {
        return getOrCreatePersistence(client,
                dbSubDirectory.getDbPath(),
                fileName,
                persistableStore,
                maxBackupSize);
    }

    public <T extends PersistableStore<T>> Persistence<T> getOrCreatePersistence(PersistenceClient<T> client,
                                                                                 Path subDirPath,
                                                                                 String fileName,
                                                                                 PersistableStore<T> persistableStore,
                                                                                 MaxBackupSize maxBackupSize) {
        PersistableStoreResolver.addResolver(persistableStore.getResolver());
        clients.add(client);
        Path normalizedPath = subDirPath.normalize();
        if (normalizedPath.isAbsolute()) {
            throw new IllegalArgumentException("subDir must be relative to appDataDirPath");
        }
        Persistence<T> persistence = new Persistence<>(appDataDirPath.resolve(normalizedPath), fileName, maxBackupSize, restoreService);
        persistenceInstances.add(persistence);
        return persistence;
    }

    public CompletableFuture<Void> pruneAllBackups() {
        List<CompletableFuture<Void>> list = clients.stream()
                .map(PersistenceClient::getPersistence)
                .map(Persistence::pruneBackups)
                .collect(Collectors.toList());
        return CompletableFutureUtils.allOf(list).thenApply(l -> null);
    }

    public List<BackupFileInfo> getAllBackups() {
        return clients.stream()
                .map(PersistenceClient::getPersistence)
                .flatMap(p -> p.getBackups().stream())
                .toList();
    }

    public CompletableFuture<Boolean> readAllPersisted() {
        if (log.isDebugEnabled()) {
            List<String> storagePaths = clients.stream()
                    .map(persistenceClient -> persistenceClient.getPersistence().getStorePath()
                            .toAbsolutePath().toString())
                    .sorted()
                    .collect(Collectors.toList());
            log.debug("Read persisted data from:\n{}", Joiner.on("\n").join(storagePaths));
        }
        return CompletableFuture.supplyAsync(() -> {
            // We read sequentially as we need to ensure that low level data is present before higher level data
            // potentially access it.
            long ts = System.currentTimeMillis();
            AtomicBoolean result = new AtomicBoolean(true);
            clients.forEach(client -> {
                String storagePath = client.getPersistence().getStorePath().toAbsolutePath().toString();
                try {
                    Optional<? extends PersistableProto> optionalResult = client.readPersisted();
                    if (optionalResult.isPresent()) {
                        log.debug("Read persisted data from {}", storagePath);
                    } else {
                        log.debug("No persisted data at {} found", storagePath);
                    }
                } catch (Exception e) {
                    log.error("Error at read persisted data from: {}", storagePath, e);
                    result.set(false);
                }
            });
            log.info("Reading all persisted data took {} ms", System.currentTimeMillis() - ts);
            return result.get();
        }, EXECUTOR);
    }

    public CompletableFuture<Boolean> persistAllClients() {
        return CompletableFutureUtils.allOf(clients.stream()
                        .map(persistenceClient -> persistenceClient.persist()
                                .whenComplete((result, throwable) -> {
                                    if (throwable != null) {
                                        log.error("persistAllClients failed", throwable);
                                    } else if (!result) {
                                        log.warn("Failed to persist");
                                    }
                                })))
                .thenApply(list -> true);
    }
}