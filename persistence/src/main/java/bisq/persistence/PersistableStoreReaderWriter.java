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

import bisq.common.util.FileUtils;
import com.google.protobuf.Any;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class PersistableStoreReaderWriter<T extends PersistableStore<T>> {

    private final PersistableStoreFileManager storeFileManager;
    private final Path storeFilePath;
    private final Path parentDirectoryPath;

    public PersistableStoreReaderWriter(PersistableStoreFileManager storeFileManager) {
        this.storeFileManager = storeFileManager;
        this.storeFilePath = storeFileManager.getStoreFilePath();
        this.parentDirectoryPath = storeFilePath.getParent();
    }

    public synchronized Optional<T> read() {
        File storeFile = storeFilePath.toFile();
        if (!storeFile.exists()) {
            return Optional.empty();
        }

        try {
            PersistableStore<?> persistableStore = readStoreFromFile();
            //noinspection unchecked,rawtypes
            return (Optional) Optional.of(persistableStore);

        } catch (Exception e) {
            log.error("Couldn't read " + storeFilePath + " from file.", e);
            tryToBackupCorruptedStoreFile();
        }

        return Optional.empty();
    }

    public synchronized void write(T persistableStore) {
        storeFileManager.createParentDirectoriesIfNotExisting();

        try {
            writeStoreToTempFile(persistableStore);
            storeFileManager.tryToBackupCurrentStoreFile();
            storeFileManager.renameTempFileToCurrentFile();

        } catch (CouldNotSerializePersistableStore e) {
            log.error("Couldn't serialize " + persistableStore, e);

        } catch (Exception e) {
            log.error("Couldn't write persistable store to disk. Trying restore backup.", e);
            storeFileManager.restoreBackupFileIfCurrentFileNotExisting();
        }
    }

    private PersistableStore<?> readStoreFromFile() throws IOException {
        File storeFile = storeFilePath.toFile();
        try (FileInputStream fileInputStream = new FileInputStream(storeFile)) {
            Any any = Any.parseDelimitedFrom(fileInputStream);
            return PersistableStore.fromAny(any);
        }
    }

    private void tryToBackupCorruptedStoreFile() {
        try {
            FileUtils.backupCorruptedFile(
                    parentDirectoryPath.toAbsolutePath().toString(),
                    storeFilePath.toFile(),
                    storeFilePath.getFileName().toString(),
                    "corruptedFilesAtRead"
            );
        } catch (IOException e) {
            log.error("Error trying to backup corrupted file " + storeFilePath + ": " + e.getMessage(), e);
        }
    }

    private void writeStoreToTempFile(T persistableStore) {
        File tempFile = storeFileManager.getTempFilePath().toFile();
        writeStoreToFile(persistableStore, tempFile);
    }

    private void writeStoreToFile(T persistableStore, File file) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
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
