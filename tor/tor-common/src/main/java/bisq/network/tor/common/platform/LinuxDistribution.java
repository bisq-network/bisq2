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
package bisq.network.tor.common.platform;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Stripped copy of bisq.common.platform.LinuxDistribution with the methods which the Tor build uses.
 */
public class LinuxDistribution {
    private LinuxDistribution() {
    }

    public static boolean isWhonix() {
        return OS.isLinux() &&
                Files.isRegularFile(Paths.get("/usr/share/whonix/marker")) &&
                Files.isRegularFile(Paths.get("/usr/share/anon-dist/marker"));
    }
}
