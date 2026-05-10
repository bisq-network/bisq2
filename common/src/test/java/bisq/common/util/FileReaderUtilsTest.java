package bisq.common.util;

import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class FileReaderUtilsTest {

    @Test
    @DisplayName("has resource file")
    public void has_resource_file() {
        assertTrue(FileReaderUtils.hasResourceFile("logback.xml"));
        assertFalse(FileReaderUtils.hasResourceFile("logback.xml1"));
    }

    @Test
    @DisplayName("read utf8 string file")
    void read_utf8_string_file(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("test2.txt");
        FileMutatorUtils.writeToPath("abc".getBytes(), path);
        assertEquals("abc", FileReaderUtils.readUTF8String(path));
    }

    @Test
    @DisplayName("wait until file exists")
    void wait_until_file_exists(@TempDir Path tempDirPath) throws InterruptedException, TimeoutException {
        Path path = tempDirPath.resolve("waitExists.txt");
        new Thread(() -> {
            try {
                Thread.sleep(200);
                FileMutatorUtils.createFile(path);
            } catch (Exception ignored) {
            }
        }).start();
        FileReaderUtils.waitUntilFileExists(path, 1000);
        assertThat(path).exists();
    }

    @Test
    @DisplayName("list regular files in directory")
    void list_regular_files_in_directory(@TempDir Path tempDirPath) throws IOException {
        Path dirPath = tempDirPath.resolve("listdir");
        FileMutatorUtils.createDirectory(dirPath);
        FileMutatorUtils.createFile(dirPath.resolve("a.txt"));
        FileMutatorUtils.createFile(dirPath.resolve("b.txt"));
        Set<String> files = FileReaderUtils.listFilesInDirectory(dirPath, 1);
        assertTrue(files.contains("a.txt"));
        assertTrue(files.contains("b.txt"));
    }

    @Test
    @DisplayName("read from file if present")
    void read_from_file_if_present(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("present.txt");
        FileMutatorUtils.writeToPath("abc".getBytes(), path);
        Optional<String> result = FileReaderUtils.readFromFileIfPresent(path);
        assertTrue(result.isPresent());
        assertEquals("abc", result.get());
    }

    @Test
    @DisplayName("read from scanner")
    void read_from_scanner() {
        String input = String.format("%s%s%s", "line1", System.lineSeparator(), "line2");
        Scanner scanner = new Scanner(input);
        String expected = String.format("%s%s%s", "line1", System.lineSeparator(), "line2");
        String actual = FileReaderUtils.readFromScanner(scanner);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("get resource as stream")
    void get_resource_as_stream() throws IOException {
        InputStream is = FileReaderUtils.getResourceAsStream("logback.xml");
        assertNotNull(is);
        is.close();
    }

    @Test
    @DisplayName("list directories")
    void list_directories(@TempDir Path tempDirPath) throws IOException {
        Path dirPath = tempDirPath.resolve("listdirs");
        FileMutatorUtils.createDirectory(dirPath);

        // Create subdirectories
        Path subPath1 = dirPath.resolve("subdir1");
        Path subPath2 = dirPath.resolve("subdir2");
        FileMutatorUtils.createDirectory(subPath1);
        FileMutatorUtils.createDirectory(subPath2);

        // Create a file to ensure it is not included
        Path filePath = dirPath.resolve("file.txt");
        FileMutatorUtils.createFile(filePath);

        // Call method under test
        Set<String> dirs = FileReaderUtils.listDirectories(dirPath);

        // Assertions
        assertEquals(2, dirs.size(), "Should only contain 2 directories");
        assertTrue(dirs.contains("subdir1"), "subdir1 should be listed");
        assertTrue(dirs.contains("subdir2"), "subdir2 should be listed");
        assertFalse(dirs.contains("file.txt"), "Files should not be listed");
    }

    @Test
    @DisplayName("has resource file nonexistent")
    void has_resource_file_nonexistent() {
        assertTrue(FileReaderUtils.hasResourceFile("logback.xml"));
        assertFalse(FileReaderUtils.hasResourceFile("nonexistent.xml"));
    }

    @Test
    @DisplayName("list resources")
    void list_resources() throws Exception {
        Set<String> resources = FileReaderUtils.listResources("bisq/common/util/");
        assertTrue(resources.contains("FileReaderUtilsTest.class"));
    }
}
