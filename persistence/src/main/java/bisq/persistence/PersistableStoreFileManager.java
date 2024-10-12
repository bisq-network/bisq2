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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@Slf4j
public class PersistableStoreFileManager {
    public static final String TEMP_FILE_PREFIX = "temp_";

    @Getter
    private final Path storeFilePath;
    private final Path parentDirectoryPath;
    private final BackupService backupService;
    @Getter
    private final Path tempFilePath;

    public PersistableStoreFileManager(Path storeFilePath) {
        this(storeFilePath, MaxBackupSize.ZERO);
    }

    public PersistableStoreFileManager(Path storeFilePath, MaxBackupSize maxBackupSize) {
        this.storeFilePath = storeFilePath;
        this.parentDirectoryPath = storeFilePath.getParent();
        this.tempFilePath = createTempFilePath();
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

    public void renameTempFileToCurrentFile() throws IOException {
        File storeFile = storeFilePath.toFile();
        if (storeFile.exists()) {
            throw new IOException(storeFilePath + " does already exist.");
        }

        File tempFile = tempFilePath.toFile();
        if (!tempFile.exists()) {
            throw new NoSuchFileException(tempFile.getAbsolutePath() + " does not exist. Cannot rename not existing file.");
        }

        boolean isSuccess = tempFile.renameTo(storeFile);
        if (!isSuccess) {
            throw new IOException("Couldn't rename " + tempFile + " to " + storeFilePath);
        }
    }

    public boolean maybeBackup() {
        return backupService.maybeBackup();
    }

    public void pruneBackups() {
        backupService.prune();
    }

    private Path createTempFilePath() {
        String tempFileName = TEMP_FILE_PREFIX + storeFilePath.getFileName();
        return parentDirectoryPath.resolve(tempFileName);
    }
}
