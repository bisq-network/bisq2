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
import bisq.common.platform.OS;
import bisq.common.threading.ThreadName;
import bisq.common.util.StringUtils;
import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class FileUtils {
    public static final String FILE_SEP = File.separator;

    public static void write(String fileName, String data) throws IOException {
        write(fileName, data.getBytes(Charsets.UTF_8));
    }

    public static void write(String fileName, byte[] data) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            outputStream.write(data);
        }
    }

    public static byte[] read(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(fileName));
    }

    public static String readAsString(String fileName) throws IOException {
        return new String(read(fileName), Charsets.UTF_8);
    }

    /**
     * The `File.deleteOnExit` method is not suited for long-running processes as it never removes the added files,
     * thus leading to a memory leak.
     * See: <a href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6664633">...</a>
     * We added our own extended DeleteOnExitHook where we added a remove method. The client is responsible to call that
     * `remove` method via `releaseTempFile` once the file should be deleted.
     *
     * @param file The file to add a shutdown hook for delete on exit
     */
    public static void deleteOnExit(File file) {
        if (!DeleteOnExitHook.isShutdownInProgress()) {
            DeleteOnExitHook.add(file.getPath());
        }
    }

    /**
     * @param file The file to delete and to get removed from the `DeleteOnExitHook`.
     */
    public static void releaseTempFile(File file) throws IOException {
        if (!DeleteOnExitHook.isShutdownInProgress()) {
            DeleteOnExitHook.remove(file.getPath());
        }
        deleteFile(file);
    }

    public static void deleteFileOrDirectory(String dirPath) throws IOException {
        deleteFileOrDirectory(new File(dirPath));
    }

    public static void deleteFileOrDirectory(Path path) throws IOException {
        deleteFileOrDirectory(path.toFile());
    }

    public static void deleteFileOrDirectory(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteFileOrDirectory(file);
            }
        }
        if (dir.exists()) {
            Files.delete(dir.toPath());
        }
    }

    public static void deleteFile(File file) throws IOException {
        if (file.exists()) {
            Files.delete(file.toPath());
        }
    }

    public static void makeDirIfNotExists(String dirName) throws IOException {
        File dir = new File(dirName);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Cannot create directory " + dir.getAbsolutePath());
            }
        }
    }

    public static Path createTempDir() throws IOException {
        Path tempDirPath = Files.createTempDirectory(null);
        recursiveDeleteOnShutdownHook(tempDirPath);
        return tempDirPath;
    }

    public static Set<String> listFilesInDirectory(String directory, int depth) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(directory), depth)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public static void recursiveDeleteOnShutdownHook(Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> {
                    ThreadName.set(FileUtils.class, "shutdownHook-" + StringUtils.truncate(path.toString(), 10));
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

    public static void makeDirs(Path dirPath) throws IOException {
        makeDirs(dirPath.toFile());
    }

    public static void makeDirs(String dirPath) throws IOException {
        makeDirs(new File(dirPath));
    }

    public static void makeDirs(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not make dir " + dir);
        }
    }

    public static void makeFile(File file) throws IOException {
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Could not make file " + file);
        }
    }

    public static void writeToFile(String string, File file) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file.getAbsolutePath())) {
            fileWriter.write(string);
        } catch (IOException e) {
            log.warn("Could not write {} to file {}", string, file);
            throw e;
        }
    }

    public static Optional<String> readFromFileIfPresent(File file) {
        try {
            return Optional.of(readStringFromFile(file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static String readStringFromFile(File file) throws IOException {
        try (Scanner scanner = new Scanner(file)) {
            return readFromScanner(scanner);
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

    public static void resourceToFile(String resourceName, File outputFile) throws IOException {
        try (InputStream resource = getResourceAsStream(resourceName)) {
            if (outputFile.exists() && !outputFile.delete()) {
                throw new IOException("Could not remove existing outputFile " + outputFile.getPath());
            }
            OutputStream out = new FileOutputStream(outputFile);
            copy(resource, out);
        }
    }

    public static void copyFile(File source, File destination) throws IOException {
        try (InputStream inputStream = new FileInputStream(source);
             OutputStream outputStream = new FileOutputStream(destination)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    public static void appendFromResource(PrintWriter printWriter, String pathname) {
        try (InputStream inputStream = FileUtils.class.getResourceAsStream(pathname);
             BufferedReader bufferedReader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                printWriter.println(line);
            }
        } catch (Exception e) {
            log.error("Error at appendFromResource with pathname {}", pathname);
            log.error(e.toString(), e);
        }
    }

    public static byte[] asBytes(File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream inputStream = new FileInputStream(file)) {
            int offset = 0;
            while (offset < bytes.length) {
                int read = inputStream.read(bytes, offset, bytes.length - offset);
                if (read == -1) throw new EOFException();
                offset += read;
            }
            return bytes;
        }
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try (outputStream) {
            byte[] buffer = new byte[4096];
            while (true) {
                int read = inputStream.read(buffer);
                if (read == -1) break;
                outputStream.write(buffer, 0, read);
            }
        }
    }

    public static Set<String> listFiles(String dirPath) {
        return listFiles(Paths.get(dirPath));
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

    public static Set<String> listDirectories(String dirPath) {
        return listDirectories(Paths.get(dirPath));
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

    public static File createNewFile(Path path) throws IOException {
        File file = path.toFile();
        if (!file.createNewFile()) {
            throw new IOException("File with path exists already: " + path);
        }
        return file;
    }

    public static boolean renameFile(File oldFile, File newFile) throws IOException {
        File target = newFile;
        if (OS.isWindows()) {
            // Work around an issue on Windows whereby you can't rename over existing files.
            target = newFile.getCanonicalFile();
            if (target.exists() && !target.delete()) {
                throw new IOException("Failed to delete canonical file for replacement with save");
            }
        }

        return oldFile.renameTo(target);
    }

    public static void backupCorruptedFile(String directory, File storageFile, String fileName, String backupFolderName)
            throws IOException {
        if (storageFile.exists()) {
            File corruptedBackupDir = new File(Paths.get(directory, backupFolderName).toString());
            makeDirs(corruptedBackupDir);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String newFileName = fileName + "_at_" + timestamp;
            File target = new File(Paths.get(directory, backupFolderName, newFileName).toString());
            renameFile(storageFile, target);
        }
    }

    public static HttpURLConnection downloadFile(URL url,
                                                 File destination,
                                                 Observable<Double> progress) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int fileSize;
        try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
             FileOutputStream outputStream = new FileOutputStream(destination)) {
            connection.connect();
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
        } finally {

            connection.disconnect();
        }
        return connection;
    }

    public static boolean hasResourceFile(String fileName) {
        return FileUtils.class.getClassLoader().getResource(fileName) != null;
    }

    public static void copyDirectory(String sourceDirectory, String destinationDirectory) throws IOException {
        Path start = Paths.get(sourceDirectory);
        AtomicReference<IOException> exception = new AtomicReference<>();
        try (Stream<Path> stream = Files.walk(start)) {
            stream.forEach(source -> {
                Path destination = Paths.get(destinationDirectory, source.toString().substring(sourceDirectory.length()));
                try {
                    Files.copy(source, destination);
                } catch (IOException e) {
                    exception.set(e);
                }
            });
        }
        if (exception.get() != null) {
            throw exception.get();
        }
    }
}
