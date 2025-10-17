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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PersistableStoreFileManagerTests {

    @Test
    void createParentDirIfExisting(@TempDir Path tempDirPath) {
        Path storePath = tempDirPath.resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);

        storeFileManager.createParentDirectoriesIfNotExisting();
        assertThat(tempDirPath).exists();
    }

    @Test
    void createParentDirIfNotExisting(@TempDir Path tempDirPath) {
        Path storePath = tempDirPath.resolve("parent_dir").resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);

        storeFileManager.createParentDirectoriesIfNotExisting();
        assertThat(storePath.getParent()).exists();
    }

    @Test
    void renameTempFileToCurrentFileIfCurrentNotExisting(@TempDir Path tempDirPath) throws IOException {
        Path tmpFilePath = tempDirPath.resolve(PersistableStoreFileManager.TEMP_FILE_PREFIX + "store");
        createEmptyFilePath(tmpFilePath);

        Path storePath = tempDirPath.resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);

        storeFileManager.renameTempFileToCurrentFile();

        assertThat(storePath).exists();
        assertThat(tmpFilePath).doesNotExist();
    }

    @Test
    void renameTempFileButStorageFileExists(@TempDir Path tempDirPath) throws IOException {
        Path tmpFilePath = tempDirPath.resolve(PersistableStoreFileManager.TEMP_FILE_PREFIX + "store");
        createEmptyFilePath(tmpFilePath);

        Path storePath = tempDirPath.resolve("store");
        createEmptyFilePath(storePath);

        var storeFileManager = new PersistableStoreFileManager(storePath);
        assertThrows(IOException.class, storeFileManager::renameTempFileToCurrentFile);
    }

    @Test
    void renameTempFileButTempFileDoesNotExist(@TempDir Path tempDirPath) {
        Path storePath = tempDirPath.resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);
        assertThrows(NoSuchFileException.class, storeFileManager::renameTempFileToCurrentFile);
    }

    public static void createEmptyFilePath(Path path) throws IOException {
        Files.createFile(path);
    }
}
