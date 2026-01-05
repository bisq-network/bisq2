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

package bisq.java_se.facades;

import bisq.common.facades.JdkFacade;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class JavaSeJdkFacade implements JdkFacade {
    private static final boolean IS_POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    private static final Set<PosixFilePermission> OWNER_READ_WRITE_PERMISSIONS =
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    private static final Set<PosixFilePermission> OWNER_READ_WRITE_EXECUTE_PERMISSIONS =
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);

    private static final FileAttribute<Set<PosixFilePermission>> OWNER_READ_WRITE_EXECUTE_PERMISSIONS_FILE_ATTRIBUTE =
            PosixFilePermissions.asFileAttribute(OWNER_READ_WRITE_EXECUTE_PERMISSIONS);

    @Override
    public String getMyPid() {
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        return processName.split("@")[0];
    }

    @Override
    public Stream<String> getProcessCommandStream() {
        return ProcessHandle.allProcesses().map(processHandle -> processHandle.info().command().orElse(""));
    }

    @Override
    public void redirectError(ProcessBuilder processBuilder) {
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
    }

    @Override
    public void redirectOutput(ProcessBuilder processBuilder) {
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    }

    @Override
    public String readString(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Override
    public void writeString(String data, Path path) throws IOException {
        Files.writeString(path, data, StandardCharsets.UTF_8);
        applyFilePermissions(path);
    }

    @Override
    public void createDirectories(Path path) throws IOException {
        if (IS_POSIX) {
            Files.createDirectories(path, OWNER_READ_WRITE_EXECUTE_PERMISSIONS_FILE_ATTRIBUTE);
            applyDirectoryPermissions(path);
        } else {
            Files.createDirectories(path);
        }
    }

    private void applyFilePermissions(Path path) {
        if (IS_POSIX) {
            try {
                Files.setPosixFilePermissions(path, OWNER_READ_WRITE_PERMISSIONS);
            } catch (IOException | UnsupportedOperationException e) {
                log.warn("Could not apply file permissions to {}", path, e);
            }
        }
    }

    private void applyDirectoryPermissions(Path path) {
        if (IS_POSIX) {
            try {
                Files.setPosixFilePermissions(path, OWNER_READ_WRITE_EXECUTE_PERMISSIONS);
            } catch (IOException | UnsupportedOperationException e) {
                log.warn("Could not apply directory permissions to {}", path, e);
            }
        }
    }
}
