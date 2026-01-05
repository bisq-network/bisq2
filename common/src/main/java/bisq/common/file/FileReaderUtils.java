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

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class FileReaderUtils {
    public static final String FILE_SEP = File.separator;

    public static String readUTF8String(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
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
     * List all files (not directories) in the given directory up to the specified depth.
     *
     * @param path  The directory to list files from
     * @param depth The maximum depth to traverse
     * @return A set of file names (not paths) in the directory
     * @throws IOException if an I/O error occurs
     */
    public static Set<String> listFilesInDirectory(Path path, int depth) throws IOException {
        try (Stream<Path> stream = Files.walk(path, depth)) {
            return stream
                    .filter(p -> !Files.isDirectory(p))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public static Optional<String> readFromFileIfPresent(Path path) {
        try {
            return Optional.of(readUTF8String(path));
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
        InputStream resource = FileReaderUtils.class.getClassLoader().getResourceAsStream(resourceName);
        if (resource == null) {
            throw new IOException("Could not load " + resourceName);
        }
        return resource;
    }

    public static Set<Path> listRegularFilesAsPath(Path dirPath) {
        if (Files.notExists(dirPath)) {
            return new HashSet<>();
        }
        try (Stream<Path> stream = Files.list(dirPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error(e.toString(), e);
            return new HashSet<>();
        }
    }

    public static Set<String> listRegularFiles(Path dirPath) {
        if (Files.notExists(dirPath)) {
            return new HashSet<>();
        }
        try (Stream<Path> stream = Files.list(dirPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error(e.toString(), e);
            return new HashSet<>();
        }
    }

    public static Set<String> listDirectories(Path dirPath) {
        if (Files.notExists(dirPath)) {
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

    public static boolean hasResourceFile(String fileName) {
        return FileReaderUtils.class.getClassLoader().getResource(fileName) != null;
    }

    public static Set<String> listResources(String resourceDir) throws IOException, URISyntaxException {
        // Resource paths always use forward slashes on all OS.
        String normalized = resourceDir.replace('\\', '/');
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        URL dirURL = FileReaderUtils.class.getClassLoader().getResource(normalized);
        if (dirURL == null) {
            throw new IOException("Resource directory not found: " + normalized);
        }
        String protocol = dirURL.getProtocol();
        if ("file".equals(protocol)) {
            Path dirPath = Path.of(dirURL.toURI());
            if (!Files.isDirectory(dirPath)) {
                throw new IOException("Resource path is not a directory: " + dirPath);
            }
            try (Stream<Path> stream = Files.walk(dirPath)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(path -> dirPath.relativize(path).toString().replace(File.separatorChar, '/'))
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

}
