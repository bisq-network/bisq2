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
    void testReadUtf8StringFile(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("test2.txt");
        Files.write(path, "abc".getBytes());
        assertEquals("abc", FileUtils.readUTF8String(path));
    }

    @Test
    void testDeleteOnExitAndReleaseTempFile(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("deleteOnExit.txt");
        Files.createFile(path);
        FileUtils.deleteOnExit(path);
        FileUtils.releaseTempFile(path);
        assertThat(path).doesNotExist();
    }

    @Test
    void testDeleteFileOrDirectory(@TempDir Path tempDir) throws IOException {
        Path dir = tempDir.resolve("dir");
        Files.createDirectory(dir);
        Path file = dir.resolve("file.txt");
        Files.createFile(file);
        FileUtils.deleteFileOrDirectory(dir);
        assertThat(file).doesNotExist();
    }

    @Test
    void testDeleteFileOrDirectoryWithComplexDirectory(@TempDir Path tempDir) throws IOException {
        // Setup complex nested directories and files
        Path subDir1 = tempDir.resolve("subDir1");
        Path subDir2 = tempDir.resolve("subDir2");
        Files.createDirectories(subDir1);
        Files.createDirectories(subDir2);

        // Create files at multiple levels
        Files.writeString(tempDir.resolve("rootFile.txt"), "root");
        Files.writeString(subDir1.resolve("file1.txt"), "file1");
        Files.writeString(subDir1.resolve("file2.txt"), "file2");
        Files.writeString(subDir2.resolve("file3.txt"), "file3");

        Path nested = subDir2.resolve("nested");
        Files.createDirectory(nested);
        Files.writeString(nested.resolve("nestedFile.txt"), "nested");

        // Ensure files exist before deletion
        assertThat(tempDir.resolve("rootFile.txt")).exists();
        assertThat(subDir1.resolve("file1.txt")).exists();
        assertThat(nested.resolve("nestedFile.txt")).exists();

        // Perform recursive delete
        FileUtils.deleteFileOrDirectory(tempDir);

        // Assert everything is deleted
        assertThat(tempDir).doesNotExist();
        assertThat(subDir1).doesNotExist();
        assertThat(nested).doesNotExist();
    }

    @Test
    void testDeleteFileAndWait(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path path = tempDir.resolve("wait.txt");
        Files.createFile(path);
        FileUtils.deleteFileAndWait(path, 1000);
        assertThat(path).doesNotExist();
    }

    @Test
    void testWaitUntilFileExists(@TempDir Path tempDir) throws InterruptedException, TimeoutException {
        Path path = tempDir.resolve("waitExists.txt");
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
    void testCreateTempDir() throws IOException {
        Path temp = FileUtils.createTempDir();
        assertTrue(Files.isDirectory(temp));
        // Clean up
        FileUtils.deleteFileOrDirectory(temp);
    }

    @Test
    void testListFilesInDirectory(@TempDir Path tempDir) throws IOException {
        Path dir = tempDir.resolve("listdir");
        Files.createDirectory(dir);
        Files.createFile(dir.resolve("a.txt"));
        Files.createFile(dir.resolve("b.txt"));
        Set<String> files = FileUtils.listFilesInDirectory(dir, 1);
        assertTrue(files.contains("a.txt"));
        assertTrue(files.contains("b.txt"));
    }

    @Test
    void testRenameFileOverExistingFile(@TempDir Path tempDir) throws IOException {
        // Create the source file
        Path sourceFile = tempDir.resolve("source.txt");
        Files.writeString(sourceFile, "Hello source");

        // Create the target file that already exists
        Path targetFile = tempDir.resolve("target.txt");
        Files.writeString(targetFile, "Existing target");

        // Perform the rename (should overwrite existing target)
        boolean success = FileUtils.renameFile(sourceFile, targetFile);

        assertTrue(success, "Rename should succeed");

        // Source file should no longer exist
        assertFalse(Files.exists(sourceFile), "Source file should be deleted");

        // Target file should exist and contain the source content
        assertTrue(Files.exists(targetFile), "Target file should exist");
        String content = Files.readString(targetFile);
        assertEquals("Hello source", content, "Target file should have the source content");
    }

    @Test
    void testWriteToFile(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("writefile.txt");
        FileUtils.writeToFile("data", path);
        assertEquals("data", Files.readString(path));
    }

    @Test
    void testReadFromFileIfPresent(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("present.txt");
        Files.write(path, "abc".getBytes());
        Optional<String> result = FileUtils.readFromFileIfPresent(path);
        assertTrue(result.isPresent());
        assertEquals("abc", result.get());
    }

    @Test
    void testReadFromScanner() {
        Scanner scanner = new Scanner("line1\nline2");
        assertEquals("line1\nline2", FileUtils.readFromScanner(scanner));
    }

    @Test
    void testGetResourceAsStream() throws IOException {
        InputStream is = FileUtils.getResourceAsStream("logback.xml");
        assertNotNull(is);
        is.close();
    }

    @Test
    void testResourceToFile(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("resource.txt");
        FileUtils.resourceToFile("logback.xml", path);
        assertThat(path).exists();
    }

    @Test
    void testCopyFile(@TempDir Path tempDir) throws IOException {
        Path src = tempDir.resolve("src.txt");
        Path dest = tempDir.resolve("dest.txt");
        Files.write(src, "copy".getBytes());
        FileUtils.copyFile(src, dest);
        assertEquals("copy", Files.readString(dest));
    }

    @Test
    void testCopyInputStream() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("abc".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtils.copy(in, out);
        assertEquals("abc", out.toString());
    }

    @Test
    void testListDirectories(@TempDir Path tempDir) throws IOException {
        Path dir = tempDir.resolve("listdirs");
        Files.createDirectory(dir);

        // Create subdirectories
        Path sub1 = dir.resolve("subdir1");
        Path sub2 = dir.resolve("subdir2");
        Files.createDirectory(sub1);
        Files.createDirectory(sub2);

        // Create a file to ensure it is not included
        Path file = dir.resolve("file.txt");
        Files.createFile(file);

        // Call method under test
        Set<String> dirs = FileUtils.listDirectories(dir);

        // Assertions
        assertEquals(2, dirs.size(), "Should only contain 2 directories");
        assertTrue(dirs.contains("subdir1"), "subdir1 should be listed");
        assertTrue(dirs.contains("subdir2"), "subdir2 should be listed");
        assertFalse(dirs.contains("file.txt"), "Files should not be listed");
    }

    @Test
    void testRenameFile(@TempDir Path tempDir) throws IOException {
        Path oldPath = tempDir.resolve("old.txt");
        Path newPath = tempDir.resolve("new.txt");
        Files.write(oldPath, "rename".getBytes());
        assertTrue(FileUtils.renameFile(oldPath, newPath));
        assertTrue(Files.exists(newPath));
        assertFalse(Files.exists(oldPath));
    }

    @Test
    void testBackupCorruptedFile(@TempDir Path tempDir) throws IOException {
        Path dir = tempDir.resolve("backup");
        Files.createDirectory(dir);
        Path path = dir.resolve("corrupt.txt");
        Files.write(path, "data".getBytes());
        FileUtils.backupCorruptedFile(dir.toString(), path, "corrupt.txt", "backup");
        Set<String> files = FileUtils.listFiles(dir.resolve("backup"));
        assertTrue(files.stream().anyMatch(f -> f.startsWith("corrupt.txt_at_")));
    }

    @Test
    void testHasResourceFile() {
        assertTrue(FileUtils.hasResourceFile("logback.xml"));
        assertFalse(FileUtils.hasResourceFile("nonexistent.xml"));
    }

    @Test
    void testCopyDirectoryWithExtensionsToSkip(@TempDir Path tempDir) throws IOException {
        Path srcDir = tempDir.resolve("srcdir2");
        Path destDir = tempDir.resolve("destdir2");
        Files.createDirectory(srcDir);
        Files.write(srcDir.resolve("file.txt"), "abc".getBytes());
        Files.write(srcDir.resolve("file.skip"), "skip".getBytes());
        FileUtils.copyDirectory(srcDir.toString(), destDir.toString(), Set.of("skip"));
        assertTrue(Files.exists(destDir.resolve("file.txt")));
        assertFalse(Files.exists(destDir.resolve("file.skip")));
    }

    @Test
    void testListResources() throws Exception {
        Set<String> resources = FileUtils.listResources("bisq/common/util/");
        assertTrue(resources.contains("FileUtilsTest.class"));
    }

    @Test
    void testCopyResourceDirectory(@TempDir Path tempDir) throws Exception {
        Path targetDir = tempDir.resolve("resourceCopy");
        FileUtils.copyResourceDirectory("bisq/common/util/", targetDir);
        assertTrue(Files.exists(targetDir.resolve("FileUtilsTest.class")));
    }
}
