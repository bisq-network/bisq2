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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class FileUtils {
    public static final String FILE_SEP = File.separator;

    public static String readUTF8String(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public static void writeUTF8String(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
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
     * <b>Blocking</b>; Waits until the specified path exists, polling every 100ms up to the given timeout.
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public static void waitUntilFileExists(Path path,
                                           long timeoutMillis) throws InterruptedException, TimeoutException {
        long start = System.currentTimeMillis();
        while (!Files.exists(path)) {
            if (System.currentTimeMillis() - start > timeoutMillis) {
                throw new TimeoutException("File did not exist after " + timeoutMillis + " ms: " + path.toAbsolutePath());
            }
            Thread.sleep(100);
        }
    }

    /**
     * Create a temporary directory that is automatically deleted on JVM exit.
     *
     * @return The path to the created temporary directory
     * @throws IOException if an I/O error occurs
     */
    public static Path createTempDir() throws IOException {
        Path tempDirPath = Files.createTempDirectory(null);
        recursiveDeleteOnShutdownHook(tempDirPath);
        return tempDirPath;
    }

    private static void recursiveDeleteOnShutdownHook(Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> {
                    Thread.currentThread().setName("ShutdownHook.recursiveDelete");
                    try {
                        Files.walkFileTree(path, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file,
                                                             @SuppressWarnings("unused") BasicFileAttributes attrs)
                                    throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                                    throws IOException {
                                if (e == null) {
                                    Files.delete(dir);
                                    return FileVisitResult.CONTINUE;
                                }
                                // directory iteration failed
                                throw e;
                            }
                        });
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete " + path, e);
                    }
                }));
    }

    /**
     * List all files (not directories) in the given directory up to the specified depth.
     *
     * @param directory The directory to list files from
     * @param depth     The maximum depth to traverse
     * @return A set of file names (not paths) in the directory
     * @throws IOException if an I/O error occurs
     */
    public static Set<String> listFilesInDirectory(Path directory, int depth) throws IOException {
        try (Stream<Path> stream = Files.walk(directory, depth)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Write the given content to the specified file, creating the file if it does not exist.
     * Uses the platform default charset.
     *
     * @param content The content to write
     * @param path    The path to the file
     * @throws IOException if an I/O error occurs
     */
    public static void writeToFile(String content, Path path) throws IOException {
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8); // uses platform default charset
        } catch (IOException e) {
            log.warn("Could not write to file {}", path);
            throw e;
        }
    }

    public static Optional<String> readFromFileIfPresent(Path path) {
        try {
            return Optional.of(Files.readString(path));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static String readStringFromResource(String resourceName) throws IOException {
        try (Scanner scanner = new Scanner(getResourceAsStream(resourceName))) {
            return readFromScanner(scanner);
        }
    }

    public static String readFromScanner(Scanner scanner) {
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNextLine()) {
            sb.append(scanner.nextLine());
            if (scanner.hasNextLine()) {
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    public static InputStream getResourceAsStream(String resourceName) throws IOException {
        InputStream resource = FileUtils.class.getClassLoader().getResourceAsStream(resourceName);
        if (resource == null) {
            throw new IOException("Could not load " + resourceName);
        }
        return resource;
    }

    public static void resourceToFile(String resourceName, Path outputPath) throws IOException {
        try (InputStream resource = getResourceAsStream(resourceName)) {
            Files.deleteIfExists(outputPath);
            Files.copy(resource, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void copyFile(Path source, Path destination) throws IOException {
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        inputStream.transferTo(outputStream);
    }

    public static Set<String> listFiles(Path dirPath) {
        if (!dirPath.toFile().exists()) {
            return new HashSet<>();
        }
        try (Stream<Path> stream = Files.list(dirPath)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error(e.toString(), e);
            return new HashSet<>();
        }
    }

    public static Set<String> listDirectories(Path dirPath) {
        if (!dirPath.toFile().exists()) {
            return new HashSet<>();
        }
        try (Stream<Path> stream = Files.list(dirPath)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error(e.toString(), e);
            return new HashSet<>();
        }
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
            return true;
        } catch (IOException e) {
            log.error("Failed to rename {} to {}", oldPath, newPath, e);
            return false;
        }
    }

    public static void backupCorruptedFile(Path directory, Path storageFile, String fileName, String backupFolderName)
            throws IOException {
        if (Files.exists(storageFile)) {
            Path corruptedBackupDir = directory.resolve(backupFolderName);
            Files.createDirectories(corruptedBackupDir);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String newFileName = fileName + "_at_" + timestamp;
            Path target = directory.resolve(backupFolderName).resolve(newFileName);
            Files.move(storageFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static HttpURLConnection downloadFile(URL url,
                                                 Path destination,
                                                 Observable<Double> progress) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int fileSize;
        try {
            connection.connect();
            try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                 OutputStream outputStream = Files.newOutputStream(destination)) {
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

    public static boolean hasResourceFile(String fileName) {
        return FileUtils.class.getClassLoader().getResource(fileName) != null;
    }

    public static void copyDirectory(Path sourceDirectory,
                                     Path destinationDirectory,
                                     Set<String> extensionsToSkip) throws IOException {
        AtomicReference<IOException> exception = new AtomicReference<>();
        try (Stream<Path> stream = Files.walk(sourceDirectory)) {
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
                    Path relativePath = sourceDirectory.relativize(source);
                    Path destination = destinationDirectory.resolve(relativePath);
                    try {
                        Files.copy(source, destination);
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

    public static Set<String> listResources(String resourceDir) throws IOException, URISyntaxException {
        // Resource paths always use forward slashes on all OS.
        String normalized = resourceDir.replace('\\', '/');
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        URL dirURL = FileUtils.class.getClassLoader().getResource(normalized);
        if (dirURL == null) {
            throw new IOException("Resource directory not found: " + normalized);
        }
        String protocol = dirURL.getProtocol();
        if ("file".equals(protocol)) {
            Path dir = Path.of(dirURL.toURI());
            if (!Files.isDirectory(dir)) {
                throw new IOException("Resource path is not a directory: " + dir);
            }
            try (Stream<Path> stream = Files.walk(dir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(path -> dir.relativize(path).toString().replace(File.separatorChar, '/'))
                        .collect(Collectors.toSet());
            }
        } else if ("jar".equals(protocol)) {
            String jarUrlPath = dirURL.getPath();
            int bangIndex = jarUrlPath.indexOf('!');
            String jarFilePath = jarUrlPath.substring(0, bangIndex);
            if (jarFilePath.startsWith("file:")) {
                jarFilePath = jarFilePath.substring(5);
            }
            jarFilePath = URLDecoder.decode(jarFilePath, StandardCharsets.UTF_8);
            Set<String> resourceFiles = new HashSet<>();
            try (JarFile jar = new JarFile(jarFilePath)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory() && name.startsWith(normalized)) {
                        resourceFiles.add(name.substring(normalized.length()));
                    }
                }
            }
            return resourceFiles;
        } else {
            throw new IOException("Unsupported URL protocol: " + protocol);
        }
    }

    public static void copyResourceDirectory(String resourceDir,
                                             Path path) throws IOException, URISyntaxException {
        // Resource paths always use forward slashes on all OS.
        String normalized = resourceDir.replace('\\', '/');
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        Set<String> resources = listResources(normalized);
        for (String resourceFile : resources) {
            String resourcePath = normalized + resourceFile; // classpath uses '/'
            Path targetFile = path.resolve(resourceFile); // preserve relative layout
            try {
                Path parent = targetFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                FileUtils.resourceToFile(resourcePath, targetFile);
            } catch (IOException e) {
                log.error("Could not copy resource {} to {}", resourcePath, targetFile, e);
                throw e;
            }
        }
    }
}
