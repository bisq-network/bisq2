package bisq.common.util;

import bisq.common.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
public class FileUtilsTest {

    @Test
    public void hasResourceFile() {
        assertTrue(FileUtils.hasResourceFile("logback.xml"));
        assertFalse(FileUtils.hasResourceFile("logback.xml1"));
    }

    @Test
    void testReadUtf8StringFile(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("test2.txt");
        Files.write(path, "abc".getBytes());
        assertEquals("abc", FileUtils.readUTF8String(path));
    }

    @Test
    void testDeleteOnExitAndReleaseTempFile(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("deleteOnExit.txt");
        Files.createFile(path);
        FileUtils.deleteOnExit(path);
        FileUtils.releaseTempFile(path);
        assertThat(path).doesNotExist();
    }

    @Test
    void testDeleteFileOrDirectory(@TempDir Path tempDirPath) throws IOException {
        Path dirPath = tempDirPath.resolve("dir");
        Files.createDirectory(dirPath);
        Path filePath = dirPath.resolve("file.txt");
        Files.createFile(filePath);
        FileUtils.deleteFileOrDirectory(dirPath);
        assertThat(filePath).doesNotExist();
    }

    @Test
    void testDeleteFileOrDirectoryWithComplexDirectory(@TempDir Path tempDirPath) throws IOException {
        // Setup complex nested directories and files
        Path subDirPath1 = tempDirPath.resolve("subDir1");
        Path subDirPath2 = tempDirPath.resolve("subDir2");
        Files.createDirectories(subDirPath1);
        Files.createDirectories(subDirPath2);

        // Create files at multiple levels
        Files.writeString(tempDirPath.resolve("rootFile.txt"), "root");
        Files.writeString(subDirPath1.resolve("file1.txt"), "file1");
        Files.writeString(subDirPath1.resolve("file2.txt"), "file2");
        Files.writeString(subDirPath2.resolve("file3.txt"), "file3");

        Path nestedPath = subDirPath2.resolve("nested");
        Files.createDirectory(nestedPath);
        Files.writeString(nestedPath.resolve("nestedFile.txt"), "nested");

        // Ensure files exist before deletion
        assertThat(tempDirPath.resolve("rootFile.txt")).exists();
        assertThat(subDirPath1.resolve("file1.txt")).exists();
        assertThat(nestedPath.resolve("nestedFile.txt")).exists();

        // Perform recursive delete
        FileUtils.deleteFileOrDirectory(tempDirPath);

        // Assert everything is deleted
        assertThat(tempDirPath).doesNotExist();
        assertThat(subDirPath1).doesNotExist();
        assertThat(nestedPath).doesNotExist();
    }

    @Test
    void testDeleteFileAndWait(@TempDir Path tempDirPath) throws IOException, InterruptedException {
        Path path = tempDirPath.resolve("wait.txt");
        Files.createFile(path);
        FileUtils.deleteFileAndWait(path, 1000);
        assertThat(path).doesNotExist();
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
        FileUtils.waitUntilFileExists(path, 1000);
        assertThat(path).exists();
    }

    @Test
    void testCreateTempDirPath() throws IOException {
        Path tempPath = FileUtils.createTempDirPath();
        assertTrue(Files.isDirectory(tempPath));
        // Clean up
        FileUtils.deleteFileOrDirectory(tempPath);
    }

    @Test
    void testListRegularFilesInDirectory(@TempDir Path tempDirPath) throws IOException {
        Path dirPath = tempDirPath.resolve("listdir");
        Files.createDirectory(dirPath);
        Files.createFile(dirPath.resolve("a.txt"));
        Files.createFile(dirPath.resolve("b.txt"));
        Set<String> files = FileUtils.listFilesInDirectory(dirPath, 1);
        assertTrue(files.contains("a.txt"));
        assertTrue(files.contains("b.txt"));
    }

    @Test
    void testRenameFileOverExistingFile(@TempDir Path tempDirPath) throws IOException {
        // Create the source file
        Path sourceFilePath = tempDirPath.resolve("source.txt");
        Files.writeString(sourceFilePath, "Hello source");

        // Create the target file that already exists
        Path targetFilePath = tempDirPath.resolve("target.txt");
        Files.writeString(targetFilePath, "Existing target");

        // Perform the rename (should overwrite existing target)
        boolean success = FileUtils.renameFile(sourceFilePath, targetFilePath);

        assertTrue(success, "Rename should succeed");

        // Source file should no longer exist
        assertFalse(Files.exists(sourceFilePath), "Source file should be deleted");

        // Target file should exist and contain the source content
        assertTrue(Files.exists(targetFilePath), "Target file should exist");
        String content = Files.readString(targetFilePath);
        assertEquals("Hello source", content, "Target file should have the source content");
    }

    @Test
    void testWriteToPath(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("writefile.txt");
        FileUtils.writeToPath("data", path);
        assertEquals("data", Files.readString(path));
    }

    @Test
    void testReadFromFileIfPresent(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("present.txt");
        Files.write(path, "abc".getBytes());
        Optional<String> result = FileUtils.readFromFileIfPresent(path);
        assertTrue(result.isPresent());
        assertEquals("abc", result.get());
    }

    @Test
    void testReadFromScanner() {
        String input = String.format("%s%s%s", "line1", System.lineSeparator(), "line2");
        Scanner scanner = new Scanner(input);
        String expected = String.format("%s%s%s", "line1", System.lineSeparator(), "line2");
        String actual = FileUtils.readFromScanner(scanner);
        assertEquals(expected, actual);
    }

    @Test
    void testGetResourceAsStream() throws IOException {
        InputStream is = FileUtils.getResourceAsStream("logback.xml");
        assertNotNull(is);
        is.close();
    }

    @Test
    void testResourceToFile(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("resource.txt");
        FileUtils.resourceToFile("logback.xml", path);
        assertThat(path).exists();
    }

    @Test
    void testCopyFile(@TempDir Path tempDirPath) throws IOException {
        Path srcPath = tempDirPath.resolve("src.txt");
        Path destPath = tempDirPath.resolve("dest.txt");
        Files.write(srcPath, "copy".getBytes());
        FileUtils.copyFile(srcPath, destPath);
        assertEquals("copy", Files.readString(destPath));
    }

    @Test
    void testCopyInputStream() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("abc".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtils.copy(in, out);
        assertEquals("abc", out.toString());
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
        Set<String> dirs = FileUtils.listDirectories(dirPath);

        // Assertions
        assertEquals(2, dirs.size(), "Should only contain 2 directories");
        assertTrue(dirs.contains("subdir1"), "subdir1 should be listed");
        assertTrue(dirs.contains("subdir2"), "subdir2 should be listed");
        assertFalse(dirs.contains("file.txt"), "Files should not be listed");
    }

    @Test
    void testRenameFile(@TempDir Path tempDirPath) throws IOException {
        Path oldPath = tempDirPath.resolve("old.txt");
        Path newPath = tempDirPath.resolve("new.txt");
        Files.write(oldPath, "rename".getBytes());
        assertTrue(FileUtils.renameFile(oldPath, newPath));
        assertTrue(Files.exists(newPath));
        assertFalse(Files.exists(oldPath));
    }

    @Test
    void testBackupCorruptedFile(@TempDir Path tempDirPath) throws IOException {
        Path backupPath = tempDirPath.resolve("backup");
        Files.createDirectory(backupPath);
        Path corruptPath = backupPath.resolve("corrupt.txt");
        Files.write(corruptPath, "data".getBytes());
        FileUtils.backupCorruptedFile(backupPath, corruptPath, "corrupt.txt", "backup");
        Set<String> files = FileUtils.listRegularFiles(backupPath.resolve("backup"));
        assertTrue(files.stream().anyMatch(f -> f.startsWith("corrupt.txt_at_")));
    }

    @Test
    void testHasResourceFile() {
        assertTrue(FileUtils.hasResourceFile("logback.xml"));
        assertFalse(FileUtils.hasResourceFile("nonexistent.xml"));
    }

    @Test
    void testCopyDirectoryWithExtensionsToSkip(@TempDir Path tempDirPath) throws IOException {
        Path srcDirPath = tempDirPath.resolve("srcdir2");
        Path destDirPath = tempDirPath.resolve("destdir2");
        Files.createDirectory(srcDirPath);
        Files.write(srcDirPath.resolve("file.txt"), "abc".getBytes());
        Files.write(srcDirPath.resolve("file.skip"), "skip".getBytes());
        FileUtils.copyDirectory(srcDirPath, destDirPath, Set.of("skip"));
        assertTrue(Files.exists(destDirPath.resolve("file.txt")));
        assertFalse(Files.exists(destDirPath.resolve("file.skip")));
    }

    @Test
    void testListResources() throws Exception {
        Set<String> resources = FileUtils.listResources("bisq/common/util/");
        assertTrue(resources.contains("FileUtilsTest.class"));
    }

    @Test
    void testCopyResourceDirectory(@TempDir Path tempDirPath) throws Exception {
        Path targetDirPath = tempDirPath.resolve("resourceCopy");
        FileUtils.copyResourceDirectory("bisq/common/util/", targetDirPath);
        assertTrue(Files.exists(targetDirPath.resolve("FileUtilsTest.class")));
    }
}
