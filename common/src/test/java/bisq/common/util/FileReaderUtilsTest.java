package bisq.common.util;

import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileReaderUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    public void hasResourceFile() {
        assertTrue(FileReaderUtils.hasResourceFile("logback.xml"));
        assertFalse(FileReaderUtils.hasResourceFile("logback.xml1"));
    }

    @Test
    void testReadUtf8StringFile(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("test2.txt");
        Files.write(path, "abc".getBytes());
        assertEquals("abc", FileReaderUtils.readUTF8String(path));
    }

    @Test
    void testWaitUntilFileExists(@TempDir Path tempDirPath) throws InterruptedException, TimeoutException {
        Path path = tempDirPath.resolve("waitExists.txt");
        new Thread(() -> {
            try {
                Thread.sleep(200);
                Files.createFile(path);
            } catch (Exception ignored) {
            }
        }).start();
        FileReaderUtils.waitUntilFileExists(path, 1000);
        assertThat(path).exists();
    }

    @Test
    void testListRegularFilesInDirectory(@TempDir Path tempDirPath) throws IOException {
        Path dirPath = tempDirPath.resolve("listdir");
        Files.createDirectory(dirPath);
        Files.createFile(dirPath.resolve("a.txt"));
        Files.createFile(dirPath.resolve("b.txt"));
        Set<String> files = FileReaderUtils.listFilesInDirectory(dirPath, 1);
        assertTrue(files.contains("a.txt"));
        assertTrue(files.contains("b.txt"));
    }

    @Test
    void testReadFromFileIfPresent(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("present.txt");
        Files.write(path, "abc".getBytes());
        Optional<String> result = FileReaderUtils.readFromFileIfPresent(path);
        assertTrue(result.isPresent());
        assertEquals("abc", result.get());
    }

    @Test
    void testReadFromScanner() {
        String input = String.format("%s%s%s", "line1", System.lineSeparator(), "line2");
        Scanner scanner = new Scanner(input);
        String expected = String.format("%s%s%s", "line1", System.lineSeparator(), "line2");
        String actual = FileReaderUtils.readFromScanner(scanner);
        assertEquals(expected, actual);
    }

    @Test
    void testGetResourceAsStream() throws IOException {
        InputStream is = FileReaderUtils.getResourceAsStream("logback.xml");
        assertNotNull(is);
        is.close();
    }

    @Test
    void testListDirectories(@TempDir Path tempDirPath) throws IOException {
        Path dirPath = tempDirPath.resolve("listdirs");
        Files.createDirectory(dirPath);

        // Create subdirectories
        Path subPath1 = dirPath.resolve("subdir1");
        Path subPath2 = dirPath.resolve("subdir2");
        Files.createDirectory(subPath1);
        Files.createDirectory(subPath2);

        // Create a file to ensure it is not included
        Path filePath = dirPath.resolve("file.txt");
        Files.createFile(filePath);

        // Call method under test
        Set<String> dirs = FileReaderUtils.listDirectories(dirPath);

        // Assertions
        assertEquals(2, dirs.size(), "Should only contain 2 directories");
        assertTrue(dirs.contains("subdir1"), "subdir1 should be listed");
        assertTrue(dirs.contains("subdir2"), "subdir2 should be listed");
        assertFalse(dirs.contains("file.txt"), "Files should not be listed");
    }

    @Test
    void testHasResourceFile() {
        assertTrue(FileReaderUtils.hasResourceFile("logback.xml"));
        assertFalse(FileReaderUtils.hasResourceFile("nonexistent.xml"));
    }

    @Test
    void testListResources() throws Exception {
        Set<String> resources = FileReaderUtils.listResources("bisq/common/util/");
        assertTrue(resources.contains("FileReaderUtilsTest.class"));
    }
}
