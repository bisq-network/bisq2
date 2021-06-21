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

package network.misq.common.util;

import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
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

    public static void makeDirIfNotExists(String dirName) throws IOException {
        File dir = new File(dirName);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Cannot create directory " + dir.getAbsolutePath());
            }
        }
    }

    public static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (final File file : files) {
                deleteDirectory(file);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        dir.delete();
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

    public static String readFromFile(File file) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
            }
        }
        return sb.toString();
    }

    public static InputStream getResourceAsStream(String fileName) throws IOException {
        InputStream resource = FileUtils.class.getResourceAsStream(fileName);
        if (resource == null) {
            throw new IOException("Could not load " + fileName);
        }
        return resource;
    }

    public static void resourceToFile(File file) throws IOException {
        InputStream resource = getResourceAsStream(FILE_SEP + file.getName());
        if (file.exists() && !file.delete()) {
            throw new IOException("Could not remove existing file " + file.getName());
        }
        OutputStream out = new FileOutputStream(file);
        copy(resource, out);
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
        try (Stream<Path> stream = Files.list(Paths.get(dirPath))) {
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
}
