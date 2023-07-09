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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@Slf4j
public class PersistableStoreFileManager {

    public static final String BACKUP_DIR = "backup";
    public static final String TEMP_FILE_PREFIX = "temp_";

    @Getter
    private final Path storeFilePath;
    private final Path parentDirectoryPath;

    private final Path backupFilePath;
    @Getter
    private final Path tempFilePath;

    public PersistableStoreFileManager(Path storeFilePath) {
        this.storeFilePath = storeFilePath;
        this.parentDirectoryPath = storeFilePath.getParent();
        this.backupFilePath = createBackupFilePath();
        this.tempFilePath = createTempFilePath();
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

    public void tryToBackupCurrentStoreFile() throws IOException {
        File storeFile = storeFilePath.toFile();
        if (!storeFile.exists()) {
            return;
        }

        File backupFile = backupFilePath.toFile();
        if (backupFile.exists()) {
            Files.delete(backupFilePath);
        }

        boolean isSuccess = storeFilePath.toFile().renameTo(backupFile);
        if (!isSuccess) {
            throw new IOException("Couldn't rename " + storeFilePath + " to " + backupFilePath);
        }
    }

    public void restoreBackupFileIfCurrentFileNotExisting() {
        File storeFile = storeFilePath.toFile();
        if (!storeFile.exists()) {
            File backupFile = backupFilePath.toFile();
            boolean isSuccess = backupFile.renameTo(storeFile);

            if (!isSuccess) {
                log.error("Couldn't rename " + backupFile + " to " + storeFilePath);
            }
        }
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

    private Path createBackupFilePath() {
        Path dirPath = Path.of(parentDirectoryPath.toString(), BACKUP_DIR);
        dirPath.toFile().mkdirs();
        return dirPath.resolve(storeFilePath.getFileName());
    }

    private Path createTempFilePath() {
        String tempFileName = TEMP_FILE_PREFIX + storeFilePath.getFileName();
        return parentDirectoryPath.resolve(tempFileName);
    }
}
