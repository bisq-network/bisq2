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

package bisq.common.file;

import bisq.common.jvm.DeleteOnExitHook;
import bisq.common.observable.Observable;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Slf4j
public class FileMutatorUtils {

    private static final boolean IS_POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    private static volatile boolean supportsModernFilesApi = true;
    private static volatile boolean supportsPosixPermissions = true;
    private static final AtomicBoolean configured = new AtomicBoolean(false);

    private static final Set<PosixFilePermission> OWNER_READ_WRITE_PERMISSIONS =
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    private static final Set<PosixFilePermission> OWNER_READ_WRITE_EXECUTE_PERMISSIONS =
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);

    private static final FileAttribute<Set<PosixFilePermission>> OWNER_READ_WRITE_PERMISSIONS_FILE_ATTRIBUTE =
            PosixFilePermissions.asFileAttribute(OWNER_READ_WRITE_PERMISSIONS);
    private static final FileAttribute<Set<PosixFilePermission>> OWNER_READ_WRITE_EXECUTE_PERMISSIONS_FILE_ATTRIBUTE =
            PosixFilePermissions.asFileAttribute(OWNER_READ_WRITE_EXECUTE_PERMISSIONS);

    /**
     * Sets platform file capabilities.
     * Intended to be called once at startup.
     */
    public static void setup(boolean supportsModernFilesApi, boolean supportsPosixPermissions) {
        if (!configured.compareAndSet(false, true)) {
            log.warn("FileMutatorUtils already configured, ignoring repeated setup call");
            return;
        }

        FileMutatorUtils.supportsModernFilesApi = supportsModernFilesApi;
        FileMutatorUtils.supportsPosixPermissions = supportsPosixPermissions;
    }

    /**
     * The `File.deleteOnExit` method is not suited for long-running processes as it never removes the added files,
     * thus leading to a memory leak.
     * See: <a href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6664633">...</a>
     * We added our own extended DeleteOnExitHook where we added a remove method. The client is responsible to call that
     * `remove` method via `releaseTempFile` once the file should be deleted.
     *
     * @param path The path to add a shutdown hook for delete on exit
     */
    public static void deleteOnExit(Path path) {
        if (!DeleteOnExitHook.isShutdownInProgress()) {
            DeleteOnExitHook.add(path.toString());
        }
    }

    /**
     * @param path The path to delete and to get removed from the `DeleteOnExitHook`.
     */
    public static void releaseTempFile(Path path) throws IOException {
        if (!DeleteOnExitHook.isShutdownInProgress()) {
            DeleteOnExitHook.remove(path.toString());
        }
        Files.deleteIfExists(path);
    }

    /**
     * Recursively delete a file or directory. If the path does not exist, do nothing.
     *
     * @param path The path to delete
     * @throws IOException if an I/O error occurs
     */
    public static void deleteFileOrDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            // Use try-with-resources to ensure the walk stream is closed
            try (var stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder()) // delete children before parents
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to delete: " + p, e);
                            }
                        });
            }
        }
    }

    /**
     * <b>Blocking</b>; delete path and wait until it no longer exists, polling every 50ms.
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public static void deleteFileAndWait(Path path, long timeoutMillis) throws IOException, InterruptedException {
        Files.deleteIfExists(path);
        long start = System.currentTimeMillis();
        while (Files.exists(path)) {
            if (System.currentTimeMillis() - start > timeoutMillis) {
                throw new IOException("Failed to delete file within timeout: " + path.toAbsolutePath());
            }
            Thread.sleep(50);
        }
    }

    /**
     * Create a temporary directory that is automatically deleted on JVM exit.
     *
     * @return The path to the created temporary directory
     * @throws IOException if an I/O error occurs
     */
    public static Path createTempDirPath() throws IOException {
        Path tempPath = Files.createTempDirectory(null);
        recursiveDeleteOnShutdownHook(tempPath);
        return tempPath;
    }

    private static void recursiveDeleteOnShutdownHook(Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> {
                    Thread.currentThread().setName("ShutdownHook.recursiveDelete");
                    try {
                        Files.walkFileTree(path, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path path,
                                                             @SuppressWarnings("unused") BasicFileAttributes attrs)
                                    throws IOException {
                                Files.delete(path);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path path, IOException e)
                                    throws IOException {
                                if (e == null) {
                                    Files.delete(path);
                                    return FileVisitResult.CONTINUE;
                                }
                                // directory iteration failed
                                throw e;
                            }
                        });
                    } catch (IOException e) {
                        log.error("Failed to delete " + path, e);
                    }
                }));
    }

    /**
     * Write the given content to the specified file, creating the file if it does not exist.
     * Uses the platform default charset.
     *
     * @param content The content to write
     * @param path    The path to the file
     * @throws IOException if an I/O error occurs
     */
    public static void writeToPath(String content, Path path) throws IOException {
        try {
            if (supportsModernFilesApi) {
                Files.writeString(path, content, StandardCharsets.UTF_8); // uses platform default charset
                applyDefaultPermissions(path);
            } else {
                writeToPath(content.getBytes(StandardCharsets.UTF_8), path);
            }

        } catch (IOException e) {
            log.warn("Could not write to file {}", path);
            throw e;
        }
    }

    public static void writeToPath(byte[] bytes, Path path) throws IOException {
        try {
            Files.write(path, bytes);
            applyDefaultPermissions(path);
        } catch (IOException e) {
            log.warn("Could not write to file {}", path);
            throw e;
        }
    }

    public static void resourceToFile(String resourceName, Path outputPath) throws IOException {
        try (InputStream resource = FileReaderUtils.getResourceAsStream(resourceName)) {
            Files.deleteIfExists(outputPath);
            Files.copy(resource, outputPath, StandardCopyOption.REPLACE_EXISTING);
            applyDefaultPermissions(outputPath);
        }
    }

    public static void copyFile(Path sourcePath, Path destinationPath) throws IOException {
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        applyDefaultPermissions(destinationPath);
    }

    public static void inputStreamToFile(InputStream inputStream, Path destinationPath) throws IOException {
        Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        applyDefaultPermissions(destinationPath);
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        inputStream.transferTo(outputStream);
    }

    /**
     * Rename (move) a file from oldPath to newPath. If newPath exists, it will be replaced.
     *
     * @param oldPath The current path of the file
     * @param newPath The new path of the file
     * @return true if the file was successfully renamed, false otherwise
     */
    public static boolean renameFile(Path oldPath, Path newPath) {
        try {
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            applyDefaultPermissions(newPath);
            return true;
        } catch (IOException e) {
            log.error("Failed to rename {} to {}", oldPath, newPath, e);
            return false;
        }
    }

    public static void backupCorruptedFile(Path dirPath, Path storageFilePath, String fileName, String backupFolderName)
            throws IOException {
        if (Files.exists(storageFilePath)) {
            Path corruptedBackupDirPath = dirPath.resolve(backupFolderName);
            createDirectories(corruptedBackupDirPath);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String newFileName = fileName + "_at_" + timestamp;
            Path targetPath = dirPath.resolve(backupFolderName).resolve(newFileName);
            Files.move(storageFilePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            applyDefaultPermissions(targetPath);
        }
    }

    public static OutputStream newOutputStream(Path filePath) throws IOException {
        if (supportsSettingPosixPermissions()) {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath, OWNER_READ_WRITE_PERMISSIONS_FILE_ATTRIBUTE);
            } else {
                try {
                    applyDefaultPermissions(filePath);
                } catch (UnsupportedOperationException e) {
                    // Non-POSIX FS — safe to ignore or log
                }
            }
        }
        return Files.newOutputStream(filePath);
    }

    public static void createFile(Path path) throws IOException {
        if (supportsSettingPosixPermissions()) {
            Files.createFile(path, OWNER_READ_WRITE_PERMISSIONS_FILE_ATTRIBUTE);
            applyDefaultPermissions(path);
        } else {
            Files.createFile(path);
        }
    }

    public static void createDirectories(Path path) throws IOException {
        if (supportsSettingPosixPermissions()) {
            Files.createDirectories(path, OWNER_READ_WRITE_EXECUTE_PERMISSIONS_FILE_ATTRIBUTE);
            applyDefaultPermissions(path);
        } else {
            Files.createDirectories(path);
        }
    }

    public static void createDirectory(Path path) throws IOException {
        if (supportsSettingPosixPermissions()) {
            Files.createDirectory(path, OWNER_READ_WRITE_EXECUTE_PERMISSIONS_FILE_ATTRIBUTE);
            applyDefaultPermissions(path);
        } else {
            Files.createDirectory(path);
        }
    }

    public static void copyDirectory(Path sourceDirPath,
                                     Path destinationDirPath,
                                     Set<String> extensionsToSkip) throws IOException {
        AtomicReference<IOException> exception = new AtomicReference<>();
        try (Stream<Path> stream = Files.walk(sourceDirPath)) {
            stream.forEach(source -> {
                boolean shouldSkip = false;
                if (!Files.isDirectory(source)) {
                    String fileName = source.getFileName().toString();
                    int lastDotIndex = fileName.lastIndexOf('.');
                    if (lastDotIndex > 0) {
                        String extension = fileName.substring(lastDotIndex + 1);
                        shouldSkip = extensionsToSkip.contains(extension);
                    }
                }

                if (!shouldSkip) {
                    Path relativePath = sourceDirPath.relativize(source);
                    Path destinationPath = destinationDirPath.resolve(relativePath);
                    try {
                        Files.copy(source, destinationPath);
                        applyDefaultPermissions(destinationPath);
                    } catch (IOException e) {
                        exception.set(e);
                    }
                }
            });
        }
        if (exception.get() != null) {
            throw exception.get();
        }
    }

    public static void copyResourceDirectory(String resourceDir,
                                             Path path) throws IOException, URISyntaxException {
        // Resource paths always use forward slashes on all OS.
        String normalized = resourceDir.replace('\\', '/');
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        Set<String> resources = FileReaderUtils.listResources(normalized);
        for (String resourceFile : resources) {
            String resourcePath = normalized + resourceFile; // classpath uses '/'
            Path targetFilePath = path.resolve(resourceFile); // preserve relative layout
            try {
                Path parentPath = targetFilePath.getParent();
                if (parentPath != null) {
                    createDirectories(parentPath);
                }
                resourceToFile(resourcePath, targetFilePath);
            } catch (IOException e) {
                log.error("Could not copy resource {} to {}", resourcePath, targetFilePath, e);
                throw e;
            }
        }
    }

    public static HttpURLConnection downloadFile(URL url,
                                                 Path destinationPath,
                                                 Observable<Double> progress) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int fileSize;
        try {
            connection.connect();
            try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                 OutputStream outputStream = newOutputStream(destinationPath)) {
                // If server does not provide contentLength it is -1
                fileSize = connection.getContentLength();
                double totalReadBytes = 0d;
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    if (fileSize != -1) {
                        totalReadBytes += bytesRead;
                        progress.set(totalReadBytes / fileSize);
                    }
                }
                if (fileSize == -1) {
                    progress.set(1d);
                }
            }
        } finally {
            connection.disconnect();
        }
        return connection;
    }

    private static void applyDefaultPermissions(Path path) throws IOException {
        try {
            if (supportsSettingPosixPermissions()) {
                if (Files.isDirectory(path)) {
                    Files.setPosixFilePermissions(path, OWNER_READ_WRITE_EXECUTE_PERMISSIONS);
                } else {
                    Files.setPosixFilePermissions(path, OWNER_READ_WRITE_PERMISSIONS);
                }
            }
        } catch (UnsupportedOperationException e) {
            // Non-POSIX FS — safe to ignore or log
        }
    }

    private static boolean supportsSettingPosixPermissions() {
        return IS_POSIX && supportsPosixPermissions;
    }
}
