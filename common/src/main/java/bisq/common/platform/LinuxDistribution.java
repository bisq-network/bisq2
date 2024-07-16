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

import java.io.File;

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
        return OS.isLinux() && new File("/etc/debian_version").isFile();
    }

    public static boolean isRedHat() {
        return OS.isLinux() && new File("/etc/redhat-release").isFile();
    }

    public static boolean isWhonix() {
        return OS.isLinux() && new File("/usr/share/whonix/marker").isFile() &&
                new File("/usr/share/anon-dist/marker").isFile();
    }
}


