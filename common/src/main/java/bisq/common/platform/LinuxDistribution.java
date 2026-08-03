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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Slf4j
public enum LinuxDistribution {
    DEBIAN("debian"),
    RED_HAT("redhat"),
    WHONIX("whonix"),
    TAILS("tails");

    @Getter
    private final String canonicalName;

    LinuxDistribution(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public static boolean isDebian() {
        return OS.isLinux() && Files.isRegularFile(Paths.get("/etc/debian_version"));
    }

    public static boolean isRedHat() {
        return OS.isLinux() && Files.isRegularFile(Paths.get("/etc/redhat-release"));
    }

    public static boolean isTails() {
        if (!OS.isLinux()) {
            return false;
        }
        // Official, distribution-agnostic marker: /etc/os-release with ID=tails. Fall back to the
        // legacy amnesia marker and the tails data directory for older or partially-mounted systems.
        return osReleaseIdEquals("tails")
                || Files.isRegularFile(Paths.get("/etc/amnesia_version"))
                || Files.isDirectory(Paths.get("/usr/share/tails"));
    }

    /**
     * Reads the {@code ID} field of {@code /etc/os-release} and compares it case-insensitively.
     * The value may be quoted (e.g. {@code ID="tails"}); surrounding quotes are stripped.
     */
    private static boolean osReleaseIdEquals(String expectedId) {
        return readOsReleaseId()
                .map(id -> id.equalsIgnoreCase(expectedId))
                .orElse(false);
    }

    private static Optional<String> readOsReleaseId() {
        Path osReleasePath = Paths.get("/etc/os-release");
        if (!Files.isRegularFile(osReleasePath)) {
            return Optional.empty();
        }
        try {
            return Files.readAllLines(osReleasePath).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("ID="))
                    .map(line -> line.substring("ID=".length()).trim())
                    .map(LinuxDistribution::unquote)
                    .findFirst();
        } catch (IOException e) {
            log.warn("Could not read /etc/os-release", e);
            return Optional.empty();
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && (value.startsWith("\"") && value.endsWith("\"")
                || value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    public static boolean isWhonix() {
        return OS.isLinux() &&
                Files.isRegularFile(Paths.get("/usr/share/whonix/marker")) &&
                Files.isRegularFile(Paths.get("/usr/share/anon-dist/marker"));
    }
}


