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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PersistableStoreFileManagerTests {
    private static final String BACKUP_DIR = "backup" + File.separator;

    @Test
    void createParentDirIfExisting(@TempDir Path tempDir) {
        Path storePath = tempDir.resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);

        storeFileManager.createParentDirectoriesIfNotExisting();
        assertThat(tempDir).exists();
    }

    @Test
    void createParentDirIfNotExisting(@TempDir Path tempDir) {
        Path storePath = tempDir.resolve("parent_dir").resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);

        storeFileManager.createParentDirectoriesIfNotExisting();
        assertThat(storePath.getParent()).exists();
    }

    @Test
    void backupCurrentStoreIfBackupNotExisting(@TempDir Path tempDir) throws IOException {
        Path storePath = tempDir.resolve("store");
        createEmptyFile(storePath);

        var storeFileManager = new PersistableStoreFileManager(storePath);
        storeFileManager.tryToBackupCurrentStoreFile();

        Path backupFilePath = tempDir.resolve(BACKUP_DIR + "store");
        assertThat(backupFilePath).exists();
    }

    @Test
    void backupCurrentStoreIfBackupExists(@TempDir Path tempDir) throws IOException {
        Path storePath = tempDir.resolve("store");
        createEmptyFile(storePath);
        var storeFileManager = new PersistableStoreFileManager(storePath);

        Path backupFilePath = tempDir.resolve(BACKUP_DIR + "store");
        boolean isSuccess = backupFilePath.toFile().createNewFile();
        assertThat(isSuccess).isTrue();

        storeFileManager.tryToBackupCurrentStoreFile();
        assertThat(backupFilePath).exists();
    }

    @Test
    void backupNotExistingStore(@TempDir Path tempDir) throws IOException {
        Path storePath = tempDir.resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);

        Path backupFilePath = tempDir.resolve(BACKUP_DIR + "store");
        createEmptyFile(backupFilePath);

        storeFileManager.tryToBackupCurrentStoreFile();
        assertThat(backupFilePath).exists();
        assertThat(storePath).doesNotExist();
    }

    @Test
    void restoreBackupIfCurrentFileNotExisting(@TempDir Path tempDir) throws IOException {
        Path storePath = tempDir.resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);

        Path backupFilePath = tempDir.resolve(BACKUP_DIR + "store");
        boolean isSuccess = backupFilePath.toFile().createNewFile();
        assertThat(isSuccess).isTrue();

        storeFileManager.restoreBackupFileIfCurrentFileNotExisting();
        assertThat(storePath).exists();
        assertThat(backupFilePath).doesNotExist();
    }

    @Test
    void restoreBackupIfCurrentFileExists(@TempDir Path tempDir) throws IOException {
        Path storePath = tempDir.resolve("store");
        createEmptyFile(storePath);
        var storeFileManager = new PersistableStoreFileManager(storePath);

        Path backupFilePath = tempDir.resolve(BACKUP_DIR + "store");
        boolean isSuccess = backupFilePath.toFile().createNewFile();
        assertThat(isSuccess).isTrue();

        storeFileManager.restoreBackupFileIfCurrentFileNotExisting();
        assertThat(storePath).exists();
        assertThat(backupFilePath).exists();
    }

    @Test
    void renameTempFileToCurrentFileIfCurrentNotExisting(@TempDir Path tempDir) throws IOException {
        Path tmpFilePath = tempDir.resolve(PersistableStoreFileManager.TEMP_FILE_PREFIX + "store");
        createEmptyFile(tmpFilePath);

        Path storePath = tempDir.resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);

        storeFileManager.renameTempFileToCurrentFile();

        assertThat(storePath).exists();
        assertThat(tmpFilePath).doesNotExist();
    }

    @Test
    void renameTempFileButStorageFileExists(@TempDir Path tempDir) throws IOException {
        Path tmpFilePath = tempDir.resolve(PersistableStoreFileManager.TEMP_FILE_PREFIX + "store");
        createEmptyFile(tmpFilePath);

        Path storePath = tempDir.resolve("store");
        createEmptyFile(storePath);

        var storeFileManager = new PersistableStoreFileManager(storePath);
        assertThrows(IOException.class, storeFileManager::renameTempFileToCurrentFile);
    }

    @Test
    void renameTempFileButTempFileDoesNotExist(@TempDir Path tempDir) {
        Path storePath = tempDir.resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);
        assertThrows(NoSuchFileException.class, storeFileManager::renameTempFileToCurrentFile);
    }

    public static void createEmptyFile(Path path) throws IOException {
        boolean isSuccess = path.toFile().createNewFile();
        assertThat(isSuccess).isTrue();
    }
}
