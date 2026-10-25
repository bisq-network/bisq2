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
package bisq.network.tor.common.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUtilsTest {
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void createdDirectoriesAndFilesAreOnlyAccessibleByTheOwner(@TempDir Path tempDirPath) throws IOException {
        Path dirPath = tempDirPath.resolve("a").resolve("b");
        FileUtils.createDirectories(dirPath);
        Path singleDirPath = tempDirPath.resolve("c");
        FileUtils.createDirectory(singleDirPath);
        Path filePath = dirPath.resolve("file");
        FileUtils.writeToPath("content", filePath);
        Path copiedFilePath = dirPath.resolve("copied");
        FileUtils.inputStreamToFile(new ByteArrayInputStream(new byte[]{1, 2}), copiedFilePath);

        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(dirPath))).isEqualTo("rwx------");
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(singleDirPath))).isEqualTo("rwx------");
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(filePath))).isEqualTo("rw-------");
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(copiedFilePath))).isEqualTo("rw-------");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void posixPermissionsAreNotSetIfNotAllowed(@TempDir Path tempDirPath) throws IOException {
        Path referenceDirPath = Files.createDirectories(tempDirPath.resolve("reference"));
        Path referenceFilePath = Files.write(tempDirPath.resolve("reference-file"), new byte[0]);

        Path dirPath = tempDirPath.resolve("dir");
        FileUtils.createDirectories(dirPath, false);
        Path filePath = tempDirPath.resolve("file");
        FileUtils.writeToPath("content", filePath, false);

        assertThat(Files.getPosixFilePermissions(dirPath)).isEqualTo(Files.getPosixFilePermissions(referenceDirPath));
        assertThat(Files.getPosixFilePermissions(filePath)).isEqualTo(Files.getPosixFilePermissions(referenceFilePath));
    }

    @Test
    void createDirectoriesAcceptsAnExistingDirectory(@TempDir Path tempDirPath) throws IOException {
        FileUtils.createDirectories(tempDirPath);
        FileUtils.createDirectories(tempDirPath, false);

        assertThat(tempDirPath).isDirectory();
    }

    @Test
    void writtenTextIsReadBack(@TempDir Path tempDirPath) throws IOException {
        Path filePath = tempDirPath.resolve("file");
        FileUtils.writeToPath("äöü €", filePath);

        assertThat(Files.readAllBytes(filePath)).isEqualTo("äöü €".getBytes(StandardCharsets.UTF_8));
        assertThat(FileUtils.readUTF8String(filePath)).isEqualTo("äöü €");
    }

    @Test
    void writeToPathReplacesTheContent(@TempDir Path tempDirPath) throws IOException {
        Path filePath = tempDirPath.resolve("file");
        FileUtils.writeToPath("a longer first content", filePath);
        FileUtils.writeToPath("second", filePath);

        assertThat(FileUtils.readUTF8String(filePath)).isEqualTo("second");
    }

    @Test
    void malformedUtf8IsRejected(@TempDir Path tempDirPath) throws IOException {
        Path filePath = Files.write(tempDirPath.resolve("file"), new byte[]{(byte) 0xC3, (byte) 0x28});

        assertThatThrownBy(() -> FileUtils.readUTF8String(filePath)).isInstanceOf(CharacterCodingException.class);
        assertThatThrownBy(() -> FileUtils.writeToPath("\uD800", tempDirPath.resolve("other")))
                .isInstanceOf(CharacterCodingException.class);
    }

    @Test
    void resourceLinesAreJoinedWithTheLineSeparatorOfTheSystem() throws IOException {
        String content = FileUtils.readStringFromResource("file_utils_test_resource.txt");

        assertThat(content).isEqualTo(String.join(System.lineSeparator(), "first line", "second line", "third line"));
    }

    @Test
    void missingResourceFails() {
        assertThatThrownBy(() -> FileUtils.readStringFromResource("missing_resource.txt"))
                .isInstanceOf(IOException.class)
                .hasMessage("Could not load missing_resource.txt");
    }

    @Test
    void deleteFileAndWaitDeletesTheFile(@TempDir Path tempDirPath) throws IOException, InterruptedException {
        Path filePath = Files.write(tempDirPath.resolve("file"), new byte[0]);

        FileUtils.deleteFileAndWait(filePath, 1000);
        FileUtils.deleteFileAndWait(filePath, 1000);

        assertThat(filePath).doesNotExist();
    }
}
