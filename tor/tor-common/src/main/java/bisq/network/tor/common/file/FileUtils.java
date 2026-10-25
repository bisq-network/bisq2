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
package bisq.network.tor.common.file;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Stripped copy of the methods of bisq.common.file.FileMutatorUtils and FileReaderUtils which the Tor build uses.
 * <p>
 * On a POSIX file system, created directories get the permissions rwx------ and written files rw-------, as in
 * Bisq. The originals keep the setting whether POSIX permissions may be used in global state, which bisq-mobile sets
 * to false. Here the caller passes it with the parameter posixPermissionsAllowed. The methods without this parameter
 * allow POSIX permissions, which is the default of the originals and the setting of all desktop applications.
 * <p>
 * The methods only use APIs which exist on Android: UTF-8 is encoded and decoded strictly, so malformed content fails
 * with an exception, as Files.readString and Files.writeString do.
 */
@Slf4j
public class FileUtils {
    private static final boolean IS_POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    private static final Set<PosixFilePermission> OWNER_READ_WRITE_PERMISSIONS =
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    private static final Set<PosixFilePermission> OWNER_READ_WRITE_EXECUTE_PERMISSIONS =
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
    private static final FileAttribute<Set<PosixFilePermission>> OWNER_READ_WRITE_EXECUTE_PERMISSIONS_FILE_ATTRIBUTE =
            PosixFilePermissions.asFileAttribute(OWNER_READ_WRITE_EXECUTE_PERMISSIONS);

    private FileUtils() {
    }

    public static void createDirectories(Path path) throws IOException {
        createDirectories(path, true);
    }

    public static void createDirectories(Path path, boolean posixPermissionsAllowed) throws IOException {
        if (usePosixPermissions(posixPermissionsAllowed)) {
            Files.createDirectories(path, OWNER_READ_WRITE_EXECUTE_PERMISSIONS_FILE_ATTRIBUTE);
            applyDefaultPermissions(path, posixPermissionsAllowed);
        } else {
            Files.createDirectories(path);
        }
    }

    public static void createDirectory(Path path) throws IOException {
        if (usePosixPermissions(true)) {
            Files.createDirectory(path, OWNER_READ_WRITE_EXECUTE_PERMISSIONS_FILE_ATTRIBUTE);
            applyDefaultPermissions(path, true);
        } else {
            Files.createDirectory(path);
        }
    }

    public static void writeToPath(String content, Path path) throws IOException {
        writeToPath(content, path, true);
    }

    public static void writeToPath(String content, Path path, boolean posixPermissionsAllowed) throws IOException {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(content));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            Files.write(path, bytes);
            applyDefaultPermissions(path, posixPermissionsAllowed);
        } catch (IOException e) {
            log.warn("Could not write to file {}", path);
            throw e;
        }
    }

    public static void inputStreamToFile(InputStream inputStream, Path destinationPath) throws IOException {
        Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        applyDefaultPermissions(destinationPath, true);
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

    public static String readUTF8String(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }

    /**
     * Reads the lines of the resource and joins them with the line separator of the system, without a line separator
     * at the end.
     */
    public static String readStringFromResource(String resourceName) throws IOException {
        InputStream resource = FileUtils.class.getClassLoader().getResourceAsStream(resourceName);
        if (resource == null) {
            throw new IOException("Could not load " + resourceName);
        }
        try (Scanner scanner = new Scanner(resource)) {
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
                if (scanner.hasNextLine()) {
                    sb.append(System.lineSeparator());
                }
            }
            return sb.toString();
        }
    }

    private static void applyDefaultPermissions(Path path, boolean posixPermissionsAllowed) throws IOException {
        try {
            if (usePosixPermissions(posixPermissionsAllowed)) {
                if (Files.isDirectory(path)) {
                    Files.setPosixFilePermissions(path, OWNER_READ_WRITE_EXECUTE_PERMISSIONS);
                } else {
                    Files.setPosixFilePermissions(path, OWNER_READ_WRITE_PERMISSIONS);
                }
            }
        } catch (UnsupportedOperationException e) {
            // Non-POSIX file system
        }
    }

    private static boolean usePosixPermissions(boolean posixPermissionsAllowed) {
        return IS_POSIX && posixPermissionsAllowed;
    }
}
