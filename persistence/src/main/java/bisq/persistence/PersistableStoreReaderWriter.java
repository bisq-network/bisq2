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

import bisq.common.file.FileMutatorUtils;
import bisq.persistence.backup.BackupFileInfo;
import bisq.persistence.backup.RestoreService;
import com.google.protobuf.Any;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class PersistableStoreReaderWriter<T extends PersistableStore<T>> {

    private final PersistableStoreFileManager storeFileManager;
    private final RestoreService restoreService;
    private final Path storeFilePath;
    private final Path parentDirectoryPath;

    public PersistableStoreReaderWriter(PersistableStoreFileManager storeFileManager, RestoreService restoreService) {
        this.storeFileManager = storeFileManager;
        this.restoreService = restoreService;
        this.storeFilePath = storeFileManager.getStoreFilePath();
        this.parentDirectoryPath = storeFilePath.getParent();
    }

    public synchronized Optional<T> read() {
        if (!Files.exists(storeFilePath)) {
            return Optional.empty();
        }

        // In case we do not have any backup file yet, we check if we have a legacy backup file (pre-v2.1.2) and move
        // that to the new backup structure. As we only do the backup at write we would otherwise not have data which
        // have been written only once like the user identity.
        storeFileManager.maybeMigrateLegacyBackupFile();

        return readStoreFromFileOrRestoreFromBackup();
    }

    public synchronized void write(T persistableStore) {
        storeFileManager.createParentDirectoriesIfNotExisting();
        try {
            writeStoreToTempFilePath(persistableStore);
            boolean hasFileBeenBackedUp = storeFileManager.maybeBackup();
            if (!hasFileBeenBackedUp) {
                Files.deleteIfExists(storeFilePath);
            }
            storeFileManager.renameTempFileToCurrentFile();
        } catch (CouldNotSerializePersistableStore e) {
            log.error("Couldn't serialize {}", persistableStore, e);
        } catch (Exception e) {
            log.error("Couldn't write persistable store to disk.", e);
        }
    }

    public void pruneBackups() {
        storeFileManager.pruneBackups();
    }

    public List<BackupFileInfo> getBackups() {
        return storeFileManager.getBackups();
    }

    private Optional<T> readStoreFromFileOrRestoreFromBackup() {
        Optional<T> optionalStore = readStore(storeFilePath);
        if (optionalStore.isPresent()) {
            return optionalStore;
        } else {
            return restoreService.tryToRestoreFromBackup(storeFileManager.getBackups(), this::readStore);
        }
    }

    private Optional<T> readStore(Path path) {
        try {
            PersistableStore<?> persistableStore = readStoreFromFile(path);

            // copy to main store file path if read from backup, because in case of a corrupted main file the main folder would not contain any file at all
            if (!path.equals(storeFilePath)) {
                log.info("Copy content to storeFilePath");
                FileMutatorUtils.copyFile(path, storeFilePath);
            }

            //noinspection unchecked,rawtypes
            return (Optional) Optional.of(persistableStore);

        } catch (Exception e) {
            log.error("Couldn't read {} from file.", path, e);
            tryToBackupCorruptedStoreFile(path);
        }
        return Optional.empty();
    }

    private PersistableStore<?> readStoreFromFile(Path path) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(path)) {
            Any any = Any.parseDelimitedFrom(fileInputStream);
            checkNotNull(any, "Any.parseDelimitedFrom for " + path + " resulted in a null value.");
            return PersistableStore.fromAny(any);
        }
    }

    private void tryToBackupCorruptedStoreFile(Path pathToBackup) {
        try {
            FileMutatorUtils.backupCorruptedFile(
                    parentDirectoryPath,
                    pathToBackup,
                    pathToBackup.getFileName().toString(),
                    "corruptedFilesAtRead"
            );
        } catch (IOException e) {
            log.error("Error trying to backup corrupted file {}: {}", pathToBackup, e.getMessage(), e);
        }
    }

    private void writeStoreToTempFilePath(T persistableStore) {
        Path tempFilePath = storeFileManager.getTempFilePath();
        writeStoreToFilePath(persistableStore, tempFilePath);
    }

    private void writeStoreToFilePath(T persistableStore, Path filePath) {
        try (OutputStream fileOutputStream = FileMutatorUtils.newRestrictedOutputStream(filePath)) {
            // We use an Any container (byte blob) as we do not have the dependencies to the
            // external PersistableStore implementations (at deserialization we would have an issue otherwise as
            // it requires static access).
            Any any = persistableStore.toAny();
            any.writeDelimitedTo(fileOutputStream);
        } catch (IOException e) {
            throw new CouldNotSerializePersistableStore(e);
        }
    }
}
