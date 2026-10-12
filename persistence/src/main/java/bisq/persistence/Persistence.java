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
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.persistence.backup.BackupFileInfo;
import bisq.persistence.backup.MaxBackupSize;
import bisq.persistence.backup.RestoreService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class Persistence<T extends PersistableStore<T>> {
    public static final String EXTENSION = ".protobuf";
    private static final ExecutorService EXECUTOR = ExecutorFactory.newSingleThreadExecutor("Persistence");

    @Getter
    private final Path storePath;
    @Getter
    private final String fileName;

    private final PersistableStoreReaderWriter<T> persistableStoreReaderWriter;

    public Persistence(Path directoryPath, String fileName, MaxBackupSize maxBackupSize, RestoreService restoreService) {
        this(directoryPath, fileName, maxBackupSize, restoreService, UnreadableStoreFilePolicy.NO_ARCHIVE,
                Optional.empty());
    }

    public Persistence(Path directoryPath,
                       String fileName,
                       MaxBackupSize maxBackupSize,
                       RestoreService restoreService,
                       UnreadableStoreFilePolicy unreadableStoreFilePolicy,
                       Optional<UnreadableStoreArchive> unreadableStoreArchive) {
        this.fileName = fileName;
        String storageFileName = StringUtils.camelCaseToSnakeCase(fileName);
        Path resolved = directoryPath.resolve(storageFileName + EXTENSION).normalize();
        Path baseDir = directoryPath.normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid storage path");
        }
        storePath = resolved;
        var storeFileManager = new PersistableStoreFileManager(storePath, maxBackupSize);
        persistableStoreReaderWriter = new PersistableStoreReaderWriter<>(storeFileManager, restoreService,
                unreadableStoreFilePolicy, unreadableStoreArchive);
    }

    public Optional<T> read() {
        return persistableStoreReaderWriter.read();
    }

    public CompletableFuture<Void> persistAsync(T serializable) {
        return CompletableFuture.runAsync(() -> persist(serializable), EXECUTOR);
    }

    protected void persist(T persistableStore) {
        try {
            persistableStoreReaderWriter.write(persistableStore);
        } catch (RuntimeException e) {
            // The persistence clients turn a failed write into the result false and drop the cause, and most callers
            // ignore the result. So the failure is logged here, with the store path. A file system error names a path
            // and the error of the operating system. Another cause, for example a failed serialization, can carry
            // values of the store in its message, so only its type and stack trace are logged (see
            // docs/specifications/persistence/store-write-results.md).
            Throwable rootCause = ExceptionUtil.getRootCause(e);
            String cause = rootCause instanceof IOException ?
                    rootCause.toString() :
                    ExceptionUtil.getSafeStackTraceAsString(e);
            log.error("Persisting the store {} failed. Cause: {}", storePath, cause);
            throw e;
        }
    }

    public CompletableFuture<Void> pruneBackups() {
        return CompletableFuture.runAsync(persistableStoreReaderWriter::pruneBackups, EXECUTOR);
    }

    /** Runs on the persistence executor, so it is ordered against pending writes. */
    public CompletableFuture<Void> deleteBackups() {
        return CompletableFuture.runAsync(persistableStoreReaderWriter::deleteBackups, EXECUTOR);
    }

    public List<BackupFileInfo> getBackups() {
        return persistableStoreReaderWriter.getBackups();
    }
}
