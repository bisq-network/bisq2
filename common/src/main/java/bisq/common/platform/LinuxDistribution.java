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

import java.nio.file.Files;
import java.nio.file.Path;

public enum LinuxDistribution {
    DEBIAN("debian"),
    RED_HAT("redhat"),
    WHONIX("whonix");

    @Getter
    private final String canonicalName;

    LinuxDistribution(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public static boolean isDebian() {
        return OS.isLinux() && Files.isRegularFile(Path.of("/etc/debian_version"));
    }

    public static boolean isRedHat() {
        return OS.isLinux() && Files.isRegularFile(Path.of("/etc/redhat-release"));
    }

    public static boolean isWhonix() {
        return OS.isLinux() &&
                Files.isRegularFile(Path.of("/usr/share/whonix/marker")) &&
                Files.isRegularFile(Path.of("/usr/share/anon-dist/marker"));
    }
}


