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

import bisq.common.threading.ExecutorFactory;
import bisq.common.util.StringUtils;
import bisq.persistence.backup.BackupFileInfo;
import bisq.persistence.backup.MaxBackupSize;
import bisq.persistence.backup.RestoreService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class Persistence<T extends PersistableStore<T>> {
    public static final String EXTENSION = ".protobuf";
    private static final ExecutorService EXECUTOR = ExecutorFactory.newSingleThreadExecutor("Persistence");

    @Getter
    private final Path storePath;
    @Getter
    private final String fileName;

    private final PersistableStoreReaderWriter<T> persistableStoreReaderWriter;
    // Write-id guard: every write request takes a monotonic ticket; a write is SKIPPED when a
    // higher-ticket write of this store already reached disk. This makes writes monotonic regardless
    // of which thread performs them - specifically, a direct shutdown-hook write (see
    // RateLimitedPersistenceClient#persistOnShutdown) cannot be overwritten later by a STALE write
    // that was still queued on the shared executor when the hook ran.
    private final AtomicLong writeRequestId = new AtomicLong();
    private final AtomicLong lastWrittenId = new AtomicLong();

    public Persistence(Path directoryPath, String fileName, MaxBackupSize maxBackupSize, RestoreService restoreService) {
        this.fileName = fileName;
        String storageFileName = StringUtils.camelCaseToSnakeCase(fileName);
        Path resolved = directoryPath.resolve(storageFileName + EXTENSION).normalize();
        Path baseDir = directoryPath.normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid storage path");
        }
        storePath = resolved;
        var storeFileManager = new PersistableStoreFileManager(storePath, maxBackupSize);
        persistableStoreReaderWriter = new PersistableStoreReaderWriter<>(storeFileManager, restoreService);
    }

    public Optional<T> read() {
        return persistableStoreReaderWriter.read();
    }

    public CompletableFuture<Void> persistAsync(T serializable) {
        // Ticket taken at SUBMISSION time, so a direct persist() call issued later (shutdown hook)
        // always outranks every write already sitting in the executor queue.
        long writeId = writeRequestId.incrementAndGet();
        return CompletableFuture.runAsync(() -> writeIfNotSuperseded(serializable, writeId), EXECUTOR);
    }

    protected void persist(T persistableStore) {
        writeIfNotSuperseded(persistableStore, writeRequestId.incrementAndGet());
    }

    /** The actual disk write - the single overridable seam below the write-id guard. */
    protected void doWrite(T persistableStore) {
        persistableStoreReaderWriter.write(persistableStore);
    }

    /**
     * Reserves a write ticket WITHOUT writing. Callers that capture their snapshot under a lock (see
     * RateLimitedPersistenceClient's scheduleLock) must reserve the ticket under that SAME lock and pass
     * it to {@link #persist(PersistableStore, long)} - otherwise a concurrent submission can capture a
     * newer snapshot yet receive an OLDER ticket, and the guard would skip the newer write as stale.
     * Ticket order must equal snapshot-capture order for the guard to be correct.
     */
    long reserveWriteId() {
        return writeRequestId.incrementAndGet();
    }

    void persist(T persistableStore, long writeId) {
        writeIfNotSuperseded(persistableStore, writeId);
    }

    private void writeIfNotSuperseded(T persistableStore, long writeId) {
        // Same monitor as PersistableStoreReaderWriter#write (a synchronized method): the stale check,
        // the disk write and the watermark update must be one atomic unit. Without it a stale task
        // could pass the check, block on an in-flight newer write, and then overwrite it anyway.
        synchronized (persistableStoreReaderWriter) {
            if (lastWrittenId.get() > writeId) {
                log.info("Skipping write {} of {}: a newer snapshot (write {}) is already on disk.",
                        writeId, fileName, lastWrittenId.get());
                return;
            }
            doWrite(persistableStore);
            lastWrittenId.accumulateAndGet(writeId, Math::max);
        }
    }

    public CompletableFuture<Void> pruneBackups() {
        return CompletableFuture.runAsync(persistableStoreReaderWriter::pruneBackups, EXECUTOR);
    }

    public List<BackupFileInfo> getBackups() {
        return persistableStoreReaderWriter.getBackups();
    }
}
