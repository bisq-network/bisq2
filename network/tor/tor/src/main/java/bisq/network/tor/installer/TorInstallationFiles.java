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

package bisq.network.tor.installer;

import lombok.Getter;

import java.nio.file.Path;

@Getter
public class TorInstallationFiles {
    private final Path torDir;
    private final Path torBinary;
    private final Path torrcFile;
    private final Path versionFile;

    public TorInstallationFiles(Path torDirPath) {
        torDir = torDirPath;
        torBinary = torDir.resolve("tor");
        torrcFile = torDir.resolve("torrc");
        versionFile = torDir.resolve("version");
    }
}
