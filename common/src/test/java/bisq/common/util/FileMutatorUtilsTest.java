package bisq.common.util;

import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class FileMutatorUtilsTest {

    @Test
    void testDeleteOnExitAndReleaseTempFile(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("deleteOnExit.txt");
        Files.createFile(path);
        FileMutatorUtils.deleteOnExit(path);
        FileMutatorUtils.releaseTempFile(path);
        assertThat(path).doesNotExist();
    }

    @Test
    void testDeleteFileOrDirectory(@TempDir Path tempDirPath) throws IOException {
        Path dirPath = tempDirPath.resolve("dir");
        Files.createDirectory(dirPath);
        Path filePath = dirPath.resolve("file.txt");
        Files.createFile(filePath);
        FileMutatorUtils.deleteFileOrDirectory(dirPath);
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
        FileMutatorUtils.deleteFileOrDirectory(tempDirPath);

        // Assert everything is deleted
        assertThat(tempDirPath).doesNotExist();
        assertThat(subDirPath1).doesNotExist();
        assertThat(nestedPath).doesNotExist();
    }

    @Test
    void testDeleteFileAndWait(@TempDir Path tempDirPath) throws IOException, InterruptedException {
        Path path = tempDirPath.resolve("wait.txt");
        Files.createFile(path);
        FileMutatorUtils.deleteFileAndWait(path, 1000);
        assertThat(path).doesNotExist();
    }


    @Test
    void testCreateTempDirPath() throws IOException {
        Path tempPath = FileMutatorUtils.createTempDirPath();
        assertTrue(Files.isDirectory(tempPath));
        // Clean up
        FileMutatorUtils.deleteFileOrDirectory(tempPath);
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
        boolean success = FileMutatorUtils.renameFile(sourceFilePath, targetFilePath);

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
        FileMutatorUtils.writeToPath("data", path);
        assertEquals("data", Files.readString(path));
    }

    @Test
    void testResourceToFile(@TempDir Path tempDirPath) throws IOException {
        Path path = tempDirPath.resolve("resource.txt");
        FileMutatorUtils.resourceToFile("logback.xml", path);
        assertThat(path).exists();
    }

    @Test
    void testCopyFile(@TempDir Path tempDirPath) throws IOException {
        Path srcPath = tempDirPath.resolve("src.txt");
        Path destPath = tempDirPath.resolve("dest.txt");
        Files.write(srcPath, "copy".getBytes());
        FileMutatorUtils.copyFile(srcPath, destPath);
        assertEquals("copy", Files.readString(destPath));
    }

    @Test
    void testCopyInputStream() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("abc".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileMutatorUtils.copy(in, out);
        assertEquals("abc", out.toString());
    }

    @Test
    void testRenameFile(@TempDir Path tempDirPath) throws IOException {
        Path oldPath = tempDirPath.resolve("old.txt");
        Path newPath = tempDirPath.resolve("new.txt");
        Files.write(oldPath, "rename".getBytes());
        assertTrue(FileMutatorUtils.renameFile(oldPath, newPath));
        assertTrue(Files.exists(newPath));
        assertFalse(Files.exists(oldPath));
    }

    @Test
    void testBackupCorruptedFile(@TempDir Path tempDirPath) throws IOException {
        Path backupPath = tempDirPath.resolve("backup");
        Files.createDirectory(backupPath);
        Path corruptPath = backupPath.resolve("corrupt.txt");
        Files.write(corruptPath, "data".getBytes());
        FileMutatorUtils.backupCorruptedFile(backupPath, corruptPath, "corrupt.txt", "backup");
        Set<String> files = FileReaderUtils.listRegularFiles(backupPath.resolve("backup"));
        assertTrue(files.stream().anyMatch(f -> f.startsWith("corrupt.txt_at_")));
    }

    @Test
    void testCopyDirectoryWithExtensionsToSkip(@TempDir Path tempDirPath) throws IOException {
        Path srcDirPath = tempDirPath.resolve("srcdir2");
        Path destDirPath = tempDirPath.resolve("destdir2");
        Files.createDirectory(srcDirPath);
        Files.write(srcDirPath.resolve("file.txt"), "abc".getBytes());
        Files.write(srcDirPath.resolve("file.skip"), "skip".getBytes());
        FileMutatorUtils.copyDirectory(srcDirPath, destDirPath, Set.of("skip"));
        assertTrue(Files.exists(destDirPath.resolve("file.txt")));
        assertFalse(Files.exists(destDirPath.resolve("file.skip")));
    }

    @Test
    void testCopyResourceDirectory(@TempDir Path tempDirPath) throws Exception {
        Path targetDirPath = tempDirPath.resolve("resourceCopy");
        FileMutatorUtils.copyResourceDirectory("bisq/common/util/", targetDirPath);
        assertTrue(Files.exists(targetDirPath.resolve("FileMutatorUtilsTest.class")));
    }
}
