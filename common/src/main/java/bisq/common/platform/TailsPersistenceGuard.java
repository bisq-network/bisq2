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

package bisq.common.platform;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Tails runs amnesically: anything not stored on the unlocked Persistent Storage volume is lost on
 * shutdown. If Bisq's data directory is not backed by that volume the user would silently lose their
 * identity keys and open offers (dead offers, more mediation). This guard detects that situation so
 * the caller can warn and abort before any data is written.
 */
@Slf4j
public class TailsPersistenceGuard {
    // The persistent volume is unlocked to this mountpoint; persistent directories (e.g.
    // /home/amnesia/Persistent) are bind-mounted from it, so /proc/mounts references it as the source.
    private static final String PERSISTENCE_MOUNT = "/live/persistence/TailsData_unlocked";
    private static final String PERSISTENCE_DEVICE_MARKER = "TailsData_unlocked";

    /**
     * @return true only when running on Tails AND the given data directory is NOT backed by the
     * unlocked Persistent Storage volume. Returns false on every non-Tails system.
     */
    public static boolean isDataDirAmnesic(Path dataDirPath) {
        if (!LinuxDistribution.isTails()) {
            return false;
        }
        return !isOnPersistentStorage(dataDirPath);
    }

    private static boolean isOnPersistentStorage(Path dataDirPath) {
        Path realPath = toRealPath(dataDirPath);

        // Fast path: the resolved path lives directly under the unlocked volume.
        if (realPath.startsWith(PERSISTENCE_MOUNT)) {
            return true;
        }

        // Tails bind-mounts persistent directories elsewhere (e.g. /home/amnesia/Persistent), so
        // consult /proc/mounts: find the mount that backs the data dir and check it is the persistent
        // volume rather than an amnesic tmpfs/overlay.
        return findBackingMount(realPath)
                .map(TailsPersistenceGuard::isPersistentMount)
                .orElse(false);
    }

    private static boolean isPersistentMount(MountEntry mount) {
        return mount.source().contains(PERSISTENCE_DEVICE_MARKER)
                || mount.mountPoint().startsWith(PERSISTENCE_MOUNT);
    }

    private static Optional<MountEntry> findBackingMount(Path realPath) {
        Path mountsPath = Paths.get("/proc/mounts");
        if (!Files.isRegularFile(mountsPath)) {
            return Optional.empty();
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(mountsPath);
        } catch (IOException e) {
            log.warn("Could not read /proc/mounts", e);
            return Optional.empty();
        }

        MountEntry best = null;
        int bestLength = -1;
        for (String line : lines) {
            String[] fields = line.split(" ");
            if (fields.length < 3) {
                continue;
            }
            String source = unescapeMount(fields[0]);
            Path mountPoint = Paths.get(unescapeMount(fields[1]));
            // Longest matching mountpoint prefix is the filesystem actually backing the path.
            if (realPath.startsWith(mountPoint)) {
                int length = mountPoint.toString().length();
                if (length > bestLength) {
                    bestLength = length;
                    best = new MountEntry(source, mountPoint.toString());
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static Path toRealPath(Path dataDirPath) {
        try {
            return dataDirPath.toRealPath();
        } catch (IOException e) {
            log.debug("Could not resolve real path of {}, using normalized absolute path", dataDirPath, e);
            return dataDirPath.toAbsolutePath().normalize();
        }
    }

    // /proc/mounts escapes spaces, tabs, newlines and backslashes as octal sequences.
    private static String unescapeMount(String field) {
        return field.replace("\\040", " ")
                .replace("\\011", "\t")
                .replace("\\012", "\n")
                .replace("\\134", "\\");
    }

    private record MountEntry(String source, String mountPoint) {
    }
}
