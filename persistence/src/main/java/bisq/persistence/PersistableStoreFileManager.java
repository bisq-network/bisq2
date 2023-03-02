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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class PersistableStoreFileManager {

    public static final String BACKUP_FILE_PREFIX = "backup_";
    public static final String TEMP_FILE_PREFIX = "temp_";

    private final Path storeFilePath;
    private final Path parentDirectoryPath;

    public PersistableStoreFileManager(Path storeFilePath) {
        this.storeFilePath = storeFilePath;
        this.parentDirectoryPath = storeFilePath.getParent();
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

    public void backupCurrentStoreFile() throws IOException {
        String backupFileName = BACKUP_FILE_PREFIX + storeFilePath.getFileName();
        Path backupFilePath = parentDirectoryPath.resolve(backupFileName);
        File backupFile = backupFilePath.toFile();

        if (backupFile.exists()) {
            Files.delete(backupFilePath);
        }

        boolean isSuccess = storeFilePath.toFile().renameTo(backupFile);
        if (!isSuccess) {
            throw new IOException("Couldn't rename " + storeFilePath + " to " + backupFilePath);
        }
    }

    public void renameTempFileToCurrentFile() throws IOException {
        File storeFile = storeFilePath.toFile();
        if (storeFile.exists()) {
            throw new PersistableStoreFileBackupFailed();
        }

        String tempFileName = TEMP_FILE_PREFIX + storeFilePath.getFileName();
        File tempFile = parentDirectoryPath.resolve(tempFileName).toFile();

        if (!tempFile.exists()) {
            throw new NoSuchFileException(tempFile.getAbsolutePath() + " does not exist. Cannot rename not existing file.");
        }

        boolean isSuccess = tempFile.renameTo(storeFile);
        if (!isSuccess) {
            throw new IOException("Couldn't rename " + tempFile + " to " + storeFilePath);
        }
    }
}
