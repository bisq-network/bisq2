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

import bisq.persistence.backup.BackupService;
import bisq.persistence.backup.MaxBackupSize;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
public class PersistableStoreFileManager {
    public static final String TEMP_FILE_PREFIX = "temp_";

    @Getter
    private final Path storeFilePath;
    private final Path parentDirectoryPath;
    private final BackupService backupService;

    public PersistableStoreFileManager(Path storeFilePath) {
        this(storeFilePath, MaxBackupSize.ZERO);
    }

    public PersistableStoreFileManager(Path storeFilePath, MaxBackupSize maxBackupSize) {
        this.storeFilePath = storeFilePath;
        this.parentDirectoryPath = storeFilePath.getParent();
        Path dataDir = storeFilePath.getParent().getParent().getParent();
        backupService = new BackupService(dataDir, storeFilePath, maxBackupSize);
    }

    public void createParentDirectoriesIfNotExisting() {
        File parentDir = parentDirectoryPath.toFile();
        if (!parentDir.exists()) {
            boolean isSuccess = parentDir.mkdirs();
            if (!isSuccess) {
                throw new CouldNotCreateParentDirs("Couldn't create " + parentDir);
            }
        }
    }

    public void maybeMigrateLegacyBackupFile() {
        backupService.maybeMigrateLegacyBackupFile();
    }

    public void renameTempFileToCurrentFile(Path tempFilePath) throws IOException {
        File tempFile = tempFilePath.toFile();
        if (!tempFile.exists()) {
            // Log directory contents to help diagnose the issue
            File parentDir = tempFile.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                File[] files = parentDir.listFiles();
                String fileList = files != null ? String.join(", ",
                    java.util.Arrays.stream(files).map(File::getName).toArray(String[]::new)) : "null";
                log.error("Temp file does not exist: {}. Parent directory contents: [{}]",
                         tempFile.getAbsolutePath(), fileList);
            }
            throw new NoSuchFileException(tempFile.getAbsolutePath() + " does not exist. Cannot rename not existing file.");
        }

        File storeFile = storeFilePath.toFile();
        // Delete existing store file if it exists (for updates)
        if (storeFile.exists()) {
            Files.delete(storeFilePath);
        }

        // Use Files.move for atomic, platform-independent rename operation
        // ATOMIC_MOVE ensures the operation is atomic on platforms that support it
        try {
            log.debug("Renaming temp file {} to {}", tempFilePath, storeFilePath);
            Files.move(tempFilePath, storeFilePath, StandardCopyOption.ATOMIC_MOVE);
            log.debug("Successfully renamed temp file to {}", storeFilePath);
        } catch (IOException e) {
            throw new IOException("Couldn't rename " + tempFile + " to " + storeFilePath, e);
        }
    }

    public boolean maybeBackup() {
        return backupService.maybeBackup();
    }

    public void pruneBackups() {
        backupService.prune();
    }

    /**
     * Creates a unique temp file path for each write operation to avoid race conditions
     * when multiple threads try to persist concurrently (e.g., main thread + shutdown hook).
     */
    public Path createTempFilePath() throws IOException {
        String prefix = TEMP_FILE_PREFIX + storeFilePath.getFileName().toString().replace(".protobuf", "_");
        return Files.createTempFile(parentDirectoryPath, prefix, ".protobuf");
    }
}
