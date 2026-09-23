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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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

    @Test
    void renameTempFileFailsWhenTheMoveIsRefused(@TempDir Path tempDirPath) throws IOException {
        // A rename the file system refuses is reported by the utility as false, not thrown. Swallowed,
        // the write would count as done while the active store file is missing.
        assumeCanRevokeDirectoryWrite();
        Path tmpFilePath = tempDirPath.resolve(PersistableStoreFileManager.TEMP_FILE_PREFIX + "store");
        createEmptyFilePath(tmpFilePath);
        Path storePath = tempDirPath.resolve("store");
        var storeFileManager = new PersistableStoreFileManager(storePath);

        Set<PosixFilePermission> original = Files.getPosixFilePermissions(tempDirPath);
        Files.setPosixFilePermissions(tempDirPath, PosixFilePermissions.fromString("r-x------"));
        try {
            assertThrows(IOException.class, storeFileManager::renameTempFileToCurrentFile);
        } finally {
            Files.setPosixFilePermissions(tempDirPath, original);
        }
        assertThat(storePath).doesNotExist();
        assertThat(tmpFilePath).exists();
    }

    static void assumeCanRevokeDirectoryWrite() {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                "Needs POSIX permissions to make the directory read-only");
        assumeTrue(!"root".equals(System.getProperty("user.name")),
                "root ignores directory permissions");
    }

    public static void createEmptyFilePath(Path path) throws IOException {
        FileMutatorUtils.createFile(path);
    }
}
